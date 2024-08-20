package net.warp_scores.discord_bot.domain;

import discord4j.common.util.Snowflake;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Document
@Getter
@Setter
public class ChannelLeagueRegistration {
    @Id
    private Long id;
    private Long channelId;
    private String leagueUuid;
    private Boolean spoiler = false;
    private Date lastPublishedMatchDate;
}
