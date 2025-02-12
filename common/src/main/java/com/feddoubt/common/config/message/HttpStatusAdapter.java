package com.feddoubt.common.config.message;

import org.springframework.http.HttpStatus;

public class HttpStatusAdapter implements ApiStatus {
    private final HttpStatus httpStatus;

    public HttpStatusAdapter(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    @Override
    public int value() {
        return httpStatus.value();
    }

    @Override
    public String getReasonPhrase() {
        return httpStatus.getReasonPhrase();
    }
}
