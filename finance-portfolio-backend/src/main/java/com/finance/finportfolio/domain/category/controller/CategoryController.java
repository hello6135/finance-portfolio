package com.finance.finportfolio.domain.category.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.category.dto.CategoryRequestDto;
import com.finance.finportfolio.domain.category.dto.CategoryResponseDto;
import com.finance.finportfolio.domain.category.service.CategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    // 조회
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getCategories() {
        return ResponseEntity.ok(categoryService.findAllOrderByOrder());
    }

    // 생성
    @PostMapping
    public ResponseEntity<Long> save(@RequestBody CategoryRequestDto requestDto) {
        return ResponseEntity.ok(categoryService.save(requestDto));
    }

    // 수정
    @PatchMapping("/{id}")
    public ResponseEntity<Long> update(@PathVariable Long id, @RequestBody CategoryRequestDto requestDto) {
        categoryService.update(id, requestDto);
        return ResponseEntity.ok(id);
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Long> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(id);
    }
}