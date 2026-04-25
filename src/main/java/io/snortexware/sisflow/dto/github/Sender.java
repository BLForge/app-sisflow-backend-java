package io.snortexware.sisflow.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sender {
    private String login;
    private Long id;
}