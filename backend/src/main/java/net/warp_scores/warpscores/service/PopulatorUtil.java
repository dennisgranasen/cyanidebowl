package net.warp_scores.warpscores.service;



import com.fasterxml.jackson.annotation.JsonAlias;

import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.cyanide.api.model.ApiLeague;
import net.warp_scores.warpscores.cyanide.api.model.ApiContest;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Identifiable;
import net.warp_scores.warpscores.utils.ConverterRegistry;
import net.warp_scores.warpscores.utils.FieldHandler;
import net.warp_scores.warpscores.utils.FieldHandlerRegistry;
import net.warp_scores.warpscores.utils.TypeConverter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.el.lang.ELArithmetic.BigDecimalDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulatorUtil {
    private static final Logger log = LoggerFactory.getLogger(PopulatorUtil.class);
    protected static final ConverterRegistry converterRegistry = new ConverterRegistry();
    protected static final FieldHandlerRegistry fieldHandlerRegistry = new FieldHandlerRegistry();

    private static class CommaSeparatedStringArrayHandler implements FieldHandler<String> {
        private final String targetFieldName;
        public CommaSeparatedStringArrayHandler(String targetFieldName) {
            this.targetFieldName = targetFieldName;
        }
        @Override
        public void handle(String sourceValue, Object target) throws Exception {
            if (sourceValue == null || !(sourceValue instanceof String)  || sourceValue.isEmpty()) {
                return; // No league names to process
            }
            String[] newValues = sourceValue.split(",");
            target.getClass().getMethod("set" + 
                Character.toUpperCase(targetFieldName.charAt(0)) + 
                targetFieldName.substring(1), String[].class)
                .invoke(target, (Object) newValues);
        }
    };

    private static class CommaSeparatedIdArrayHandler implements FieldHandler<String> {
        private final String targetFieldName;
        public CommaSeparatedIdArrayHandler(String targetFieldName) {
            this.targetFieldName = targetFieldName;
        }
        @Override
        public void handle(String sourceValue, Object target) throws Exception {
            if (sourceValue == null || !(sourceValue instanceof String)  || sourceValue.isEmpty()) {
                return; // No league names to process
            }
            if (Identifiable.class.isAssignableFrom(target.getClass())) {
                Identity targetId = ((Identifiable) target).getId();
                if (targetId == null) {
                    log.error("Target object {} has no valid opus for field {}", target, targetFieldName);
                    return; // No opus to process
                }
                int  opus = targetId.getOpus();
                String[] ids = sourceValue.split(",");
                Identity[] newValues = Arrays.stream(ids)
                    .map(id -> new SimpleIdentity(id.trim(), opus))
                    .toArray(Identity[]::new);
                target.getClass().getMethod("set" +
                    Character.toUpperCase(targetFieldName.charAt(0)) + 
                    targetFieldName.substring(1), Identity[].class)
                    .invoke(target, (Object) newValues);                    
            } else {
                log.error("Target object {} is not Identifiable for field {}", target, targetFieldName);
                return; // No opus to process
            }
        }
    };

    private static class CompetitionStatusNameHandler implements FieldHandler<CompetitionStatus> {
        @Override
        public void handle(CompetitionStatus sourceValue, Object target) throws Exception {
            if (sourceValue == null || !(sourceValue instanceof CompetitionStatus)) {
                log.error("Invalid source value for CompetitionStatusNameHandler: {}", (Object) sourceValue);
                return; // No status name to process
            }
            Competition competition = (Competition) target;
            competition.setStatus(sourceValue);
        }
    }

    private static class CompetitionStatusHandler implements FieldHandler<Integer> {
        @Override
        public void handle(Integer sourceValue, Object target) throws Exception {
            if (sourceValue == null || !(sourceValue instanceof Integer)) {
                log.error("Invalid source value for CompetitionStatusHandler: {}", (Object) sourceValue);
                return; // No status to process
            }
            Competition competition = (Competition) target;
            competition.setStatusNumber(sourceValue); // Convert to zero-based index
        }
    }

    private static class CompetitionLeagueHandler implements FieldHandler<ApiLeague> {
        @Override
        public void handle(ApiLeague sourceValue, Object target) throws Exception {
            if (sourceValue == null || !(sourceValue instanceof ApiLeague)) {
                log.error("Invalid source value for CompetitionLeagueHandler: {}", (Object) sourceValue);
                return; // No league to process
            }
            Competition competition = (Competition) target;
            competition.setLeagueId(
                new SimpleIdentity(sourceValue.getId(),
                                    competition.getId().getOpus()));
            competition.setLeagueName(sourceValue.getName());
        }
    }

    private static class ContestOpponentHandler implements FieldHandler<ApiContest.Opponent[]> {
        @Override
        @SuppressWarnings("unchecked")
        public void handle(ApiContest.Opponent[] sourceValue, Object target) throws Exception {
            if (sourceValue == null || !(sourceValue instanceof ApiContest.Opponent[])) {
                log.error("Invalid source value for ContestOpponentHandler: {}", (Object) sourceValue);
                return; // No opponents to process
            }
            Contest contest = (Contest) target;
            Stream<Team> opponents = Arrays.stream((ApiContest.Opponent[])sourceValue).map(o -> {
                    if (o == null) {
                        log.error("Skipping null opponent in ContestOpponentHandler");
                        return null; // Skip null opponents
                    }
                    if (o.getTeam() == null) {
                        log.error("Skipping opponent with null team in ContestOpponentHandler");
                        return null; // Skip opponents with null teams
                    }
                    ApiContest.Coach coach = o.getCoach();
                    ApiContest.Team team = o.getTeam();
                    Team opponentTeam = new Team(new SimpleIdentity(team.getId(), 
                                                                    contest.getId().getOpus()));
                    opponentTeam.setName(team.getName());
                    Integer i;
                    String s;
                    i = team.getDeath();
                    if (i != null)
                        opponentTeam.setDeath(i);
                    s = team.getLogo();
                    if (s != null && !s.isEmpty())
                        opponentTeam.setLogo(s);
                    s = team.getMotto();
                    if (s != null && !s.isEmpty())
                        opponentTeam.setMotto(s);
                    s = team.getRace();
                    if (s != null && !s.isEmpty())
                        opponentTeam.setRace(s);
                    i = team.getScore();
                    if (i != null)
                        opponentTeam.setScore(i);
                    BigDecimal value = team.getValue();
                    if (value != null)
                        opponentTeam.setValue(value);

                    s = coach.getId();
                    if (s == null || s.isEmpty())
                        log.error("No coach ID found for opponent team {} in contest {}",
                            opponentTeam.getName(), contest.getContestId());
                    else
                        opponentTeam.setCoachId(new SimpleIdentity(s, contest.getId().getOpus()));
                    s = coach.getName();
                    if (s == null || s.isEmpty())
                        log.error("No coach name found for opponent team {} in contest {}",
                            opponentTeam.getName(), contest.getContestId());
                    else
                        opponentTeam.setCoachName(s);
                    return opponentTeam;
                });
            contest.setOpponents(opponents.toArray(Team[]::new));
        }
    }

    static {
        fieldHandlerRegistry.register("leagueNames", Team.class, new CommaSeparatedStringArrayHandler("leagueNames"));
        //fieldHandlerRegistry.register("leagueId", Team.class, new CommaSeparatedStringArrayHandler("leagueIds"));
        fieldHandlerRegistry.register("leagueIds", Team.class, new CommaSeparatedIdArrayHandler("leagueIds"));
        fieldHandlerRegistry.register("leagueNames", Team.class, new CommaSeparatedStringArrayHandler("leagueNames"));
        fieldHandlerRegistry.register("competitionIds", Team.class, new CommaSeparatedIdArrayHandler("competitionIds"));
        fieldHandlerRegistry.register("competitionNames", Team.class, new CommaSeparatedStringArrayHandler("competitionNames"));        
        fieldHandlerRegistry.register("statusName", Competition.class, new CompetitionStatusNameHandler());
        fieldHandlerRegistry.register("status", Competition.class, new CompetitionStatusHandler());
        fieldHandlerRegistry.register("league", Competition.class, new CompetitionLeagueHandler());
        fieldHandlerRegistry.register("opponents", Contest.class, new ContestOpponentHandler());
    }

    public static void copyNonNullProperties(Object source, Object destination) {
        copyProperties(source, destination, true);
    }

    public static void copyProperties(Object source, Object destination, boolean ignoreNullProperties) {
        copyWithAliases(source, destination, ignoreNullProperties);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void copyWithAliases(Object source, Object target, boolean ignoreNullProperties) {
        Map<String, Field> targetFields = new HashMap<>();
        // Collect all fields from target class (including superclasses)
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field f : clazz.getDeclaredFields()) {
                targetFields.put(f.getName(), f);
                JsonAlias alias = f.getAnnotation(JsonAlias.class);
                if (alias != null) {
                    for (String a : alias.value()) {
                        targetFields.put(a, f);
                    }
                }
            }
        }
        Integer opus = null;
        if (Identifiable.class.isAssignableFrom(target.getClass())) {
            Identity id = ((Identifiable) target).getId();
            opus = id.getOpus();
        }
        // Iterate source fields (including superclasses)
        for (Class<?> clazz = source.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field srcField : clazz.getDeclaredFields()) {
                srcField.setAccessible(true);
                Object value;
                try {
                    value = srcField.get(source);
                } catch (IllegalAccessException e) {
                    continue;
                }
                if (ignoreNullProperties && value == null) continue;
                // Collect all possible source names (name + aliases)
                Set<String> possibleNames = new HashSet<>();
                possibleNames.add(srcField.getName());
                JsonAlias srcAlias = srcField.getAnnotation(JsonAlias.class);
                if (srcAlias != null) {
                    possibleNames.addAll(Arrays.asList(srcAlias.value()));
                }                

                // If there is a custom handler for this field, use it
                FieldHandler handler = null;
                for (String name : possibleNames) {
                    handler = fieldHandlerRegistry.getHandler(name, target.getClass());
                    if (handler != null) break;
                }
                if (handler != null) {
                    try {
                        handler.handle(value, target);
                        continue; // When handled, skip the restp
                    } catch (Exception e) {
                        log.error("Custom handler failed for {}: {}", srcField.getName(), e.getMessage(), e);
                    }
                } else {
                    // No custom handler, find matching target field
                    Field tgtField = null;
                    for (String name : possibleNames) {
                        tgtField = targetFields.get(name);
                        if (tgtField != null) break;
                    }

                    if (tgtField == null) {
                        // No matching target field found. This is odd.
                        log.error(null != opus ?
                            "No matching field found for {}.{} of type {} in {} (opus: {})" :
                            "No matching field found for {}.{} of type {} in {}",
                            clazz.getName(), srcField.getName(), 
                            srcField.getType().getSimpleName(),
                            target.getClass().getSimpleName(), opus);
                        continue; // Skip to next source field
                    }

                    if (Modifier.isFinal(tgtField.getModifiers())) {
                            if (tgtField.getName().equals("id")) {
                                continue; // Skip Identity id as it is handled in the constructor
                            }
                            log.warn("Skipping final field: {}.{}", 
                                target.getClass().getSimpleName(), tgtField.getName());
                            continue; // Skip final fields                        
                        }
                    else {
                        String setterName = "set" +
                            Character.toUpperCase(tgtField.getName().charAt(0)) +
                            tgtField.getName().substring(1);
                        Object newValue = value;

                        if (tgtField.getType() != srcField.getType()) {
                            if (tgtField.getType().isArray()) {
                                if (srcField.getType().isArray()) {
                                    Class<?> tgtComponentType = tgtField.getType().getComponentType();
                                    Class<?> srcComponentType = srcField.getType().getComponentType();
                                    if (tgtComponentType.equals(srcComponentType)) {
                                        // Safe to copy the array
                                        newValue = value;
                                    } else {
                                        TypeConverter c = converterRegistry.getConverter(srcComponentType, tgtComponentType);
                                        if (c != null) {
                                            Object[] srcArray = (Object[]) value;
                                            Object[] tgtArray = (Object[]) java.lang.reflect.Array.newInstance(tgtComponentType, srcArray.length);
                                            for (int i = 0; i < srcArray.length; i++) {
                                                tgtArray[i] = c.convert(srcArray[i], opus);
                                            }
                                            newValue = tgtArray;
                                        } else {
                                            log.warn("No converter found for {} to {} for field: {}.{}",
                                                srcComponentType.getSimpleName(), tgtComponentType.getSimpleName(),
                                                target.getClass().getSimpleName(), tgtField.getName());
                                            continue;
                                        }
                                    }
                                } else {
                                    log.warn("Cannot assign non-array to array field: {}.{}", 
                                        target.getClass().getSimpleName(), tgtField.getName());
                                    continue;
                                }
                            } else if (Collection.class.isAssignableFrom(tgtField.getType())) {
                                log.warn("Collection fields are not handled in this method: {}.{}",
                                    target.getClass().getSimpleName(), tgtField.getName());
                                continue; // All collections are handled outside this loop
                            } else if (Identity.class.isAssignableFrom(tgtField.getType()) && opus != null) {
                                newValue = new SimpleIdentity(value, opus);
                            } else {
                                TypeConverter c = converterRegistry.getConverter(srcField.getType(), tgtField.getType());
                                if (c != null) {                                
                                    newValue = c.convert(value, opus); // Ensure the converter is called;
                                } else {
                                    log.warn("No converter found for {} to {} for field: {}.{}",
                                        srcField.getType().getSimpleName(), tgtField.getType().getSimpleName(),
                                        target.getClass().getSimpleName(), tgtField.getName());
                                    continue;
                                }
        
                            }
                        }                    

                        try {
                            Method setter = target.getClass().getMethod(setterName, tgtField.getType());
                            // Cast to Object to ensure the array (if any) is passed as a single argument
                            // and to avoid varargs/heap-pollution compiler warnings.
                            setter.invoke(target, (Object) newValue);
                            continue;
                        } catch (NoSuchMethodException e) {
                            tgtField.setAccessible(true);
                            try {
                                tgtField.set(target, newValue);
                            } catch (IllegalAccessException ignored) {
                                log.warn("Cannot access field {} on {}: {}", tgtField.getName(), target.getClass().getSimpleName(), ignored.getMessage());
                                continue; // Skip inaccessible fields
                            }
                        } catch (IllegalAccessException ignored) {
                            log.warn("Cannot access setter {} on {}: {}", setterName, target.getClass().getSimpleName(), ignored.getMessage());
                            continue; // Skip inaccessible setters
                        } catch (InvocationTargetException e) {
                            log.error("Error invoking setter {} on {}: {}", setterName, target.getClass().getSimpleName(), e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }
}
