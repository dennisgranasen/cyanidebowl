package net.warp_scores.warpscores.cyanide.api.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(of = {"type", "name", "amount"})
@ToString(of = {"type", "name", "amount"})
public class ApiCard {
   private String type;
   private String name;
   private Integer amount;
}