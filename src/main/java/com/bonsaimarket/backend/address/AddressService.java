package com.bonsaimarket.backend.address;

import com.bonsaimarket.backend.user.*;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.marketplace.MarketplaceTransactions;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class AddressService {
    private final AddressRepository addresses;
    private final UserRepository users;
    private final Clock clock;
    private final EntityManager em;
    public AddressService(AddressRepository addresses, UserRepository users, Clock clock, EntityManager em) { this.addresses=addresses; this.users=users; this.clock=clock; this.em=em; }
    @Transactional(readOnly=true)
    public List<AddressDtos.View> list(Long userId) { return addresses.findByUserIdOrderByIdAsc(userId).stream().map(AddressDtos.View::from).toList(); }
    public AddressDtos.View create(Long userId, AddressDtos.Write body) {
        var user=lockUser(userId);
        if (addresses.findByUserIdOrderByIdAsc(userId).size() >= 50) throw MarketplaceException.invalid("Mỗi tài khoản tối đa 50 địa chỉ.");
        var address=new Address(); address.setUser(user); address.setIsDefault(false); address.setCreatedAt(now()); fill(address,body); addresses.saveAndFlush(address);
        normalizeDefault(userId, body.isDefault() ? address.getId() : null);
        return AddressDtos.View.from(address);
    }
    public AddressDtos.View update(Long userId, Long id, AddressDtos.Write body) {
        lockUser(userId); var address=owned(userId,id); fill(address,body);
        // Giữ địa chỉ mặc định cho đến khi người dùng chọn địa chỉ khác.
        normalizeDefault(userId, body.isDefault() ? id : null); return AddressDtos.View.from(address);
    }
    public AddressDtos.View makeDefault(Long userId, Long id) {
        lockUser(userId); var address=owned(userId,id); normalizeDefault(userId,id); return AddressDtos.View.from(address);
    }
    public void delete(Long userId, Long id) {
        lockUser(userId); var address=owned(userId,id); addresses.delete(address); addresses.flush(); normalizeDefault(userId,null);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public AddressDtos.ShippingSnapshot shippingSnapshot(Long buyerId, Long addressId) {
        lockUser(buyerId); var a=owned(buyerId,addressId);
        return new AddressDtos.ShippingSnapshot(a.getRecipientName(),a.getPhone(),a.getProvinceName(),a.getDistrictName(),a.getWardName(),a.getAddressDetail());
    }
    private User lockUser(Long userId) { MarketplaceTransactions.requireReadCommitted(em); return users.lockById(userId).orElseThrow(MarketplaceException::missing); }
    private Address owned(Long userId,Long id) { return addresses.findByIdAndUserId(id,userId).orElseThrow(MarketplaceException::missing); }
    private void fill(Address a,AddressDtos.Write b) { a.setLabel(b.label());a.setRecipientName(b.recipientName());a.setPhone(b.phone());a.setProvinceName(b.provinceName());a.setDistrictName(b.districtName());a.setWardName(b.wardName());a.setAddressDetail(b.addressDetail());a.setUpdatedAt(now()); }
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC); }
    private void normalizeDefault(Long userId,Long preferred) {
        var all=addresses.findByUserIdOrderByIdAsc(userId); if(all.isEmpty()) return;
        Long selected=preferred != null ? preferred : all.stream().filter(a->Boolean.TRUE.equals(a.getIsDefault())).map(Address::getId).findFirst().orElse(all.getFirst().getId());
        for(var a:all) if(Boolean.TRUE.equals(a.getIsDefault()) && !a.getId().equals(selected)) {a.setIsDefault(false);a.setUpdatedAt(now());}
        addresses.flush();
        var chosen=all.stream().filter(a->a.getId().equals(selected)).findFirst().orElseThrow(); chosen.setIsDefault(true);chosen.setUpdatedAt(now());
    }
}
