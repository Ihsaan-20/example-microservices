package com.example.paymentservice.config;

import com.example.paymentservice.entity.User;
import com.example.paymentservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin", "password", "ADMIN"));
            userRepository.save(new User("user", "123456", "USER"));
            log.info("Dummy users inserted into database");
        }
    }
}
