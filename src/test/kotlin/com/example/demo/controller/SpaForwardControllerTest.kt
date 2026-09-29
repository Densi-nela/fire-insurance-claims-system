package com.example.demo.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SpaForwardControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should forward root path to index html`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/index.html"))
    }

    @Test
    fun `should forward dashboard route to index html`() {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/index.html"))
    }

    @Test
    fun `should forward login route to index html`() {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/index.html"))
    }

    @Test
    fun `should forward register route to index html`() {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/index.html"))
    }

    @Test
    fun `should forward claim detail route to index html`() {
        mockMvc.perform(get("/claims/1"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/index.html"))
    }
}
