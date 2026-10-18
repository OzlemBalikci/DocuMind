package com.sirket.auth.token.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/.well-known")
public class JwksController {

	@GetMapping("/jwks.json")
	public ResponseEntity<Map<String, Object>> jwks() {
		return ResponseEntity.ok(Map.of("keys", List.of()));
	}
}
