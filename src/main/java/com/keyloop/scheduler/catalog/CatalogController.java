package com.keyloop.scheduler.catalog;

import com.keyloop.scheduler.catalog.dto.DealershipResponse;
import com.keyloop.scheduler.catalog.dto.ServiceTypeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/dealerships")
    public List<DealershipResponse> dealerships() {
        return catalogService.listDealerships();
    }

    @GetMapping("/service-types")
    public List<ServiceTypeResponse> serviceTypes() {
        return catalogService.listServiceTypes();
    }
}
