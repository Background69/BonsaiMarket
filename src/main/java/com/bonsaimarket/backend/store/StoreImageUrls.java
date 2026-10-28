package com.bonsaimarket.backend.store;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import java.net.URI;
import java.util.Locale;

/** Chỉ hiển thị URL; server không tải ảnh, tra DNS hay làm proxy. */
final class StoreImageUrls {
    private StoreImageUrls() { }
    static String validate(String value) {
        if(value==null || value.isBlank()) return null;
        String url=value.strip();
        try {
            URI uri=URI.create(url);String host=uri.getHost();
            if(!"https".equalsIgnoreCase(uri.getScheme()) || host==null || uri.getRawUserInfo()!=null || uri.getFragment()!=null
                || uri.getPort()!=-1 && uri.getPort()!=443 || url.length()>500 || url.chars().anyMatch(Character::isWhitespace)) throw new IllegalArgumentException();
            host=host.toLowerCase(Locale.ROOT);
            if(!host.contains(".") || host.endsWith(".local") || host.endsWith(".localhost") || host.endsWith(".internal")
                || host.matches("[0-9.]+") || host.contains(":")) throw new IllegalArgumentException();
            return url;
        } catch(IllegalArgumentException e) {throw MarketplaceException.invalid("Ảnh gian hàng cần URL HTTPS công khai, không chứa tài khoản hoặc địa chỉ nội bộ.");}
    }
}
