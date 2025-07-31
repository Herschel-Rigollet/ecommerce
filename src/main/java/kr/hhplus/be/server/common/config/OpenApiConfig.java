package kr.hhplus.be.server.common.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info().title("E-Commerce API")
                        .description("E-commerce backend API 문서입니다.")
                        .version("v1.0"));
    }
}
