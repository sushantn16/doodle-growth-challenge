package com.example.dashboard.experiment;

import java.util.Random;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ExperimentConfig {

    /** java.util.Random is thread-safe. Tests pass a seeded one to VariantAssigner instead. */
    @Bean
    RandomGenerator assignmentRandom() {
        return new Random();
    }
}
