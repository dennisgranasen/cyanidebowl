package net.warp_scores.discord_bot.domain;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelLeagueRegistrationRepository extends MongoRepository<ChannelLeagueRegistration, Long> {
    Optional<ChannelLeagueRegistration> findByChannelIdAndLeagueUuid(Long channelId, String leagueUuid);

    List<ChannelLeagueRegistration> findByChannelId(Long channelId);
}
