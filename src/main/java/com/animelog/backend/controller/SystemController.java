package com.animelog.backend.controller;

import com.animelog.backend.dto.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统控制器，提供健康检查等基础系统接口。
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    /**
     * 健康检查接口，返回服务运行状态。
     */
    @GetMapping("/ping")
    public AjaxResult ping() {
        return AjaxResult.success(Map.of("status", "ok"));
    }
}
