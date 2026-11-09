package com.nilspath.api;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {
    @GetMapping({"/candidate/login","/candidate/register","/hr/login","/hr/register","/admin","/admin/login"})
    public String accessPage() { return "forward:/index.html"; }

    @GetMapping("/health") @ResponseBody
    public Map<String,String> health() { return Map.of("status","ok"); }
}
