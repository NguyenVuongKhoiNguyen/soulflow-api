package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import com.poly.models.entities.Account;
import com.poly.models.entities.Role;
import com.poly.models.enums.RoleCode;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.services.impl.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepo;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setCode(RoleCode.USER);

        sampleAccount = new Account();
        sampleAccount.setId(1L);
        sampleAccount.setUsername("auth_user");
        sampleAccount.setPassword("hashed_pwd");
        sampleAccount.setRoles(List.of(role));
        sampleAccount.setDisabled(false);
    }

    // TC_AUTH_01: loadUserByUsername thành công trả về UserDetails
    @Test
    @DisplayName("loadUserByUsername - tìm thấy tài khoản trả về UserDetails hợp lệ")
    void loadUserByUsername_success() {
        when(accountRepo.findByUsername("auth_user")).thenReturn(Optional.of(sampleAccount));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("auth_user");

        assertNotNull(userDetails);
        assertEquals("auth_user", userDetails.getUsername());
        assertEquals("hashed_pwd", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("USER")));
        verify(accountRepo).checkAndExpireCredential("auth_user");
    }

    // TC_AUTH_02: loadUserByUsername khi credential hết hạn
    @Test
    @DisplayName("loadUserByUsername - mật khẩu hết hạn thì isCredentialsNonExpired là false")
    void loadUserByUsername_credentialExpired() {
        sampleAccount.setCredentialExpiredDate(LocalDateTime.now().minusDays(1));
        when(accountRepo.findByUsername("auth_user")).thenReturn(Optional.of(sampleAccount));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("auth_user");

        assertFalse(userDetails.isCredentialsNonExpired());
    }

    // TC_AUTH_03: loadUserByUsername không tìm thấy tài khoản ném UsernameNotFoundException
    @Test
    @DisplayName("loadUserByUsername - không tìm thấy ném UsernameNotFoundException")
    void loadUserByUsername_notFound_throwsException() {
        when(accountRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("unknown"));
    }

    // TC_AUTH_04: RefreshTokenService.issue phát hành token 43 ký tự và lưu vào Redis
    @Test
    @DisplayName("RefreshTokenService.issue - tạo token 43 ký tự và lưu Redis")
    void refreshToken_issue_savesToRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = refreshTokenService.issue("auth_user", true);

        assertNotNull(token);
        assertEquals(43, token.length());
        verify(valueOperations).set(anyString(), eq("1\nauth_user"), any());
    }

    // TC_AUTH_05: RefreshTokenService.consume với token không hợp lệ ném ResponseStatusException
    @Test
    @DisplayName("RefreshTokenService.consume - token sai độ dài ném lỗi 401")
    void refreshToken_consume_invalidLength_throwsUnauthorized() {
        assertThrows(ResponseStatusException.class, () -> refreshTokenService.consume("short_token"));
        assertThrows(ResponseStatusException.class, () -> refreshTokenService.consume(null));
    }

    // TC_AUTH_06: RefreshTokenService.consume không tìm thấy trong Redis ném 401
    @Test
    @DisplayName("RefreshTokenService.consume - không có trong Redis ném 401")
    void refreshToken_consume_notFoundInRedis_throwsUnauthorized() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        // Chuỗi 43 ký tự
        String dummyToken = "1234567890123456789012345678901234567890123";
        assertThrows(ResponseStatusException.class, () -> refreshTokenService.consume(dummyToken));
    }

    // TC_AUTH_07: RefreshTokenService.consume thành công trả về Session
    @Test
    @DisplayName("RefreshTokenService.consume - hợp lệ trả về Session chứa username và rememberMe")
    void refreshToken_consume_success_returnsSession() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn("1\nauth_user");

        String dummyToken = "1234567890123456789012345678901234567890123";
        RefreshTokenService.Session session = refreshTokenService.consume(dummyToken);

        assertNotNull(session);
        assertEquals("auth_user", session.username());
        assertTrue(session.rememberMe());
    }

    // TC_AUTH_08: RefreshTokenService.revoke xóa token khỏi Redis
    @Test
    @DisplayName("RefreshTokenService.revoke - gọi redis.delete khi token hợp lệ")
    void refreshToken_revoke_callsRedisDelete() {
        String dummyToken = "1234567890123456789012345678901234567890123";
        refreshTokenService.revoke(dummyToken);

        verify(redisTemplate).delete(anyString());
    }
}
