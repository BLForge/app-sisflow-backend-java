package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByType(Category.CategoryType type);
}
