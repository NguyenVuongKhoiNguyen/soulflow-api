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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;

import com.poly.models.entities.Account;
import com.poly.models.enums.RoleCode;
import com.poly.models.enums.SortOrder;
import com.poly.models.mappers.AccountMapper;
import com.poly.models.repositories.AccountRepository;
import com.poly.models.repositories.RoleRepository;
import com.poly.models.requests.AccountRequest;
import com.poly.models.responses.AccountResponse;
import com.poly.models.responses.PageResponse;
import com.poly.models.services.impl.AccountServiceImpl;
import com.poly.models.services.impl.GoogleAuthService;
import com.poly.models.services.impl.RefreshTokenService;
import com.poly.utils.JwtUtil;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private RefreshTokenService refreshTokens;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account sampleAccount;
    private AccountRequest sampleRequest;
    private AccountResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setId(1L);
        sampleAccount.setUsername("nguyen123");
        sampleAccount.setEmail("nguyen@gmail.com");
        sampleAccount.setFullname("Nguyen Vuong");
        sampleAccount.setDeleted(false);
        sampleAccount.setDisabled(false);

        sampleRequest = new AccountRequest();
        sampleRequest.setUsername("nguyen123");
        sampleRequest.setEmail("nguyen@gmail.com");
        sampleRequest.setFullname("Nguyen Vuong");

        sampleResponse = new AccountResponse();
        sampleResponse.setId("1");
        sampleResponse.setUsername("nguyen123");
        sampleResponse.setEmail("nguyen@gmail.com");
        sampleResponse.setFullname("Nguyen Vuong");
    }

    // TC_ACC_01: save tài khoản thành công
    @Test
    @DisplayName("save - lưu tài khoản thành công trả về AccountResponse")
    void save_validRequest_returnsAccountResponse() {
        when(accountMapper.toEntity(sampleRequest)).thenReturn(sampleAccount);
        when(accountRepo.save(sampleAccount)).thenReturn(sampleAccount);
        when(accountMapper.toBasicResponse(sampleAccount)).thenReturn(sampleResponse);

        AccountResponse result = accountService.save(sampleRequest);

        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("nguyen123", result.getUsername());
        verify(accountRepo).save(sampleAccount);
    }

    // TC_ACC_02: softDeleteById thành công
    @Test
    @DisplayName("softDeleteById - xóa mềm tài khoản gọi accountRepo.softDelete")
    void softDeleteById_validId_callsRepository() {
        doNothing().when(accountRepo).softDelete(1L);

        accountService.softDeleteById(1L);

        verify(accountRepo).softDelete(1L);
    }

    // TC_ACC_03: findById với id = null ném IllegalArgumentException
    @Test
    @DisplayName("findById - id null ném IllegalArgumentException")
    void findById_nullId_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.findById(null));
        assertTrue(ex.getMessage().contains("Can't find account when id is null"));
    }

    // TC_ACC_04: findById không tồn tại ném EntityNotFoundException
    @Test
    @DisplayName("findById - không tìm thấy ném EntityNotFoundException")
    void findById_notFound_throwsEntityNotFoundException() {
        when(accountRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> accountService.findById(999L));
    }

    // TC_ACC_05: findById tìm thấy trả về AccountResponse
    @Test
    @DisplayName("findById - tìm thấy trả về AccountResponse")
    void findById_found_returnsResponse() {
        when(accountRepo.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountMapper.toBasicResponse(sampleAccount)).thenReturn(sampleResponse);

        AccountResponse result = accountService.findById(1L);

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    // TC_ACC_06: findByUsername tìm thấy và không tìm thấy
    @Test
    @DisplayName("findByUsername - tìm thấy trả về kết quả, không tìm thấy ném EntityNotFoundException")
    void findByUsername_testBranches() {
        when(accountRepo.findByUsername("nguyen123")).thenReturn(Optional.of(sampleAccount));
        when(accountMapper.toBasicResponse(sampleAccount)).thenReturn(sampleResponse);

        AccountResponse result = accountService.findByUsername("nguyen123");
        assertNotNull(result);
        assertEquals("nguyen123", result.getUsername());

        when(accountRepo.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> accountService.findByUsername("unknown"));
    }

    // TC_ACC_07: findByEmail tìm thấy và không tìm thấy
    @Test
    @DisplayName("findByEmail - tìm thấy trả về kết quả, không tìm thấy ném EntityNotFoundException")
    void findByEmail_testBranches() {
        when(accountRepo.findByEmail("nguyen@gmail.com")).thenReturn(Optional.of(sampleAccount));
        when(accountMapper.toBasicResponse(sampleAccount)).thenReturn(sampleResponse);

        AccountResponse result = accountService.findByEmail("nguyen@gmail.com");
        assertNotNull(result);
        assertEquals("nguyen@gmail.com", result.getEmail());

        when(accountRepo.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> accountService.findByEmail("unknown@gmail.com"));
    }

    // TC_ACC_08: findAccountDetailById
    @Test
    @DisplayName("findAccountDetailById - id null ném lỗi, tồn tại trả về detail")
    void findAccountDetailById_test() {
        assertThrows(IllegalArgumentException.class, () -> accountService.findAccountDetailById(null));

        when(accountRepo.findById(1L)).thenReturn(Optional.of(sampleAccount));
        when(accountMapper.toDetailResponse(sampleAccount)).thenReturn(sampleResponse);

        AccountResponse result = accountService.findAccountDetailById(1L);
        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    // TC_ACC_09: filterAndPaginateAccounts
    @Test
    @DisplayName("filterAndPaginateAccounts - phân trang trả về PageResponse")
    void filterAndPaginateAccounts_returnsPage() {
        Page<Account> page = new PageImpl<>(List.of(sampleAccount));
        when(accountRepo.filterAccounts(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(accountMapper.toBasicResponseList(anyList())).thenReturn(List.of(sampleResponse));

        PageResponse<AccountResponse> result = accountService.filterAndPaginateAccounts(
                false, "nguyen", null, null, false, RoleCode.USER, SortOrder.DESC, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // TC_ACC_10: checkAndExpireBeforePagination
    @Test
    @DisplayName("checkAndExpireBeforePagination - xóa cache khi có bản ghi bị hết hạn")
    void checkAndExpireBeforePagination_clearsCache() {
        when(accountRepo.checkAndExpireCredentialBeforePagination(any(), any(), any(), any(), any()))
                .thenReturn(2);
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        accountService.checkAndExpireBeforePagination(false, "nguyen", LocalDateTime.now(), LocalDateTime.now(), false);

        verify(mockCache, atLeastOnce()).clear();
    }
}
