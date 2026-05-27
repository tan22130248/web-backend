package com.fashion.auth.dto.ghn;

import lombok.Data;

@Data
public class GhnBaseResponse<T> {
    private Integer code;
    private String message;
    private T data;
}
