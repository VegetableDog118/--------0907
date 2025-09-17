package com.powertrading.user.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具类
 * 用于生成BCrypt加密的密码哈希值
 */
public class PasswordGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        if (args.length == 0) {
            System.out.println("请提供要加密的密码");
            return;
        }
        
        for (String password : args) {
            String encoded = encoder.encode(password);
            System.out.println("明文密码: " + password + " -> BCrypt哈希: " + encoded);
        }
    }
}