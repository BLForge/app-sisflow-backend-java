package io.snortexware.sisflow.controllers;

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

    public GithubWebhookController(ProjectRepository projectRepository,
                                  GithubConfigurationRepository githubConfigurationRepository,
                                  TicketRepository ticketRepository) {
        this.projectRepository = projectRepository;
        this.githubConfigurationRepository = githubConfigurationRepository;
        this.ticketRepository = ticketRepository;
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("X-GitHub-Event") String event,
                                               @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok("Event ignored");
        }

        try {
            // Parse payload to get repository info for signature verification
            GithubWebhookDTO webhookData = parseWebhookPayload(payload);
            
            if (webhookData == null) {
                logger.warn("Failed to parse webhook payload");
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            String action = webhookData.getAction();
            if (!"opened".equals(action) && !"closed".equals(action) && !"merged".equals(action)) {
                return ResponseEntity.ok("Action ignored");
            }

            // Extract repository info
            String fullName = webhookData.getRepository().getFull_name();
            String[] parts = fullName.split("/");
            if (parts.length != 2) {
                logger.warn("Invalid repository full_name format: {}", fullName);
                return ResponseEntity.badRequest().body("Invalid repository format");
            }

            String owner = parts[0];
            String repo = parts[1];

            // Find project by GitHub repository
            Optional<Project> projectOpt = projectRepository.findByGithubOwnerAndGithubRepository(owner, repo);
            if (projectOpt.isEmpty()) {
                logger.warn("No project found for repository: {}/{}", owner, repo);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");
            }

            Project project = projectOpt.get();

            // Check if GitHub configuration exists and is enabled
            Optional<GithubConfiguration> configOpt = githubConfigurationRepository.findByProjectId(project.getId());
            if (configOpt.isEmpty() || !configOpt.get().getEnabled()) {
                logger.warn("GitHub configuration not found or disabled for project: {}", project.getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("GitHub integration not configured");
            }

            GithubConfiguration config = configOpt.get();

            // SECURITY: Verify webhook signature if secret is configured
            if (config.getWebhookSecret() != null && !config.getWebhookSecret().isEmpty()) {
                if (signature == null || !verifySignature(payload, config.getWebhookSecret(), signature)) {
                    logger.warn("Invalid webhook signature for project: {}", project.getName());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
                }
            } else {
                logger.warn("No webhook secret configured for project: {} - this is insecure!", project.getName());
            }

            // Extract ticket code from PR title and branch
            String title = webhookData.getPull_request().getTitle();
            String branch = webhookData.getPull_request().getHead().getRef();
            String prUrl = webhookData.getPull_request().getHtml_url();

            Long ticketCode = extractTicketCode(title + " " + branch);
            if (ticketCode == null) {
                logger.info("No ticket reference found in PR title or branch");
                return ResponseEntity.ok("No ticket reference found");
            }

            // Find ticket by code
            Optional<Ticket> ticketOpt = ticketRepository.findByCode(ticketCode);
            if (ticketOpt.isEmpty()) {
                logger.warn("Ticket not found with code: {}", ticketCode);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ticket not found");
            }

            Ticket ticket = ticketOpt.get();

            if (project.getPullRequestStatus() != null) {
                ticket.setStatus(project.getPullRequestStatus());
                ticket.setGithubPullRequestUrl(prUrl);
                ticket.setProject(project);
                ticketRepository.save(ticket);

                logger.info("Updated ticket #{} status to {} for PR: {}", 
                           ticketCode, project.getPullRequestStatus().getName(), prUrl);
                
                return ResponseEntity.ok("Ticket updated successfully");
            } else {
                logger.warn("No pull request status configured for project: {}", project.getName());
                return ResponseEntity.ok("No status configured");
            }

        } catch (Exception e) {
            logger.error("Error processing GitHub webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    private GithubWebhookDTO parseWebhookPayload(String payload) {
        try {
            // Simple JSON parsing - in production use proper JSON library
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payload, GithubWebhookDTO.class);
        } catch (Exception e) {
            logger.error("Failed to parse webhook payload", e);
            return null;
        }
    }

    private boolean verifySignature(String payload, String secret, String signature) {
        try {
            if (!signature.startsWith("sha256=")) {
                return false;
            }

            String expectedSignature = signature.substring(7); // Remove "sha256=" prefix
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String computedSignature = hexString.toString();
            return computedSignature.equals(expectedSignature);
            
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