package com.feddoubt.common.config.message;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> implements Serializable {

	private static final long serialVersionUID = 1L;
    private int code;
    private String message;
    private boolean success;
    private T data;
}