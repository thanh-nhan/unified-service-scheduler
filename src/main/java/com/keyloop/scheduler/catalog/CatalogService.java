package com.keyloop.scheduler.catalog;

import com.keyloop.scheduler.catalog.dto.DealershipResponse;
import com.keyloop.scheduler.catalog.dto.ServiceTypeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final DealershipRepository dealerships;
    private final ServiceTypeRepository serviceTypes;

    public CatalogService(DealershipRepository dealerships, ServiceTypeRepository serviceTypes) {
        this.dealerships = dealerships;
        this.serviceTypes = serviceTypes;
    }

    public List<DealershipResponse> listDealerships() {
        return dealerships.findAllWithOpeningHoursOrderByName().stream()
                .map(DealershipResponse::from)
                .toList();
    }

    public List<ServiceTypeResponse> listServiceTypes() {
        return serviceTypes.findAllByOrderByNameAsc().stream()
                .map(ServiceTypeResponse::from)
                .toList();
    }
}
