package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateKnowledgeBaseRequest;
import io.snortexware.sisflow.dto.UpdateKnowledgeBaseRequest;
import io.snortexware.sisflow.entities.Category;
import io.snortexware.sisflow.entities.KnowledgeBase;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.CategoryRepository;
import io.snortexware.sisflow.repositories.KnowledgeBaseRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final UserProfileRepository userProfileRepository;
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;

    public List<KnowledgeBase> list(String query) {
        if (query == null || query.isBlank()) {
            return knowledgeBaseRepository.findByIsPublishedTrue();
        }
        return knowledgeBaseRepository
                .findByIsPublishedTrueAndTitleContainingIgnoreCaseOrIsPublishedTrueAndContentContainingIgnoreCase(query, query);
    }

    @Transactional
    public KnowledgeBase create(UUID callerId, CreateKnowledgeBaseRequest request) {
        UserProfile author = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User profile not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        }

        KnowledgeBase article = KnowledgeBase.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(category)
                .author(author)
                .isPublished(Boolean.TRUE.equals(request.getIsPublished()))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return knowledgeBaseRepository.save(article);
    }

    @Transactional
    public KnowledgeBase update(UUID id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase article = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        }

        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setCategory(category);
        article.setPublished(Boolean.TRUE.equals(request.getIsPublished()));
        article.setUpdatedAt(OffsetDateTime.now());

        return knowledgeBaseRepository.save(article);
    }

    @Transactional
    public void delete(UUID id) {
        if (!knowledgeBaseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        knowledgeBaseRepository.deleteById(id);
    }

    @Transactional
    public void linkToTicket(UUID ticketId, UUID articleId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        KnowledgeBase article = knowledgeBaseRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));

        ticket.getLinkedArticles().add(article);
        ticketRepository.save(ticket);
    }

    @Transactional
    public void unlinkFromTicket(UUID ticketId, UUID articleId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        KnowledgeBase article = knowledgeBaseRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));

        ticket.getLinkedArticles().remove(article);
        ticketRepository.save(ticket);
    }

    public List<KnowledgeBase> listForTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        return new ArrayList<>(ticket.getLinkedArticles());
    }
}
