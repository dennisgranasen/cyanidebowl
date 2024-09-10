package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document
public class NafCoach {
    @Id
    @JsonAlias({"naf_id"})
    private Integer nafId;
    @JsonAlias({"naf_name"})
    private String nafName;
    private String error;

    @Override
    public String toString() {
        return String.format("NafCoach[nafId=%s, nafName=%s]", nafId, nafName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NafCoach)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return this.nafId.equals(((NafCoach) obj).nafId);
    }
}
