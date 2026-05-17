package com.imgltd.mmpay.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.imgltd.mmpay")
public class MmpayApplication {

  public static void main(String[] args) {
    SpringApplication.run(MmpayApplication.class, args);
  }
}
