package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.cyanide.api.model.ApiCard;
import net.warp_scores.warpscores.cyanide.api.model.ApiPlayer;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.utils.FieldHandler;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamPopulator {
    private static final Logger log = LoggerFactory.getLogger(TeamPopulator.class);

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

    private static class SkillsHandler implements FieldHandler<Object> {
        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void handle(Object sourceValue, Object target) throws Exception {
            Player targetPlayer = (Player) target;
            if (sourceValue == null) {
                targetPlayer.setSkills(null);
                return; // No skills to process
            }
            if (sourceValue instanceof String[]) {
                targetPlayer.setSkillStrings((String[]) sourceValue);
            } else if (sourceValue instanceof ApiPlayer.Skills) {
                ApiPlayer.Skills aSkills = (ApiPlayer.Skills) sourceValue;
                Player.Skills pSkills = new Player.Skills();
                PopulatorUtil.copyNonNullProperties(aSkills, pSkills);
                targetPlayer.setSkills(pSkills);
            } else if (sourceValue instanceof ArrayList) { //BB1 and BB2
                String[] skills = ((List<?>) sourceValue).stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .collect(Collectors.toList())
                    .toArray(new String[0]);
                targetPlayer.setSkillStrings(skills);
            } else if (sourceValue instanceof LinkedHashMap) { //BB3
                ArrayList<String> is = ((ArrayList<String>)((LinkedHashMap)sourceValue).get("InnateSkills"));
                ArrayList<String> ac = ((ArrayList<String>)((LinkedHashMap)sourceValue).get("AcquiredSkills"));
                Player.Skills skills = new Player.Skills();
                if (is != null) {
                    skills.setInnateSkills(is.toArray(new String[0]));
                }
                if (ac != null) {
                    skills.setAcquiredSkills(ac.toArray(new String[0]));
                }
                targetPlayer.setSkills(skills);
                //targetPlayer.setSkillStrings(skills);
            } else {
                log.error("Unexpected skills type: {}", sourceValue.getClass().getName());
            }
        }
    }   
    
    private static class CasualtiesStateIdHandler implements FieldHandler<Integer[]> {
        @Override
        public void handle(Integer[] sourceValue, Object target) throws Exception {
            Player targetPlayer = (Player) target;
            if (sourceValue != null)
                targetPlayer.setCasualtiesStateIds(sourceValue);            
        }
    }

    private static class CasualtiesStateHandler implements FieldHandler<String[]> {
        @Override
        public void handle(String[] sourceValue, Object target) throws Exception {
            Player targetPlayer = (Player) target;
            if (sourceValue != null)
                targetPlayer.setCasualtiesState(sourceValue);            
        }
    }

    private static class SuspendedNextMatchHandler implements FieldHandler<Boolean> {
        @Override
        public void handle(Boolean sourceValue, Object target) throws Exception {
            Player targetPlayer = (Player) target;
            if (sourceValue != null)
                targetPlayer.setSuspendedNextMatch(sourceValue);
        }
    }
    
    {
        PopulatorUtil.fieldHandlerRegistry.register("leagueId",  
            Team.class, new LeagueIdHandler());
        PopulatorUtil.fieldHandlerRegistry.register("cards",  
            Team.class, new CardHandler());
        PopulatorUtil.fieldHandlerRegistry.register("casualties_state_id",
            Player.class, new CasualtiesStateIdHandler());
        PopulatorUtil.fieldHandlerRegistry.register("casualties_state",
            Player.class, new CasualtiesStateHandler());
        PopulatorUtil.fieldHandlerRegistry.register("suspended_next_match",
            Player.class, new SuspendedNextMatchHandler());
        PopulatorUtil.fieldHandlerRegistry.register("skills",
            Player.class, new SkillsHandler());
    }
    
    public void populateTeamTeam(ApiTeam sourceApiTeam, 
        TeamResponse.Player[] apiPlayers, Team targetTeam, int opus) {
        populateTeam(sourceApiTeam, targetTeam, opus);
        //if (apiPlayers != null && apiPlayers.length > 0) 
            //PopulatorUtil.copyProperties(apiPlayers, targetTeam, true);
        targetTeam.setPlayers(
            Arrays.stream(apiPlayers)
                .map((apiPlayer) -> {
                    Player player = new Player(new SimpleIdentity(apiPlayer.getId(), opus));
                    PopulatorUtil.copyNonNullProperties(apiPlayer, player);
                    return player;
                })
                .toArray(Player[]::new)
        );            
    }

    public void populateMatchTeam(ApiTeam sourceApiTeam, Team targetTeam, int opus) {
        populateTeam(sourceApiTeam, targetTeam, opus);
        //targetTeam.setPlayers(toPlayersFromMatchTeam(sourceApiTeam.getRoster(), opus));
    }


    private void populateTeam(ApiTeam sourceApiTeam, Team targetTeam, int opus) {
        PopulatorUtil.copyNonNullProperties(sourceApiTeam, targetTeam);
    }
}

