package com.preplate.fact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sustainability_facts")
public class SustainabilityFact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "fact_text", nullable = false, length = 500)
    private String factText;
    @Column(name = "source_note", length = 200)
    private String sourceNote;

    protected SustainabilityFact() {
    }

    public Long getId() { return id; }
    public String getFactText() { return factText; }
    public String getSourceNote() { return sourceNote; }
}
