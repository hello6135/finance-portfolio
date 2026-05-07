package com.finance.finportfolio.domain.category.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.category.dto.CategoryRequestDto;
import com.finance.finportfolio.domain.category.dto.CategoryResponseDto;
import com.finance.finportfolio.domain.category.entity.Category;
import com.finance.finportfolio.domain.category.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 생성
    @Transactional
    public Long save(CategoryRequestDto requestDto) {
        return categoryRepository.save(requestDto.toEntity()).getId();
    }

    // 수정 (더티 체킹)
    @Transactional
    public void update(Long id, CategoryRequestDto requestDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리가 없습니다. id=" + id));

        // Category 엔티티에 update 메서드 구현 필요
        category.update(requestDto.name(), requestDto.sortOrder());
    }

    // 삭제
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리가 없습니다. id=" + id));
        categoryRepository.delete(category);
    }

    // 목록 조회
    public List<CategoryResponseDto> findAllOrderByOrder() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(CategoryResponseDto::from)
                .toList();
    }
}