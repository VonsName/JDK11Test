package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ： fjl
 * @date ： 2018/10/8/008 10:33
 */
@RestController
public class UserController {

    @PostMapping(value = "/json/info")
    public String test2(String name, String name1) {
        System.out.println("name=" + name + "name1=" + name1);
        return "success 测试httpclient";
    }

    @PostMapping(value = "/json/info1")
    public User test3(User user) {
        System.out.println("name=" + user.getUsername() + "pwd=" + user.getPassword());
        return user;
    }
}
