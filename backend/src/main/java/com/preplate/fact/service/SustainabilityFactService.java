package com.preplate.fact.service;

import static com.preplate.common.dto.ApiDtos.FactResponse;

import com.preplate.fact.repository.SustainabilityFactRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SustainabilityFactService {
    private final SustainabilityFactRepository facts;

    public SustainabilityFactService(SustainabilityFactRepository facts) {
        this.facts = facts;
    }

    @Transactional(readOnly = true)
    public List<FactResponse> list() {
        return facts.findAll().stream()
                .map(fact -> new FactResponse(fact.getId(), fact.getFactText(), fact.getSourceNote())).toList();
    }
}
