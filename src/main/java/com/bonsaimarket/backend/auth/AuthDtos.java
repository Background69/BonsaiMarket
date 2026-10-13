package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.User;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    public record Register(
        @NotBlank(message="Vui lòng nhập họ và tên.") @Size(max=150, message="Họ tên tối đa 150 ký tự.") String name,
        @NotBlank(message="Vui lòng nhập email.") @Email(message="Email không hợp lệ.") @Size(max=255) String email,
        @NotBlank(message="Vui lòng nhập mật khẩu.") @Size(min=12,max=72,message="Mật khẩu từ 12 đến 72 ký tự.") String password,
        @NotBlank(message="Vui lòng xác nhận mật khẩu.") @Size(max=72,message="Mật khẩu xác nhận tối đa 72 ký tự.") String confirmPassword) {}
    public record Login(@NotBlank @Size(max=255) String email, @NotBlank @Size(max=72) String password) {}
    public record EmailRequest(@NotBlank @Email @Size(max=255) String email) {}
    public record CodeRequest(@NotBlank @Email @Size(max=255) String email,
        @Pattern(regexp="[0-9]{6}",message="Mã xác minh phải gồm 6 chữ số.") @NotNull String code) {}
    public record Reset(@NotBlank @Email @Size(max=255) String email,
        @NotNull @Pattern(regexp="[0-9]{6}",message="Mã xác minh phải gồm 6 chữ số.") String code,
        @NotBlank @Size(min=12,max=72,message="Mật khẩu từ 12 đến 72 ký tự.") String password,
        @NotBlank @Size(max=72) String confirmPassword) {}
    public record LinkStart(@NotBlank String provider, @NotBlank @Size(max=72) String password) {}
    public record Complete(@NotBlank @Size(max=150) String name, @NotBlank @Email @Size(max=255) String email) {}
    public record UserView(Long id, String name, String email, String role, boolean emailVerified) {
        public static UserView of(User user) {
            return new UserView(user.getId(), user.getName(), user.getEmail(),
                user.getRole().name().toLowerCase(java.util.Locale.ROOT), user.getEmailVerifiedAt()!=null);
        }
    }
    public record Message(String message, int resendAfterSeconds) {}
}
