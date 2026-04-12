package com.finance.finportfolio.domain.category.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finance.finportfolio.domain.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAsc();
}
