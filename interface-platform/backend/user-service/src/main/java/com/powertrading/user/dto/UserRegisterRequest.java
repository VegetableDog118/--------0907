package com.powertrading.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.*;

/**
 * 用户注册请求DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Schema(description = "用户注册请求")
public class UserRegisterRequest {

    @NotBlank(message = "企业名称不能为空")
    @Size(min = 2, max = 50, message = "企业名称长度2-50字符")
    @Schema(description = "企业名称", example = "北京电力交易有限公司")
    private String companyName;

    @NotBlank(message = "统一社会信用代码不能为空")
    @Pattern(regexp = "^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$", 
             message = "统一社会信用代码格式不正确")
    @Schema(description = "统一社会信用代码", example = "91110000123456789X")
    private String creditCode;

    @NotBlank(message = "联系人姓名不能为空")
    @Size(min = 2, max = 20, message = "联系人姓名长度2-20字符")
    @Schema(description = "联系人姓名", example = "张三")
    private String contactName;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    @Schema(description = "手机号码", example = "13800138000")
    private String phone;

    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100字符")
    @Schema(description = "邮箱地址", example = "zhangsan@example.com")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
             message = "密码必须包含大小写字母、数字和特殊字符，长度8-20位")
    @Schema(description = "密码", example = "Password123!")
    private String password;

    @Schema(description = "部门", example = "技术部")
    private String department;

    @Schema(description = "职位", example = "系统管理员")
    private String position;

    // Constructors
    public UserRegisterRequest() {}

    // Getters and Setters
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCreditCode() {
        return creditCode;
    }

    public void setCreditCode(String creditCode) {
        this.creditCode = creditCode;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "UserRegisterRequest{" +
                "companyName='" + companyName + '\'' +
                ", creditCode='" + creditCode + '\'' +
                ", contactName='" + contactName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", position='" + position + '\'' +
                '}';
    }
}