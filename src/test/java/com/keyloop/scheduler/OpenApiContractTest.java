package com.keyloop.scheduler;

import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Keeps docs/openapi.yaml in sync with the generated document (AC-E03.3). */
class OpenApiContractTest extends IntegrationTest {

    private static final Path COMMITTED = Path.of("docs", "openapi.yaml");

    @Test
    void committedContractMatchesGeneratedDocument() throws Exception {
        String generated = normalize(mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        if (Boolean.getBoolean("updateOpenApi")) {
            Files.writeString(COMMITTED, generated, StandardCharsets.UTF_8);
            return;
        }
        assertThat(COMMITTED).as("run with -DupdateOpenApi=true to generate it").exists();
        assertThat(generated).isEqualTo(normalize(Files.readString(COMMITTED, StandardCharsets.UTF_8)));
    }

    private static String normalize(String yaml) {
        return yaml.replace("\r\n", "\n").stripTrailing() + "\n";
    }
}
