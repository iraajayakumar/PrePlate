package com.preplate.fact.controller;

import static com.preplate.common.dto.ApiDtos.FactResponse;

import com.preplate.fact.service.SustainabilityFactService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sustainability-facts")
public class SustainabilityFactController {
    private final SustainabilityFactService facts;

    public SustainabilityFactController(SustainabilityFactService facts) {
        this.facts = facts;
    }

    @GetMapping
    public List<FactResponse> list() {
        return facts.list();
    }
}
