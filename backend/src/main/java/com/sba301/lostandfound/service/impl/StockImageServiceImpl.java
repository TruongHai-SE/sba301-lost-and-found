package com.sba301.lostandfound.service.impl;

import com.sba301.lostandfound.dto.StockImageResponse;
import com.sba301.lostandfound.entity.StockImage;
import com.sba301.lostandfound.entity.enums.Category;
import com.sba301.lostandfound.repository.StockImageRepository;
import com.sba301.lostandfound.service.ImageStorageService;
import com.sba301.lostandfound.service.StockImageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StockImageServiceImpl implements StockImageService {

    private final StockImageRepository stockImageRepository;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<StockImageResponse> getAll() {
        return stockImageRepository.findAll().stream()
                .map(StockImageResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockImageResponse> getByCategory(Category category) {
        return stockImageRepository.findByCategory(category).stream()
                .map(StockImageResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StockImageResponse getById(Long id) {
        StockImage stockImage = stockImageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock image not found: " + id));
        return StockImageResponse.from(stockImage);
    }

    @Override
    @Transactional
    public StockImageResponse create(MultipartFile file, Category category, String label) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        String cloudinaryUrl = imageStorageService.upload(file);
        StockImage stockImage = stockImageRepository.save(StockImage.builder()
                .category(category)
                .imageUrl(cloudinaryUrl)
                .label(label)
                .createdAt(LocalDateTime.now())
                .build());
        return StockImageResponse.from(stockImage);
    }

    @Override
    @Transactional
    public StockImageResponse update(Long id, MultipartFile file, Category category, String label) {
        StockImage stockImage = stockImageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock image not found: " + id));

        if (file != null && !file.isEmpty()) {
            String cloudinaryUrl = imageStorageService.upload(file);
            stockImage.setImageUrl(cloudinaryUrl);
        }
        if (category != null) {
            stockImage.setCategory(category);
        }
        if (label != null) {
            stockImage.setLabel(label);
        }

        return StockImageResponse.from(stockImageRepository.save(stockImage));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!stockImageRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock image not found: " + id);
        }
        stockImageRepository.deleteById(id);
    }
}