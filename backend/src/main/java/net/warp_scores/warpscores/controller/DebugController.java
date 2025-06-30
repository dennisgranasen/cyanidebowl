package net.warp_scores.warpscores.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {
    @GetMapping("/debug-headers")
    public Map<String, String> headers(@RequestHeader Map<String, String> headers) {
        return headers;
    }
}
