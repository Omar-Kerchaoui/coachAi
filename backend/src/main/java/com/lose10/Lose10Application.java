package com.lose10;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Lose10Application {

    public static void main(String[] args) {
        SpringApplication.run(Lose10Application.class, args);
    }
}
