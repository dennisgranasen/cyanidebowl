package de.dbbcev.dbbcbb3facade.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document
@ToString
@NoArgsConstructor
public class Status implements UpdateableFromApi {
    @Id
    private String gameName;
    private boolean gameServerDatabase;
    private boolean gameServerAddressDirectory;
    private Maintenance maintenance;
    private String[] socialLinks;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheck;

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class Maintenance {
        private Object[] pc;
        private Object[] microsoft;
        private Object[] sony;
    }
}
