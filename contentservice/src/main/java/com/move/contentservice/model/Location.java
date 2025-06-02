package com.move.contentservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Location")

public class Location {
    @Id
    private String id;
    private String address;
    private String country;
    private Double lat;
    private Double lon;

}
