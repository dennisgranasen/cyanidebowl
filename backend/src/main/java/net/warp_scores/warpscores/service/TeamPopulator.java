package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.cyanide.api.model.ApiCard;
import net.warp_scores.warpscores.cyanide.api.model.ApiPlayer;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse.Player.Attributes;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.utils.FieldHandler;
import net.warp_scores.warpscores.utils.TypeConverter;

import org.springframework.stereotype.Service;

import static java.util.Comparator.nullsLast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
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
    private static final Logger log = LoggerFactory.getLogger(TeamPopulator.class);


    /*
    private static class AttributeConverter implements TypeConverter<TeamResponse.Player.Attributes, Player.Attributes> {
        @Override
        public Player.Attributes convert(TeamResponse.Player.Attributes source) {            
            Player.Attributes target = new Player.Attributes();
            PopulatorUtil.copyNonNullProperties(source, target);
            return target;
        }
    }

    static{
        // Register default converters if needed
        // converterRegistry.register(...);
        PopulatorUtil.converterRegistry.register(
            TeamResponse.Player.Attributes.class,
            Player.Attributes.class,
            new AttributeConverter());
    };
    */

    private static class LeagueIdHandler implements FieldHandler<String> {
        @Override
        public void handle(String sourceValue, Object target) throws Exception {
            if (sourceValue == null || !(sourceValue instanceof String)) {
                return; // No league id to process
            }
            String str_lid = (String) sourceValue;
            if (str_lid.isEmpty()) {
                return; // No league id to process
            }
            Team targetTeam = (Team) target;
            List<Identity> existing = new ArrayList<>(Arrays.asList(targetTeam.getLeagueIds()));
            Identity newId = new SimpleIdentity(str_lid, targetTeam.getId().getOpus());
            if (!existing.contains(newId)) {
                existing.add(newId);
                targetTeam.setLeagueIds(existing.toArray(new Identity[0]));
            }
        }
    }

    private static class CardHandler implements FieldHandler<ApiCard[]> {
        @Override
        public void handle(ApiCard[] sourceValue, Object target) throws Exception {
            if (sourceValue == null) {
                return; // No card id to process
            }
            Team targetTeam = (Team) target;
            for (ApiCard card : sourceValue) {
                if (card == null || card.getType() == null || card.getName() == null) {
                    continue; // Skip null cards
                }
                String cardType = card.getType().toLowerCase();
                String cardName = card.getName().toLowerCase();
                if ("staff".equals(cardType)) {
                    if ("cheerleader".equals(cardName)) {
                        targetTeam.setCheerleaders(card.getAmount());
                    } else if ("reroll".equals(cardName)) {
                        targetTeam.setRerolls(card.getAmount());
                    } else if ("fanfactor".equals(cardName)) {
                        targetTeam.setDedicatedFans(card.getAmount());
                    } else if ("apothecary".equals(cardName)) {
                        targetTeam.setApothecary(card.getAmount());
                    } else if ("assistant".equals(cardName)) {
                        targetTeam.setCoachAssistants(card.getAmount());
                    } else if ("necromancer".equals(cardName)) {
                        targetTeam.setNecromancers(card.getAmount());
                    } else {
                        log.warn("Unknown staff card name: {}", cardName);
                    }
                } else if ("sponsor".equals(cardType)) {
                    targetTeam.setSponsor(card.getName());
                } else if ("building".equals(cardType)) {
                    targetTeam.setBuilding(card.getName());
                } else {
                    log.warn("Unknown card [type: {}, name: {}, amount: {}]", 
                        card.getType(), card.getName(), card.getAmount());
                }
            }
        }
    } 
    
    
    {
        PopulatorUtil.fieldHandlerRegistry.register("leagueId",  
            Team.class, new LeagueIdHandler());
        PopulatorUtil.fieldHandlerRegistry.register("cards",  
            Team.class, new CardHandler());
    }
    

    public void populateTeamTeam(ApiTeam sourceApiTeam, 
        TeamResponse.Player[] apiPlayers, Team targetTeam, int opus) {
        populateTeam(sourceApiTeam, targetTeam, opus);
        //targetTeam.setPlayers(toPlayersFromTeamTeam(apiPlayers, opus));
    }

    public void populateMatchTeam(ApiTeam sourceApiTeam, Team targetTeam, int opus) {
        populateTeam(sourceApiTeam, targetTeam, opus);
        //targetTeam.setPlayers(toPlayersFromMatchTeam(sourceApiTeam.getRoster(), opus));
    }

    /*
    private List<Player> toPlayersFromMatchTeam(ApiPlayer[] apiPlayers, int opus) {
        if (apiPlayers == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiPlayers).map((apiPlayer) -> toPlayerFromMatchTeam(apiPlayer, opus))
            .collect(Collectors.toList());
    }

    private Player toPlayerFromMatchTeam(ApiPlayer apiPlayer, int opus) {
        Player player = new Player(new SimpleIdentity(apiPlayer.getId(), opus));
        PopulatorUtil.copyNonNullProperties(apiPlayer, player);
        //player.setId(apiPlayer.getId());
        
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
        */

    private void populateTeam(ApiTeam sourceApiTeam, Team targetTeam, int opus) {
        PopulatorUtil.copyNonNullProperties(sourceApiTeam, targetTeam);
        //targetTeam.setIdentity(sourceApiTeam.getId());
        /*
        // Append the new competition id to the existing array if not already present
        List<Identity> existing = new ArrayList<Identity>();
        Identity newId;
        
        existing = targetTeam.getCompetitionIds();
        String bb3Id = sourceApiTeam.getBb3_competition_id();
        Identity newId = new SimpleIdentity(bb3Id,3);
        if (newId != null && !bb3Id.isEmpty()) {
            boolean alreadyExists = existing != null && Arrays.asList(existing).contains(newId);
            if (!alreadyExists) {
                if (existing == null) {
                    targetTeam.setCompetitionIds(new Identity[]{newId});
                } else {
                    Identity[] combined = Arrays.copyOf(existing, existing.length + 1);
                    combined[existing.length] = newId;
                    targetTeam.setCompetitionIds(combined);
                }
            }
        }
        
        // Append the new league id to the existing array if not already present

        String str_lid = sourceApiTeam.getLeagueId();
        if (str_lid == null || str_lid.isEmpty()) {
            return; // No league id to process
        }
        newId = new SimpleIdentity(str_lid, opus);
        Identity[] lids = targetTeam.getLeagueIds();
        if (lids != null)
            existing.addAll(Arrays.asList(lids));
        if (newId != null && !existing.contains(newId)) {
            existing.add(newId);
            targetTeam.setLeagueIds(existing.toArray(new Identity[0]));
        }
        */
    }

    /*
    private List<Player> toPlayersFromTeamTeam(TeamResponse.Player[] apiPlayers, int opus) {
        if (apiPlayers == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiPlayers).map((apiPlayer) -> toPlayerFromTeamTeam(apiPlayer, opus))
            .collect(Collectors.toList());
    }

    private Player toPlayerFromTeamTeam(TeamResponse.Player apiPlayer, int opus) {
        Player player = new Player(
            new SimpleIdentity(apiPlayer.getId(), opus));
        PopulatorUtil.copyNonNullProperties(apiPlayer, player);
        //player.setRaceId(apiPlayer.getIdraces());
        //player.setSuspendedNextMatch(apiPlayer.getSuspended_next_match());
        
        Attributes attribs = apiPlayer.getAttributes();
        if (attribs != null) {
            player.setAttributes(toAttributes(attribs));
        }
        TeamResponse.Player.ExtendedAttributes ea = apiPlayer.getExtendedAttributes();
        if (ea != null) {
            player.setExtendedAttributes(toExtendedAttributes(ea));
        }
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
    */

}
