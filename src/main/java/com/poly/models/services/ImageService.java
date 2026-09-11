package com.poly.models.services;

import java.io.InputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    String upload(MultipartFile file) throws Exception;

    String upload(InputStream stream, String objectName, String contentType, long size) throws Exception;

    boolean exists(String objectName) throws Exception;

    void delete(String objectName) throws Exception;

    InputStream download(String objectName) throws Exception; 

    String getPublicUrl(String objectName) throws Exception;

    List<String> listObjects() throws Exception;

}
