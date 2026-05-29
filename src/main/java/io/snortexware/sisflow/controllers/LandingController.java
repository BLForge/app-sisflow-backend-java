package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.services.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LandingController {

    private final TenantResolver tenantResolver;
    
    @Value("${app.base.url}")
    private String baseUrl;

    @GetMapping("/")
    public String landing(HttpServletRequest request, Model model) {
        Tenant tenant = tenantResolver.resolveFromRequest(request).orElse(null);
        
        if (tenant != null) {
            model.addAttribute("tenantName", tenant.getName());
            model.addAttribute("logoUrl", tenant.getLogoUrl() != null ? baseUrl + tenant.getLogoUrl() : null);
            model.addAttribute("backgroundUrl", tenant.getBackgroundUrl() != null ? baseUrl + tenant.getBackgroundUrl() : null);
        }
        
        return "landing";
    }
}
