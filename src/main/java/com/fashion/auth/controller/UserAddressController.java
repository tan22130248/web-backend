package com.fashion.auth.controller;

import com.fashion.auth.dto.AuthDto.MessageResponse;
import com.fashion.auth.dto.UserAddressDto;
import com.fashion.auth.model.User;
import com.fashion.auth.model.UserAddress;
import com.fashion.auth.repository.UserRepository;
import com.fashion.auth.repository.UserAddressRepository;
import com.fashion.auth.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-addresses")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserAddressController {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public UserAddressController(UserAddressRepository userAddressRepository,
                                 UserRepository userRepository,
                                 JwtUtils jwtUtils) {
        this.userAddressRepository = userAddressRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping
    public ResponseEntity<?> getUserAddresses(@RequestHeader("Authorization") String token) {
        try {
            String userId = getUserId(token);
            List<UserAddressDto> addresses = userAddressRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(UserAddressDto::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> saveUserAddress(
            @RequestHeader("Authorization") String token,
            @RequestBody UserAddressDto dto) {
        try {
            String userId = getUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            if (dto.getFullName() == null || dto.getFullName().isBlank() ||
                dto.getPhone() == null || dto.getPhone().isBlank() ||
                dto.getAddress() == null || dto.getAddress().isBlank() ||
                dto.getProvinceId() == null || dto.getDistrictId() == null ||
                dto.getWardCode() == null || dto.getWardCode().isBlank()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Vui lòng nhập đầy đủ thông tin địa chỉ"));
            }

            // Kiểm tra trùng lặp
            Optional<UserAddress> existing = userAddressRepository.findByUserIdAndPhoneAndAddressAndWardCode(
                    userId, dto.getPhone(), dto.getAddress(), dto.getWardCode()
            );

            if (existing.isPresent()) {
                return ResponseEntity.ok(UserAddressDto.from(existing.get()));
            }

            UserAddress address = UserAddress.builder()
                    .user(user)
                    .fullName(dto.getFullName())
                    .phone(dto.getPhone())
                    .address(dto.getAddress())
                    .provinceId(dto.getProvinceId())
                    .districtId(dto.getDistrictId())
                    .wardCode(dto.getWardCode())
                    .provinceName(dto.getProvinceName())
                    .districtName(dto.getDistrictName())
                    .wardName(dto.getWardName())
                    .build();

            UserAddress saved = userAddressRepository.save(address);
            return ResponseEntity.ok(UserAddressDto.from(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private String getUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token không hợp lệ");
        }
        String jwt = authHeader.substring(7);
        String email = jwtUtils.getEmailFromToken(jwt);
        if (email == null || !jwtUtils.validateToken(jwt)) {
            throw new RuntimeException("Token không hợp lệ");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
        return user.getId();
    }
}
