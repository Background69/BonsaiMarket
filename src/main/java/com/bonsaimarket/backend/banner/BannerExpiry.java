package com.bonsaimarket.backend.banner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="banner.expiry.enabled",havingValue="true")
public class BannerExpiry {
    private final BannerCampaigns campaigns;
    public BannerExpiry(BannerCampaigns campaigns){this.campaigns=campaigns;}
    @Scheduled(fixedDelay=60000,initialDelay=60000) public void expire(){campaigns.expireDue();}
}
