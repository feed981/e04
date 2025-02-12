package com.feddoubt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class Yt1GatewayApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(Yt1GatewayApplication.class ,args);
    }
}
