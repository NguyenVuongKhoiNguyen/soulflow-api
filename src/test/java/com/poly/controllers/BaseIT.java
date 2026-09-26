package com.poly.controllers;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import io.minio.MinioClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false) // Disables security for direct endpoint testing
@Transactional // Rolls back the database after each test
public abstract class BaseIT {
    @MockBean
    protected MinioClient minioClient;
    
    @MockBean
    protected ChatModel chatModel;

    @MockBean
    protected JavaMailSender javaMailSender;
}
