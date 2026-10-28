package com.bonsaimarket.backend.marketplace;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;

/** Giới hạn cả JSON có độ dài khai báo và chunked trước khi giải mã yêu cầu ghi. */
public class MarketplaceBodyFilter extends OncePerRequestFilter {
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        return !java.util.Set.of("POST","PUT","PATCH").contains(request.getMethod())
            || !(path.equals("/api/account/addresses") || path.startsWith("/api/account/addresses/") || path.startsWith("/api/seller/inventory/")
                || path.equals("/api/account/profile") || path.equals("/api/account/seller-application") || path.equals("/api/account/reports")
                || path.startsWith("/api/account/community/") || path.matches("/api/account/products/[0-9]+/(questions|reviews)") || path.startsWith("/api/seller/product-questions/")
                || path.startsWith("/api/account/orders/") || path.startsWith("/api/account/products/") && path.contains("/questions/")
                || path.matches("/api/seller/products/[0-9]+/content")
                || path.startsWith("/api/admin/categories") || path.startsWith("/api/admin/store-applications") || path.startsWith("/api/admin/reports") || path.equals("/api/seller/store")
                || path.startsWith("/api/admin/users/") || path.startsWith("/api/admin/orders/") || path.startsWith("/api/seller/orders/")
                || path.startsWith("/api/account/cart/") || path.startsWith("/api/account/checkout/") || path.startsWith("/api/seller/store/checkout-packages/")
                || path.equals("/api/seller/store/location") || path.equals("/api/shipping/webhooks/ghn"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        boolean compactPayload=request.getRequestURI().contains("/account/addresses") || request.getRequestURI().contains("/seller/inventory/");
        boolean longContent=request.getRequestURI().contains("/account/community/posts") || request.getRequestURI().matches(".*/seller/products/[0-9]+/content") || request.getRequestURI().endsWith("/seller/store");
        int limit=compactPayload?8192:longContent?131072:32768;
        if(request.getContentLengthLong()>limit) {tooLarge(response);return;}
        byte[] body=request.getInputStream().readNBytes(limit+1);if(body.length>limit){tooLarge(response);return;}
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input=new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override public int read(){return input.read();}
                    @Override public int read(byte[] b,int offset,int length){return input.read(b,offset,length);}
                    @Override public boolean isFinished(){return input.available()==0;}
                    @Override public boolean isReady(){return true;}
                    @Override public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}
                };
            }
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        },response);
    }
    private void tooLarge(HttpServletResponse response)throws IOException {
        response.setStatus(413);response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"REQUEST_TOO_LARGE\",\"message\":\"Yêu cầu vượt giới hạn dung lượng.\"}");
    }
}
