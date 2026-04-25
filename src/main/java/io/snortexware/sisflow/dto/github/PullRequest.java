package io.snortexware.sisflow.dto.github;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequest {

    private Long id;
    private Integer number;
    private String state;
    private String title;
    private String body;

    private Boolean merged;

    private Head head;
    private Base base;

    private User user;

    private String html_url;
}
