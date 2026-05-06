package cz.semester.courseapp.http;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI courseReservationsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Course Reservations API")
                        .version("1.0.0")
                        .description("REST API pro spravu kurzu, studentu, terminu, zapisu a cekaci listiny.")
                        .license(new License().name("School project")));
    }
}
