package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.poly.models.entities.Payment;
import com.poly.models.enums.PaymentMethod;
import com.poly.models.mappers.PaymentMapper;
import com.poly.models.repositories.PaymentRepository;
import com.poly.models.requests.PaymentRequest;
import com.poly.models.responses.PaymentResponse;
import com.poly.models.services.impl.PaymentServiceImpl;
import com.poly.models.services.impl.QrServiceImpl;
import com.poly.models.services.impl.SepayWebhookService;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepo;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private QrServiceImpl qrService;

    @Mock
    private SepayWebhookService sepayWebhookService;

    private Payment samplePayment;
    private PaymentRequest sampleRequest;
    private PaymentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        qrService = new QrServiceImpl();

        samplePayment = new Payment();
        samplePayment.setId(1L);
        samplePayment.setAmount(BigDecimal.valueOf(150_000));
        samplePayment.setPaymentMethod(PaymentMethod.COD);
        samplePayment.setPaid(false);

        sampleRequest = new PaymentRequest();
        sampleRequest.setOrderId(100L);
        sampleRequest.setAmount(BigDecimal.valueOf(150_000));
        sampleRequest.setPaymentMethod(PaymentMethod.COD);
        sampleRequest.setPaid(false);

        sampleResponse = new PaymentResponse();
        sampleResponse.setId("1");
        sampleResponse.setAmount("150000");
        sampleResponse.setPaid("false");
    }

    // TC_PAY_01: PaymentServiceImpl.save đơn mới
    @Test
    @DisplayName("save - lưu thông tin thanh toán mới thành công")
    void save_newPayment_returnsPaymentResponse() {
        when(paymentMapper.toEntity(sampleRequest)).thenReturn(samplePayment);
        when(paymentRepo.findByOrderId(100L)).thenReturn(Optional.empty());
        when(paymentRepo.save(samplePayment)).thenReturn(samplePayment);
        when(paymentMapper.toResponse(samplePayment)).thenReturn(sampleResponse);

        PaymentResponse result = paymentService.save(sampleRequest);

        assertNotNull(result);
        assertEquals("150000", result.getAmount());
        verify(paymentRepo).save(samplePayment);
    }

    // TC_PAY_02: PaymentServiceImpl.save cập nhật thông tin thanh toán đã tồn tại
    @Test
    @DisplayName("save - cập nhật thanh toán đã tồn tại kế thừa id và ngày thanh toán")
    void save_existingPayment_updatesFields() {
        Payment existing = new Payment();
        existing.setId(5L);
        existing.setPaid(true);
        existing.setPaymentMethod(PaymentMethod.E_BANKING);

        when(paymentMapper.toEntity(sampleRequest)).thenReturn(samplePayment);
        when(paymentRepo.findByOrderId(100L)).thenReturn(Optional.of(existing));
        when(paymentRepo.save(samplePayment)).thenReturn(samplePayment);
        when(paymentMapper.toResponse(samplePayment)).thenReturn(sampleResponse);

        PaymentResponse result = paymentService.save(sampleRequest);

        assertNotNull(result);
        assertEquals(5L, samplePayment.getId());
        verify(paymentRepo).save(samplePayment);
    }

    // TC_PAY_03: QrServiceImpl.buildQrUrl ném lỗi khi amount null hoặc <= 0
    @Test
    @DisplayName("buildQrUrl - số tiền null hoặc âm ném IllegalArgumentException")
    void buildQrUrl_invalidAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> qrService.buildQrUrl(null, "DH123"));
        assertThrows(IllegalArgumentException.class, () -> qrService.buildQrUrl(BigDecimal.ZERO, "DH123"));
        assertThrows(IllegalArgumentException.class, () -> qrService.buildQrUrl(BigDecimal.valueOf(-1000), "DH123"));
    }

    // TC_PAY_04: QrServiceImpl.buildQrUrl ném lỗi khi orderCode null hoặc blank
    @Test
    @DisplayName("buildQrUrl - mã đơn hàng rỗng ném IllegalArgumentException")
    void buildQrUrl_blankOrderCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> qrService.buildQrUrl(BigDecimal.valueOf(50000), null));
        assertThrows(IllegalArgumentException.class, () -> qrService.buildQrUrl(BigDecimal.valueOf(50000), "   "));
    }

    // TC_PAY_05: QrServiceImpl.buildQrUrl ném lỗi khi số tiền có số lẻ thập phân
    @Test
    @DisplayName("buildQrUrl - số tiền có phần lẻ thập phân ném IllegalArgumentException")
    void buildQrUrl_decimalAmount_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> qrService.buildQrUrl(new BigDecimal("10000.50"), "DH123"));
    }

    // TC_PAY_06: QrServiceImpl.buildQrUrl thành công trả về URL VietQR
    @Test
    @DisplayName("buildQrUrl - tạo URL VietQR thành công chứa đúng thông tin tài khoản và số tiền")
    void buildQrUrl_validInputs_returnsVietQrUrl() {
        String url = qrService.buildQrUrl(BigDecimal.valueOf(150_000), "DH123");

        assertNotNull(url);
        assertTrue(url.startsWith("https://img.vietqr.io/image/"));
        assertTrue(url.contains("amount=150000"));
        assertTrue(url.contains("addInfo=DH123"));
    }

    // TC_PAY_07: SepayWebhookService.authenticate ném SERVICE_UNAVAILABLE khi chưa cấu hình
    @Test
    @DisplayName("authenticate - chưa cấu hình API key ném ResponseStatusException 503")
    void sepayAuthenticate_unconfigured_throwsServiceUnavailable() {
        SepayWebhookService service = new SepayWebhookService(null, null, null, null, null, null);
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "accountNumber", "");

        assertThrows(ResponseStatusException.class, () -> service.authenticate("Apikey test"));
    }

    // TC_PAY_08: SepayWebhookService.authenticate ném UNAUTHORIZED khi Authorization header sai
    @Test
    @DisplayName("authenticate - Authorization header sai định dạng hoặc sai key ném 401")
    void sepayAuthenticate_invalidAuth_throwsUnauthorized() {
        SepayWebhookService service = new SepayWebhookService(null, null, null, null, null, null);
        ReflectionTestUtils.setField(service, "apiKey", "secret_key_123");
        ReflectionTestUtils.setField(service, "accountNumber", "6011246825");

        assertThrows(ResponseStatusException.class, () -> service.authenticate(null));
        assertThrows(ResponseStatusException.class, () -> service.authenticate("Bearer secret_key_123"));
        assertThrows(ResponseStatusException.class, () -> service.authenticate("Apikey wrong_key"));
    }
}
