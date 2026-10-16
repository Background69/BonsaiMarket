package com.bonsaimarket.backend.auth;

import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

/** Bound anonymous auth parsing and concurrent password hashing / synchronous SMTP work. */
public class AuthBodyFilter extends OncePerRequestFilter {
    private final Semaphore concurrent=new Semaphore(8);
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith(request.getContextPath()+"/api/auth/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(!concurrent.tryAcquire()) {error(response,429,"AUTH_BUSY","Dịch vụ đang bận. Vui lòng thử lại sau.");response.setHeader("Retry-After","5");return;}
        try {
            if(request.getContentLengthLong()>8192) {error(response,413,"REQUEST_TOO_LARGE","Yêu cầu vượt quá kích thước cho phép.");return;}
            byte[] body=request.getInputStream().readNBytes(8193);
            if(body.length>8192) {error(response,413,"REQUEST_TOO_LARGE","Yêu cầu vượt quá kích thước cho phép.");return;}
            chain.doFilter(new HttpServletRequestWrapper(request) {
                @Override public ServletInputStream getInputStream() {
                    var input=new ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        @Override public int read() {return input.read();}
                        @Override public int read(byte[] buffer,int offset,int length) {return input.read(buffer,offset,length);}
                        @Override public boolean isFinished() {return input.available()==0;}
                        @Override public boolean isReady() {return true;}
                        @Override public void setReadListener(ReadListener listener) {throw new UnsupportedOperationException();}
                    };
                }
                @Override public BufferedReader getReader() {return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
            },response);
        } finally {concurrent.release();}
    }
    private void error(HttpServletResponse response,int status,String code,String message) throws IOException {
        response.setStatus(status);response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"code\":\""+code+"\",\"message\":\""+message+"\"}");
    }
}
