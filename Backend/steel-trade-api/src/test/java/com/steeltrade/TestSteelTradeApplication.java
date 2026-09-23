package com.steeltrade;

import org.springframework.boot.SpringApplication;

public class TestSteelTradeApplication {

    public static void main(String[] args) {
        SpringApplication.from(SteelTradeApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
