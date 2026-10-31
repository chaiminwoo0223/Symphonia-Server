package com.symphonia;

import com.symphonia.global.config.TimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SymphoniaApplication {
    public static void main(String[] args) {
        TimeConfig.useServiceZone();
        SpringApplication.run(SymphoniaApplication.class, args);
    }
}
