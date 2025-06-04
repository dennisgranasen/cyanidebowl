package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.cyanide.api.model.ApiPlayer;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamPopulator {

    private final UUIDConverter uuidConverter;

    public void populateTeamTeam(ApiTeam sourceApiTeam, 
        TeamResponse.Player[] apiPlayers, Team targetTeam) {
        populateTeam(sourceApiTeam, targetTeam);
        targetTeam.setPlayers(toPlayersFromTeamTeam(apiPlayers));
    }

    public void populateMatchTeam(ApiTeam sourceApiTeam, Team targetTeam) {
        populateTeam(sourceApiTeam, targetTeam);
        targetTeam.setPlayers(toPlayersFromMatchTeam(sourceApiTeam.getRoster()));
    }

    private List<Player> toPlayersFromMatchTeam(ApiPlayer[] apiPlayers) {
        if (apiPlayers == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiPlayers).map(this::toPlayerFromMatchTeam)
            .collect(Collectors.toList());
    }

    private Player toPlayerFromMatchTeam(ApiPlayer apiPlayer) {
        Player player = new Player();
        PopulatorUtil.copyNonNullProperties(apiPlayer, player);
        player.setId(uuidConverter.toUuid(apiPlayer.getId()).orElse(null));
        player.setValue(apiPlayer.getValue());
        player.setRaceId(apiPlayer.getIdraces());
        player.setSuspendedNextMatch(apiPlayer.getSuspended_next_match());
        player.setMatchplayed(apiPlayer.getMatchplayed());
        player.setStats(toStats(apiPlayer.getStats()));
        player.setMvp(apiPlayer.getMvp());
        player.setAttributes(toAttributes(apiPlayer.getAttributes()));
        player.setExtendedAttributes(toExtendedAttributes(apiPlayer.getExtendedAttributes()));
        player.setCasualtiesStateIds(apiPlayer.getCasualties_state_id());
        player.setCasualtiesStates(apiPlayer.getCasualties_state());
        return player;
    }

    private void populateTeam(ApiTeam sourceApiTeam, Team targetTeam) {
        PopulatorUtil.copyNonNullProperties(sourceApiTeam, targetTeam);
        targetTeam.setId(uuidConverter.toUuid(sourceApiTeam.getId()).orElse(null));
        targetTeam.setCompetitionIds(
                uuidConverter.toUuids(targetTeam.getCompetitionIds(),
                                      sourceApiTeam.getBb3_competition_id()));
        targetTeam.setLeagueIds(
                uuidConverter.toUuids(targetTeam.getLeagueIds(), sourceApiTeam.getLeagueId()));
    }

    private List<Player> toPlayersFromTeamTeam(TeamResponse.Player[] apiPlayers) {
        if (apiPlayers == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiPlayers).map(this::toPlayerFromTeamTeam)
            .collect(Collectors.toList());
    }

    private Player toPlayerFromTeamTeam(TeamResponse.Player apiPlayer) {
        Player player = new Player();
        PopulatorUtil.copyNonNullProperties(apiPlayer, player);
        player.setId(uuidConverter.toUuid(apiPlayer.getId()).orElse(null));
        player.setRaceId(apiPlayer.getIdraces());
        player.setSuspendedNextMatch(apiPlayer.getSuspended_next_match());
        player.setAttributes(toAttributes(apiPlayer.getAttributes()));
        player.setExtendedAttributes(toExtendedAttributes(apiPlayer.getExtendedAttributes()));
        player.setCasualtiesStateIds(apiPlayer.getCasualties_state_id());
        player.setCasualtiesStates(apiPlayer.getCasualties_state());
        return player;
    }

    private Player.Attributes toAttributes(TeamResponse.Player.Attributes apiAttributes) {
        Player.Attributes attributes = new Player.Attributes();
        PopulatorUtil.copyNonNullProperties(apiAttributes, attributes);
        return attributes;
    }

    private Player.Attributes toAttributes(ApiPlayer.Attributes apiAttributes) {
        Player.Attributes attributes = new Player.Attributes();
        PopulatorUtil.copyNonNullProperties(apiAttributes, attributes);
        return attributes;
    }

    private Player.ExtendedAttributes toExtendedAttributes(
            TeamResponse.Player.ExtendedAttributes apiExtendedAttributes) {
        Player.ExtendedAttributes extendedAttributes = new Player.ExtendedAttributes();

        Player.Attributes defaultAttributes =
            toAttributes(apiExtendedAttributes.getDefaultAttributes());
        extendedAttributes.setDefaultAttributes(defaultAttributes);
        PopulatorUtil.copyNonNullProperties(apiExtendedAttributes, extendedAttributes);
        return extendedAttributes;
    }

    private Player.ExtendedAttributes toExtendedAttributes(
            ApiPlayer.ExtendedAttributes apiExtendedAttributes) {
        Player.ExtendedAttributes extendedAttributes = new Player.ExtendedAttributes();

        Player.Attributes defaultAttributes = new Player.Attributes();
        defaultAttributes.setAg(apiExtendedAttributes.getAg().getValue());
        defaultAttributes.setMa(apiExtendedAttributes.getMa().getValue());
        defaultAttributes.setAv(apiExtendedAttributes.getAv().getValue());
        defaultAttributes.setPa(apiExtendedAttributes.getPa().getValue());
        defaultAttributes.setSt(apiExtendedAttributes.getSt().getValue());
        extendedAttributes.setDefaultAttributes(defaultAttributes);

        List<LinkedHashMap<String, Integer>> maluses = collect(apiExtendedAttributes,
                ApiPlayer.ExtendedAttributes.ExtendedAttribute::getMaluses);
        List<LinkedHashMap<String, Integer>> bonuses = collect(apiExtendedAttributes,
                ApiPlayer.ExtendedAttributes.ExtendedAttribute::getBonuses);
        extendedAttributes.setMalus(maluses);
        extendedAttributes.setBonus(bonuses);
        PopulatorUtil.copyNonNullProperties(apiExtendedAttributes, extendedAttributes);
        return extendedAttributes;
    }

    private Player.Stats toStats(ApiPlayer.Stats apiStats) {
        if ( apiStats == null ) {
            return null;
        }
        Player.Stats stats = new Player.Stats();
        PopulatorUtil.copyNonNullProperties(apiStats, stats);
        return stats;
    }

    private List<LinkedHashMap<String, Integer>> collect(
            ApiPlayer.ExtendedAttributes apiExtendedAttributes,
            Function<ApiPlayer.ExtendedAttributes.ExtendedAttribute, Integer> getter) {
        List<LinkedHashMap<String, Integer>> collected = new ArrayList<>();
        collected.add(toMap("ma", getter.apply(apiExtendedAttributes.getMa())));
        collected.add(toMap("st", getter.apply(apiExtendedAttributes.getSt())));
        collected.add(toMap("ag", getter.apply(apiExtendedAttributes.getAg())));
        collected.add(toMap("pa", getter.apply(apiExtendedAttributes.getPa())));
        collected.add(toMap("av", getter.apply(apiExtendedAttributes.getAv())));
        return collected;
    }

    private LinkedHashMap<String, Integer> toMap(String name, Integer value) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put(name, value);
        return map;
    }

}
