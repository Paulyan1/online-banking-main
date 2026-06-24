package com.example.onlinebanking;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class InitialController {

    @GetMapping("/api/hello")
    public String hello() {
        return "Hello from Spring Boot";
    }
}