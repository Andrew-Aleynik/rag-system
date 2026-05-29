package com.andrewaleynik.ragsystem.app.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class PageController {

    @GetMapping(value = "/test")
    public String testPage() {
        log.info("Serving test page");
        return "test";
    }

    @GetMapping(value = "/")
    public String adminPage() {
        log.info("Serving admin page");
        return "admin";
    }
}
