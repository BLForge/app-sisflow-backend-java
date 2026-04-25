package io.snortexware.sisflow;

import io.snortexware.sisflow.security.JwtAuthFilter;
import io.snortexware.sisflow.security.SecurityConfig;
import io.snortexware.sisflow.services.JwtService;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class SisflowApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SisflowApplication.class, args);
    }

}


