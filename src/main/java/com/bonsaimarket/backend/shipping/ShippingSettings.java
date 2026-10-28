package com.bonsaimarket.backend.shipping;

import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.bonsaimarket.backend.marketplace.MarketplaceException;

/** Platform quản lý shop riêng từng gian hàng; không nhận khóa, endpoint hay ShopId từ trình duyệt. */
@Component @ConfigurationProperties(prefix="shipping.ghn") @Getter @Setter
public class ShippingSettings {
    private boolean enabled;
    private String token="";
    private String webhookSecret="";
    private Map<Long,Integer> shopIds=new HashMap<>();
    // Platform duyệt quyền thu hộ COD của tài khoản staging theo từng gian hàng.
    private Set<Long> codEnabledStoreIds=new HashSet<>();
    public int shop(Long storeId) {
        if(!enabled||token.isBlank()) throw new MarketplaceException(503,"SHIPPING_NOT_CONFIGURED","GHN thử nghiệm chưa được cấu hình. Đơn hàng hiện tại được giữ nguyên.");
        var id=shopIds.get(storeId);
        if(id==null||id<1||Collections.frequency(new ArrayList<>(shopIds.values()),id)!=1)
            throw new MarketplaceException(409,"CARRIER_SHOP_REQUIRED","Gian hàng chưa được platform liên kết với ShopId GHN riêng hợp lệ.");
        return id;
    }
    @Override public String toString(){return "ShippingSettings[enabled="+enabled+"]";}
}
