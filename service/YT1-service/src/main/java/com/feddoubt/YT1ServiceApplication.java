package com.feddoubt;

//import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
//@MapperScan("com.feddoubt.YT1.mapper")
public class YT1ServiceApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(YT1ServiceApplication.class ,args);
    }
}
