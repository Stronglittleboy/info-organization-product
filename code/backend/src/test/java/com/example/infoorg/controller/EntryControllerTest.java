package com.example.infoorg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEntry_should_return_200_when_request_valid() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("rawContent", "测试收集内容");
        body.put("contentType", "text");
        body.put("sourceType", "manual");
        body.put("sourceTitle", "手工录入");

        mockMvc.perform(post("/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").isNotEmpty())
                .andExpect(jsonPath("$.rawContent").value("测试收集内容"));
    }

    @Test
    void getRecentEntries_should_return_created_entry() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("rawContent", "recent测试内容");
        body.put("contentType", "text");
        body.put("sourceType", "manual");
        body.put("sourceTitle", "recent来源");

        mockMvc.perform(post("/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/entries/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rawContent").value("recent测试内容"));
    }

    @Test
    void createEntry_should_create_topic_and_bind_entry_when_topic_name_provided() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("rawContent", "带专题的收集内容");
        body.put("contentType", "text");
        body.put("sourceType", "manual");
        body.put("sourceTitle", "专题来源");
        body.put("topicName", "产品设计");

        mockMvc.perform(post("/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").isNotEmpty())
                .andExpect(jsonPath("$.topicName").value("产品设计"));

        mockMvc.perform(get("/entries/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topicId").isNotEmpty())
                .andExpect(jsonPath("$[0].topicName").value("产品设计"));
    }
}
