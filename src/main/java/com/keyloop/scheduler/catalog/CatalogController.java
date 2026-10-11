package com.keyloop.scheduler.catalog;

import com.keyloop.scheduler.catalog.dto.DealershipResponse;
import com.keyloop.scheduler.catalog.dto.ServiceTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Catalog", description = "Reference data: dealerships and service types")
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "List dealerships")
    @GetMapping("/dealerships")
    public List<DealershipResponse> dealerships() {
        return catalogService.listDealerships();
    }

    @Operation(summary = "List service types")
    @GetMapping("/service-types")
    public List<ServiceTypeResponse> serviceTypes() {
        return catalogService.listServiceTypes();
    }
}
