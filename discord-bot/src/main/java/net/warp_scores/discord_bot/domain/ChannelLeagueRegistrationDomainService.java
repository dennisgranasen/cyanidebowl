package net.warp_scores.discord_bot.domain;

import discord4j.common.util.Snowflake;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.League;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelLeagueRegistrationDomainService {

    private final ChannelLeagueRegistrationRepository channelLeagueRegistrationRepository;

    private final SequenceGenerator sequenceGenerator;

    @Transactional
    public List<ChannelLeagueRegistration> findByChannelId(Snowflake channelId) {
        return channelLeagueRegistrationRepository.findByChannelId(
                channelId.asLong());
    }

    @Transactional
    public ChannelLeagueRegistration update(ChannelLeagueRegistration channelLeagueRegistration) {
        return channelLeagueRegistrationRepository.save(channelLeagueRegistration);
    }

    @Transactional
    public ChannelLeagueRegistration createOrUpdateChannelLeagueRegistration(Snowflake channelId,
            League league,
            boolean spoiler) {
        Optional<ChannelLeagueRegistration> optionalDbValue = channelLeagueRegistrationRepository.findByChannelIdAndLeagueUuid(
                channelId.asLong(), league.getLeagueId());
        ChannelLeagueRegistration channelLeagueRegistration = optionalDbValue.orElse(
                this.newChannelLeagueRegistration());
        channelLeagueRegistration.setLeagueUuid(league.getLeagueId());
        channelLeagueRegistration.setChannelId(channelId.asLong());
        channelLeagueRegistration.setSpoiler(spoiler);
        return channelLeagueRegistrationRepository.save(channelLeagueRegistration);
    }

    @Transactional
    public boolean removeChannelLeagueRegistration(Snowflake channelId, UUID uuid) {
        Optional<ChannelLeagueRegistration> channelLeagueRegistration = channelLeagueRegistrationRepository.findByChannelIdAndLeagueUuid(
                channelId.asLong(), uuid.toString());
        Optional<Long> id = channelLeagueRegistration.map(ChannelLeagueRegistration::getId);
        id.ifPresent(channelLeagueRegistrationRepository::deleteById);
        return id.isPresent();
    }

    private ChannelLeagueRegistration newChannelLeagueRegistration() {
        ChannelLeagueRegistration channelLeagueRegistration = new ChannelLeagueRegistration();
        channelLeagueRegistration.setId(sequenceGenerator.nextIdFor(ChannelLeagueRegistration.class));
        return channelLeagueRegistration;
    }
}
