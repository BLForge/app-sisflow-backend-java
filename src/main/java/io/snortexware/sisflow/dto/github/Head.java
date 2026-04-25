package io.snortexware.sisflow.dto.github;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Head {
    private String ref;
    private String sha;
    private RepoInfo repo;
}
