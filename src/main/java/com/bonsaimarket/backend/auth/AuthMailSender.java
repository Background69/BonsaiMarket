package com.bonsaimarket.backend.auth;

public interface AuthMailSender {
    void send(String email,String code,String purpose);
}
