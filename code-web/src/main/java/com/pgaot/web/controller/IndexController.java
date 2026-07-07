package com.pgaot.web.controller;

import com.pgaot.web.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController extends BaseController {

    @GetMapping("/")
    public ApiResponse<Void> index() {
        return ApiResponse.ok();
    }
}
