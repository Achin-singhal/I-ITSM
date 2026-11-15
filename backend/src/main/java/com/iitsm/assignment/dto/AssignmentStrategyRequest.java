package com.iitsm.assignment.dto;
import jakarta.validation.constraints.NotBlank;
public class AssignmentStrategyRequest { @NotBlank private String strategy; public String getStrategy(){return strategy;} public void setStrategy(String s){strategy=s;} }
