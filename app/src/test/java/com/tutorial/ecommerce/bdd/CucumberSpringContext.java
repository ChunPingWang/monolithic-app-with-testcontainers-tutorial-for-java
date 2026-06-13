package com.tutorial.ecommerce.bdd;

import com.tutorial.ecommerce.ECommerceApplication;
import com.tutorial.ecommerce.e2e.SharedContainersInitializer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(
    classes = ECommerceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = SharedContainersInitializer.class)
@ActiveProfiles("test-real")
public class CucumberSpringContext {
}
