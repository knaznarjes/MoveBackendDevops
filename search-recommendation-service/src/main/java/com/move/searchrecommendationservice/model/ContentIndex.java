package com.move.searchrecommendationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "move_contents")
public class ContentIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private Double budget;

    @Field(type = FieldType.Integer)
    private Integer rating;

    @Field(type = FieldType.Text)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String type;



    @Field(type = FieldType.Date)
    private Date creationDate;

    @Field(type = FieldType.Date)
    private Date lastModified;
    @Field(type = FieldType.Boolean)

    private Boolean isPublished;
    @Field(type = FieldType.Integer)
    private Integer likeCount;
    @CompletionField(maxInputLength = 100)
    private Completion titleSuggest;

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

}
