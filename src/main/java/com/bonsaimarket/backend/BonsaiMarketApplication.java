package com.bonsaimarket.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BonsaiMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(BonsaiMarketApplication.class, args);
    }

}
