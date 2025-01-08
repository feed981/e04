package com.feddoubt.common.YT1.config.message;

public enum CustomHttpStatus implements ApiStatus {
    URL_CANNOT_BE_NULL_OR_EMPTY(400,"URL cannot be null or empty"),
    INVALID_YOUTUBE_URL(400,"Invalid YouTube URL"),
    TOO_MANY_REQUESTS(429,"Too many requests. Please try again later"),
    ;

    private final int value;
    private final String reasonPhrase;

    CustomHttpStatus(int value, String reasonPhrase) {
        this.value = value;
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public String getReasonPhrase() {
        return reasonPhrase;
    }
}
