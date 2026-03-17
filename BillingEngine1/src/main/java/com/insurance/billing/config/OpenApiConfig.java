package com.insurance.billing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI billingEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Billing Engine API")
                        .description("""
                                Policy Billing & Collections Microservice.
                                
                                Simulates a billing engine with canned data. Use the four endpoints below to:
                                - Retrieve a policy's premium schedule
                                - Record and inspect payment attempts
                                - List delinquent policies
                                - Trigger retry logic for failed payments
                                
                                **Simulation rule:** supply a `paymentMethodToken` ending in `fail` to force a FAILED result; any other value produces SUCCESS.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Insurance Platform Team")
                                .email("platform@insurance.example.com"))
                        .license(new License()
                                .name("Internal – Not for distribution")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ));
    }
}
