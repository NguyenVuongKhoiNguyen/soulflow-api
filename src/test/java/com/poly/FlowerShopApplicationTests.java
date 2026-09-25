package com.poly;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import io.minio.MinioClient;

@SpringBootTest
class FlowerShopApplicationTests {

    @MockBean
    private MinioClient minioClient;

	@Test
	void contextLoads() {
	}

}
