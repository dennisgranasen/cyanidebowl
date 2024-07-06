package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document
@ToString
public class Circuit {
    @Id
    private Integer circuitId;

    private String circuitName;

    private List<CircuitLeg> circuitLegs = new ArrayList<CircuitLeg>();

    public void addLeg(CircuitLeg circuitLeg) {
        circuitLegs.add(circuitLeg);
    }
}