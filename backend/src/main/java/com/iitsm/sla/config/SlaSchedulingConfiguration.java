package com.iitsm.sla.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the deadline monitor in application profiles that opt into scheduling. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.sla.monitor.enabled", havingValue = "true", matchIfMissing = true)
public class SlaSchedulingConfiguration {
}
