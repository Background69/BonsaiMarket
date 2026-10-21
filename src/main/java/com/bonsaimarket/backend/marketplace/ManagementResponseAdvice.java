package com.bonsaimarket.backend.marketplace;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.http.converter.HttpMessageConverter;

@ControllerAdvice(basePackages={"com.bonsaimarket.backend.category", "com.bonsaimarket.backend.profile", "com.bonsaimarket.backend.seller.application", "com.bonsaimarket.backend.store", "com.bonsaimarket.backend.report", "com.bonsaimarket.backend.media", "com.bonsaimarket.backend.product", "com.bonsaimarket.backend.management"})
public class ManagementResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter returnType,Class<? extends HttpMessageConverter<?>> converter) {return true;}
    @Override public Object beforeBodyWrite(Object body,MethodParameter type,MediaType contentType,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response) {
        response.getHeaders().setCacheControl("no-store");return body;
    }
}
