package com.knowai.knowaibackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@MapperScan("com.knowai.knowaibackend.mapper")
@SpringBootApplication
@ConfigurationPropertiesScan
public class KnowaiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowaiBackendApplication.class, args);
	}

}
