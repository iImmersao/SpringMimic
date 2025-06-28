package com.iimmersao.springmimic.testcomponents;

import com.iimmersao.springmimic.annotations.Controller;
import com.iimmersao.springmimic.annotations.GetMapping;
import com.iimmersao.springmimic.annotations.PathVariable;
import com.iimmersao.springmimic.annotations.PostMapping;
import com.iimmersao.springmimic.annotations.RequestBody;

@Controller
public class TestController {

    @GetMapping("/echo/{value}")
    public String echo(@PathVariable("value") String val) {
        return "Echo: " + val;
    }

    @PostMapping("/json")
    public String postJson(@RequestBody TestData data) {
        return "Received: " + data.name;
    }

    public static class TestData {
        public String name;
    }
}
