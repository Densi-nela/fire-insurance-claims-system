package com.example.demo.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaForwardController {

    // Forwards client-side SPA routes to index.html for Vue Router history mode support
    @GetMapping(value = [
        "/",
        "/login",
        "/register",
        "/dashboard",
        "/claims/new",
        "/claims/{id}"
    ])
    fun forward(): String {
        return "forward:/index.html"
    }
}
