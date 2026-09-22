package com.schwab.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndRedirect_endToEndFlow() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.anthropic.com/some/page");

        String responseJson = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String shortCode = objectMapper.readTree(responseJson).get("shortCode").asText();

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.anthropic.com/some/page"));
    }

    @Test
    void createShortUrl_rejectsInvalidUrl() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("not-a-valid-url");

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirect_returns404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/doesNotExist99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void analytics_returns404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/api/urls/doesNotExist99/analytics"))
                .andExpect(status().isNotFound());
    }
}
