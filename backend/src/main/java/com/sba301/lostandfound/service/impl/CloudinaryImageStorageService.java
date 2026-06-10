package com.sba301.lostandfound.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sba301.lostandfound.service.ImageStorageService;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CloudinaryImageStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryImageStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String upload(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", "lost-and-found")
            );
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Cloudinary did not return a secure_url");
            }
            return secureUrl.toString();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "Failed to upload image to Cloudinary", exception);
        }
    }
}
