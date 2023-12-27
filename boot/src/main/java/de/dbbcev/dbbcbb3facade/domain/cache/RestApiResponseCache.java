package de.dbbcev.dbbcbb3facade.domain.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document
public class RestApiResponseCache {
    @Id
    private String apiRequestKey;

    private String apiRequestAsString;
    private String apiRequestUrl;

    private Date lastAccess;
    private String responseClassName;
    private Object response;
}
