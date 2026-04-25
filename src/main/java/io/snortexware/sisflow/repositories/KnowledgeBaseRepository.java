package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {
    List<KnowledgeBase> findByIsPublished(boolean isPublished);
    List<KnowledgeBase> findByCategoryId(UUID categoryId);
    List<KnowledgeBase> findByIsPublishedTrue();
    List<KnowledgeBase> findByIsPublishedTrueAndTitleContainingIgnoreCaseOrIsPublishedTrueAndContentContainingIgnoreCase(String title, String content);
}
