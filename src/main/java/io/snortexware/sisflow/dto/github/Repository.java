package io.snortexware.sisflow.dto.github;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repository {
    private Long id;
    private String name;
    private String full_name;
}