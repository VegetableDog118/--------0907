package com.powertrading.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Schema(description = "用户登录请求")
public class UserLoginRequest {

    @NotBlank(message = "登录账号不能为空")
    @Schema(description = "登录账号（手机号或邮箱）", example = "13800138000")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "Password123!")
    private String password;

    @Schema(description = "验证码（可选）", example = "1234")
    private String captcha;

    // Constructors
    public UserLoginRequest() {}

    public UserLoginRequest(String account, String password) {
        this.account = account;
        this.password = password;
    }

    // Getters and Setters
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    @Override
    public String toString() {
        return "UserLoginRequest{" +
                "account='" + account + '\'' +
                ", captcha='" + captcha + '\'' +
                '}';
    }
}