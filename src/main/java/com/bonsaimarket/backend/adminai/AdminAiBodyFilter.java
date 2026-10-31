package com.bonsaimarket.backend.adminai;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Chạy sau kiểm tra quyền; giới hạn byte, kể cả chunked, trước khi đọc JSON. Vẫn kiểm tra CSRF. */
public class AdminAiBodyFilter extends OncePerRequestFilter {
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !(request.getContextPath()+"/api/admin/ai/chat").equals(request.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        response.setHeader("Cache-Control","no-store");
        if(request.getContentLengthLong()>16384) {tooLarge(response);return;}
        byte[] body=request.getInputStream().readNBytes(16385);
        if(body.length>16384) {tooLarge(response);return;}
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input=new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override public int read() {return input.read();}
                    @Override public int read(byte[] b,int off,int len) {return input.read(b,off,len);}
                    @Override public boolean isFinished() {return input.available()==0;}
                    @Override public boolean isReady() {return true;}
                    @Override public void setReadListener(ReadListener listener) {throw new UnsupportedOperationException();}
                };
            }
            @Override public BufferedReader getReader() {return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        },response);
    }
    private static void tooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(413);response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"REQUEST_TOO_LARGE\",\"message\":\"Yêu cầu vượt giới hạn 16 KiB.\"}");
    }
}
