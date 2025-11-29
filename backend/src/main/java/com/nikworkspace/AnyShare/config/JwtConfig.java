package com.nikworkspace.AnyShare.config;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Configuration
public class JwtConfig {
    // Secret key for signing JWT tokens
    // In production, this should be in environment variables, NOT in code!
    @Value("${jwt.secret}")
    private String secret;

    // Token expiration time in milliseconds (5 minutes)
    @Value("${jwt.expiration}")
    private Long expiration;

}
