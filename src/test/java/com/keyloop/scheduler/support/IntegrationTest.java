package com.keyloop.scheduler.support;

import com.keyloop.scheduler.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntegrationTest.TestConfig.class})
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected AppointmentSeeder seeder;

    @Autowired
    private DataSource dataSource;

    @TestConfiguration(proxyBeanMethods = false)
    public static class TestConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }

        @Bean
        AppointmentSeeder appointmentSeeder(JdbcClient jdbc) {
            return new AppointmentSeeder(jdbc);
        }
    }

    @BeforeEach
    void resetDatabaseAndClock() {
        jdbc.sql("""
                TRUNCATE appointment, vehicle, customer, technician_skill, technician,
                         service_bay, opening_hours, service_type, dealership
                RESTART IDENTITY CASCADE
                """).update();
        new ResourceDatabasePopulator(new ClassPathResource("db/fixture/R__fixture_data.sql"))
                .execute(dataSource);
        clock.reset();
    }
}
