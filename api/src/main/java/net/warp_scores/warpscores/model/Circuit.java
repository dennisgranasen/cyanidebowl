package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document
@ToString
public class Circuit {
    @Id
    private Long circuitId;

    @Indexed(unique = true)
    private String circuitName;

    private List<CircuitLeg> circuitLegs = new ArrayList<>();

    public void addLeg(CircuitLeg circuitLeg) {
        circuitLegs.add(circuitLeg);
    }
}
