package com.cureforoptimism.toadzbot;

import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@AllArgsConstructor
public class ToadzbotApplication {
  public static void main(String[] args) {
    SpringApplication.run(ToadzbotApplication.class, args);
  }
}
