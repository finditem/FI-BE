package com.fmi.domain.post.post.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fmi.support.IntegrationTestSupport;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DisplayName("전체 API 구조 계약")
class ApiStructureContractTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc http;

    @Autowired
    private ObjectMapper mapper;

    @Test
    @DisplayName("책임을 분리해도 모든 API의 경로, 메서드, 태그와 operationId를 유지한다")
    void itPreservesApiContracts() throws Exception {
        String body = http.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode paths = mapper.readTree(body).path("paths");
        var contracts = new ArrayList<String>();
        paths.fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(operation -> {
            JsonNode contract = operation.getValue();
            if (contract.has("operationId")) {
                contracts.add(operation.getKey().toUpperCase() + " " + path.getKey() + " "
                        + contract.path("operationId").asText() + " " + contract.path("tags"));
            }
        }));
        Collections.sort(contracts);
        try (var resource = getClass().getResourceAsStream("/contracts/api-structure.txt")) {
            assertThat(resource).isNotNull();
            var expected = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))
                    .lines()
                    .toList();
            assertThat(contracts).containsExactlyElementsOf(expected);
        }
    }
}
