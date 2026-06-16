package com.example.myapp.common;

public record ApiResponse<T>(int status, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "created", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "success", null);
    }
}
