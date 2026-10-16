package com.bonsaimarket.backend.profile;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import static com.bonsaimarket.backend.profile.ProfileDtos.*;

@RestController @RequestMapping("/api")
public class ProfileController {
    private final ProfileService profiles;
    public ProfileController(ProfileService profiles) { this.profiles=profiles; }
    @GetMapping("/account/profile") public ProfileView profile(@AuthenticationPrincipal AuthPrincipal actor) {return profiles.get(actor.id());}
    @PatchMapping("/account/profile") public ProfileView profile(@AuthenticationPrincipal AuthPrincipal actor,@Valid @RequestBody ProfileWrite body) {return profiles.update(actor.id(),body);}
}
