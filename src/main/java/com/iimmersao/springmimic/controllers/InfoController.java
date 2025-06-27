package com.iimmersao.springmimic.controllers;

import com.iimmersao.springmimic.annotations.*;

@Controller
public class InfoController {

    public static class AppInfo {
        private String name;
        private int year;

        public AppInfo() {} // required for Jackson

        public AppInfo(String name, int year) {
            this.name = name;
            this.year = year;
        }

        public String getName() { return name; }
        public int getYear() { return year; }

        public void setName(String name) { this.name = name; }
        public void setYear(int year) { this.year = year; }
    }

    @GetMapping("/info")
    public AppInfo getAppInfo() {
        return new AppInfo("MinimalSpring", 2025);
    }
}
