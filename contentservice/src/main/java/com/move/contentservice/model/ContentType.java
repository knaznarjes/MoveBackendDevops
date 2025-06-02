package com.move.contentservice.model;

public enum ContentType {
    TRAVEL_STORY("TravelStory"),
    ITINERARY("Itinerary");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ContentType fromValue(String value) {
        for (ContentType type : ContentType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown content type: " + value);
    }
}