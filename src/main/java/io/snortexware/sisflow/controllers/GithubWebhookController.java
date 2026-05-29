package io.snortexware.sisflow.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.snortexware.sisflow.dto.github.GithubWebhookDTO;
import io.snortexware.sisflow.entities.GithubConfiguration;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.GithubConfigurationRepository;
import io.snortexware.sisflow.repositories.ProjectRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/github")
public class GithubWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(GithubWebhookController.class);
    private static final Pattern TICKET_PATTERN = Pattern.compile("#(\\d+)");

    private final ProjectRepository projectRepository;
    private final GithubConfigurationRepository githubConfigurationRepository;
    private final TicketRepository ticketRepository;
    private final ObjectMapper objectMapper;

    public GithubWebhookController(ProjectRepository projectRepository,
                                   GithubConfigurationRepository githubConfigurationRepository,
                                   TicketRepository ticketRepository,
                                   ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.githubConfigurationRepository = githubConfigurationRepository;
        this.ticketRepository = ticketRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("X-GitHub-Event") String event,
                                               @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok().build();
        }

        try {
            GithubWebhookDTO webhookData = parseWebhookPayload(payload);
            if (webhookData == null) {
                return ResponseEntity.badRequest().build();
            }

            String action = webhookData.getAction();
            if (!"opened".equals(action) && !"closed".equals(action) && !"merged".equals(action)) {
                return ResponseEntity.ok().build();
            }

            String fullName = webhookData.getRepository().getFull_name();
            String[] parts = fullName.split("/");
            if (parts.length != 2) {
                return ResponseEntity.badRequest().build();
            }

            if (signature == null) {
                logger.warn("Webhook received without X-Hub-Signature-256 header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String owner = parts[0];
            String repo = parts[1];

            Optional<Project> projectOpt = projectRepository.findByGithubOwnerAndGithubRepository(owner, repo);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            Project project = projectOpt.get();

            Optional<GithubConfiguration> configOpt = githubConfigurationRepository.findByProjectId(project.getId());
            if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
                return ResponseEntity.ok().build();
            }

            GithubConfiguration config = configOpt.get();

            if (config.getWebhookSecret() == null || config.getWebhookSecret().isEmpty()) {
                logger.warn("Webhook rejected: no secret configured for project {}", project.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (!verifySignature(payload, config.getWebhookSecret(), signature)) {
                logger.warn("Invalid webhook signature for project: {}", project.getId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String title = webhookData.getPull_request().getTitle();
            String branch = webhookData.getPull_request().getHead().getRef();
            String prUrl = webhookData.getPull_request().getHtml_url();

            Long ticketCode = extractTicketCode(title + " " + branch);
            if (ticketCode == null) {
                return ResponseEntity.ok().build();
            }

            Optional<Ticket> ticketOpt = ticketRepository.findByCode(ticketCode);
            if (ticketOpt.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            Ticket ticket = ticketOpt.get();

            if (project.getPullRequestStatus() != null) {
                ticket.setStatus(project.getPullRequestStatus());
                ticket.setGithubPullRequestUrl(prUrl);
                ticket.setProject(project);
                ticketRepository.save(ticket);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error processing GitHub webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private GithubWebhookDTO parseWebhookPayload(String payload) {
        try {
            return objectMapper.readValue(payload, GithubWebhookDTO.class);
        } catch (Exception e) {
            logger.error("Failed to parse webhook payload", e);
            return null;
        }
    }

    private boolean verifySignature(String payload, String secret, String signature) {
        try {
            if (!signature.startsWith("sha256=")) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return java.security.MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    signature.substring(7).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }

    private Long extractTicketCode(String text) {
        Matcher matcher = TICKET_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Invalid ticket code format: {}", matcher.group(1));
            }
        }
        return null;
    }
}
