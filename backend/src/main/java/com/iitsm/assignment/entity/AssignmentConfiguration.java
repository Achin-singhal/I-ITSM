package com.iitsm.assignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name="assignment_configuration")
public class AssignmentConfiguration {
    @Id private Long id=1L;
    @Column(name="strategy_name",nullable=false,length=64) private String strategyName="WORKLOAD_BALANCED";
    protected AssignmentConfiguration(){}
    public AssignmentConfiguration(String strategyName){this.strategyName=strategyName;}
    public Long getId(){return id;} public String getStrategyName(){return strategyName;}
    public void setStrategyName(String value){this.strategyName=value;}
}
