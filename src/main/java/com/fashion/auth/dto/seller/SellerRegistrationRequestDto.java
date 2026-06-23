package com.fashion.auth.dto.seller;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellerRegistrationRequestDto {
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String cccdFrontUrl;  
    private String cccdBackUrl;  
}
