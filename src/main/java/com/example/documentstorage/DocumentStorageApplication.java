package com.example.documentstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentStorageApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentStorageApplication.class, args);
        System.out.println("Backend UP....");
    }
}
