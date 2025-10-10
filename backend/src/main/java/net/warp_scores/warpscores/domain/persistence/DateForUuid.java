package net.warp_scores.warpscores.domain.persistence;

import java.util.Date;
import net.warp_scores.warpscores.identity.Identity;

public record DateForUuid(Identity uuid, Date date) {}


