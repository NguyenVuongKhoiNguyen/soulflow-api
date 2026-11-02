package com.poly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.multipart.MultipartFile;

import com.poly.models.services.ImageService;

class ImageControllerIT extends BaseControllerIT {

    @MockBean
    private ImageService imageService;

    // TC_IMG_01: Admin tải lên file hình ảnh hợp lệ
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/image/upload - Admin tải lên ảnh thành công trả về 200 OK")
    void uploadImage_asAdmin_validFile_returns200() throws Exception {
        when(imageService.upload(any(MultipartFile.class))).thenReturn("rose-sample.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rose.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        mockMvc.perform(multipart("/admin/image/upload")
                .file(file)
                .param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("rose-sample.jpg"));
    }

    // TC_IMG_02: Thất bại khi không gửi file
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/image/upload - thiếu file trả về 400 Bad Request")
    void uploadImage_missingFile_returns400() throws Exception {
        mockMvc.perform(post("/admin/image/upload")
                .param("productId", "1"))
                .andExpect(status().isBadRequest());
    }

    // TC_IMG_04: USER thường gọi upload ảnh bị từ chối 403 Forbidden
    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("POST /admin/image/upload - quyền USER thường bị từ chối 403")
    void uploadImage_asUser_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rose.jpg",
                "image/jpeg",
                "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/admin/image/upload")
                .file(file)
                .param("productId", "1"))
                .andExpect(status().isForbidden());
    }

    // TC_IMG_05: Admin xóa hình ảnh thành công
    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("POST /admin/image/delete/{id} - Admin xóa ảnh trả về 200 OK")
    void deleteImage_asAdmin_returns200() throws Exception {
        mockMvc.perform(post("/admin/image/delete/1"))
                .andExpect(status().isOk());
    }

    // TC_IMG_08: Chưa đăng nhập truy cập thư viện ảnh bị 401 Unauthorized
    @Test
    @DisplayName("GET /admin/image - chưa đăng nhập trả về 401 Unauthorized")
    void getImages_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/image"))
                .andExpect(status().isUnauthorized());
    }
}
