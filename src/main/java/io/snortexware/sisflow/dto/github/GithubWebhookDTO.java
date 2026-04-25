package io.snortexware.sisflow.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubWebhookDTO {

    @Getter
    @Setter
    private String action;

    @Getter
    @Setter
    private Integer number;

    @Getter
    @Setter
    private PullRequest pull_request;

    @Getter
    @Setter
    private Repository repository;

    @Getter
    @Setter
    private Sender sender;
}