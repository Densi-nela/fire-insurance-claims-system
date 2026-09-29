package com.example.demo.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val securityInterceptor: SecurityInterceptor,
    @Value("\${cors.allowed-origins:*}")
    private val allowedOriginsString: String
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // Register our security interceptor to protect /api/** paths
        registry.addInterceptor(securityInterceptor)
            .addPathPatterns("/api/**")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        // Parse the comma-separated environment string into an array of origins
        val origins = allowedOriginsString.split(",").map { it.trim() }.toTypedArray()
        val isWildcard = origins.size == 1 && origins[0] == "*"

        val mapping = registry.addMapping("/**")
            .allowedOrigins(*origins) // Spread operator (*) passes Array as varargs
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")

        // Secure Rule: allowCredentials(true) is forbidden by browsers if using a wildcard '*'
        if (!isWildcard) {
            mapping.allowCredentials(true)
        }
    }
}
