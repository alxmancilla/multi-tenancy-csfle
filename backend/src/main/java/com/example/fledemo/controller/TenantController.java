package com.example.fledemo.controller;

import com.example.fledemo.keyvault.TenantKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantKeyService tenantKeyService;

    private static final Map<String, String> TENANT_NAMES = Map.of(
            "tenant_alpha", "Acme Corp",
            "tenant_beta", "Globex Inc",
            "tenant_gamma", "Initech LLC"
    );

    public TenantController(TenantKeyService tenantKeyService) {
        this.tenantKeyService = tenantKeyService;
    }

    @GetMapping
    public List<Map<String, String>> getAllTenants() {
        log.info("Fetching all tenants");
        return tenantKeyService.getAllTenantIds().stream()
                .map(tenantId -> {
                    Map<String, String> tenant = new HashMap<>();
                    tenant.put("id", tenantId);
                    tenant.put("name", TENANT_NAMES.get(tenantId));
                    return tenant;
                })
                .collect(Collectors.toList());
    }
}

