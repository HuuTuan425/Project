package com.vuhuutuan.Project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    @GetMapping("/")
    public String home() {
        return "redirect:/hoa-don/lap";
    }

    @GetMapping("/dang-nhap")
    public String login() {
        return "taikhoandangnhap";
    }

    @GetMapping("/khong-co-quyen")
    public String accessDenied() {
        return "taikhoankhongquyen";
    }
}
