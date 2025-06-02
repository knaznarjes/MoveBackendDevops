package com.move.contentservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationDTO {
    private String id;
    private String address;
    private String country;
    private Double lat;
    private Double lon;

}