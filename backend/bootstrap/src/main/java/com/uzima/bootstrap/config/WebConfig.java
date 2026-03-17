package com.uzima.bootstrap.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration MVC générale.
 * <p>
 * La configuration CORS est centralisée dans {@link SecurityConfig#corsConfigurationSource()}.
 * Ce fichier est conservé pour les futures configurations MVC (converters, interceptors, etc.)
 * sans dupliquer le CORS.
 */
@Configuration
public class WebConfig {
    // CORS géré exclusivement par SecurityConfig.corsConfigurationSource()
}
