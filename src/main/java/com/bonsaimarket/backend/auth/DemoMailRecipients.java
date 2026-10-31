package com.bonsaimarket.backend.auth;

import java.util.Locale;
import java.util.Set;

public final class DemoMailRecipients {
    private static final Set<String> LOCAL_PARTS=Set.of("admin","seller","user",
        "storefront-v1-vuon-hien","storefront-v1-nang-som","storefront-v1-goc-la",
        "storefront-v1-moc-vien","storefront-v1-nha-chau",
        "discussion-v1-lan","discussion-v1-minh","discussion-v1-an");
    private DemoMailRecipients() {}
    public static boolean blocked(String email) {
        if(email==null)return false;
        String normalized=email.strip().toLowerCase(Locale.ROOT);
        int at=normalized.indexOf('@');
        return at>0 && normalized.substring(at).equals("@bonsai.market") && LOCAL_PARTS.contains(normalized.substring(0,at));
    }
}
