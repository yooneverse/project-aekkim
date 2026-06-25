package com.ssafy.e106.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Swagger (springdoc-openapi) 설정.
 *
 * - /swagger-ui/index.html → Swagger UI
 * - /v3/api-docs → OpenAPI 3.0 JSON 스펙
 *
 * @SecurityScheme → Swagger UI에 "Authorize 🔒" 버튼 추가.
 * Bearer 토큰을 한번 입력하면 이후 모든 API 호출에 자동으로
 * Authorization: Bearer <token> 헤더가 붙음.
 */
@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info()
            .title("E106 API")
            .version("v1")
            .description("SSAFY 14기 특화 프로젝트 E106 API 문서"));
  }
}
