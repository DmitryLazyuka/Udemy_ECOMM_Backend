package org.example.udemyproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UdemyProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(UdemyProjectApplication.class, args);
        System.out.println("USER DIR = " + System.getProperty("user.dir"));
    }

}
