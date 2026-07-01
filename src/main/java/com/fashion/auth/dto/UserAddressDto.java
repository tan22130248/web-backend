package com.fashion.auth.dto;

import com.fashion.auth.model.UserAddress;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAddressDto {
    private String id;
    private String userId;
    private String fullName;
    private String phone;
    private String address;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
    private String provinceName;
    private String districtName;
    private String wardName;
    private LocalDateTime createdAt;

    public static UserAddressDto from(UserAddress address) {
        UserAddressDto dto = new UserAddressDto();
        dto.setId(address.getId());
        dto.setUserId(address.getUser().getId());
        dto.setFullName(address.getFullName());
        dto.setPhone(address.getPhone());
        dto.setAddress(address.getAddress());
        dto.setProvinceId(address.getProvinceId());
        dto.setDistrictId(address.getDistrictId());
        dto.setWardCode(address.getWardCode());
        dto.setProvinceName(address.getProvinceName());
        dto.setDistrictName(address.getDistrictName());
        dto.setWardName(address.getWardName());
        dto.setCreatedAt(address.getCreatedAt());
        return dto;
    }
}
