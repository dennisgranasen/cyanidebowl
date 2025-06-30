package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import java.util.Date;

public record DateForId(Identity id, Date date) {}


