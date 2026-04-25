package io.snortexware.sisflow.dto.github;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String login;
    private Long id;
}
