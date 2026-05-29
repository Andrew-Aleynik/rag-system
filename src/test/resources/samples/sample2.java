package com.andrewaleynik.ragsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {})
public class RAGSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RAGSystemApplication.class, args);
    }

}
