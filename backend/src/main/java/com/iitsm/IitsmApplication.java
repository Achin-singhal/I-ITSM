package com.iitsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the I-ITSM (Intelligent IT Service Management) system.
 * Configured as a modular monolith.
 */
@SpringBootApplication
public class IitsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(IitsmApplication.class, args);
    }
}
