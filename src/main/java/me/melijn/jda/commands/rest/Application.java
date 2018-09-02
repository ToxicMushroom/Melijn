package me.melijn.jda.commands.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public void init(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
