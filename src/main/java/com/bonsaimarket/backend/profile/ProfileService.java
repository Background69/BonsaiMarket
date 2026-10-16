package com.bonsaimarket.backend.profile;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;

import com.bonsaimarket.backend.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.bonsaimarket.backend.profile.ProfileDtos.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class ProfileService {
    private final MarketplaceAccess access;
    public ProfileService(MarketplaceAccess access) {this.access=access;}
    @Transactional(readOnly=true) public ProfileView get(Long id) {return view(access.actor(id));}
    public ProfileView update(Long id,ProfileWrite body) {
        access.actor(id);var user=access.lock(User.class,id);
        user.setName(body.name().strip());user.setPhone(body.phone().strip());user.setBio(body.bio().strip());user.setUpdatedAt(access.now());return view(user);
    }
    private ProfileView view(User u) {return new ProfileView(u.getName(),u.getEmail(),u.getPhone(),u.getBio(),u.getEmailVerifiedAt()!=null);}
}
