package com.fashion.auth.controller;

import com.fashion.auth.service.GhnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ghn/locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class GhnLocationController {

    private final GhnService ghnService;

    /**
     * GET /api/ghn/locations/provinces
     */
    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        try {
            return ResponseEntity.ok(ghnService.getProvinces());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/ghn/locations/districts?provinceId=201
     */
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        try {
            return ResponseEntity.ok(ghnService.getDistricts(provinceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/ghn/locations/wards?districtId=1442
     */
    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        try {
            return ResponseEntity.ok(ghnService.getWards(districtId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
