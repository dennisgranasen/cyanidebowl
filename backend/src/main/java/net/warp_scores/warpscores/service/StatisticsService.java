package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.controller.StatisticsResponse;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.stage.StageMatchView;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.text.Normalizer;
import java.util.*;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private static final int TOP = 10;
    private final SeasonRepository seasons;
    private final StageRepository stages;
    private final StageMatchService stageMatches;
    private final MatchRepository matches;

    @Transactional(readOnly = true)
    @Cacheable(value = "seasonStatistics", key = "#leagueSystemId + ':' + #seasonId")
    public StatisticsResponse.Dashboard season(String leagueSystemId, String seasonId) {
        requireSeason(leagueSystemId, seasonId);
        Dataset data = dataset(List.of(seasonId));
        return new StatisticsResponse.Dashboard(leagueSystemId, seasonId, data.all.size(), data.playerMatchCount,
                data.editions(), playerCategories(data, false, TOP, null), teamCategories(data, TOP, false, null));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "marathonStatistics", key = "#leagueSystemId + ':' + #edition + ':' + #merge + ':' + #page + ':' + #size + ':' + #sort")
    public StatisticsResponse.Marathon marathon(String leagueSystemId, String edition, boolean merge,
            int page, int size, String sort) {
        List<String> seasonIds = seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId).stream().map(Season::getId).toList();
        Dataset all = dataset(seasonIds);
        Dataset filtered = edition == null || edition.equalsIgnoreCase("ALL") ? all : all.filterEdition(edition);
        boolean crossEdition = filtered.editions().size() > 1;
        List<TeamAggregate> teamRows = teamAggregates(filtered, merge, null);
        Comparator<TeamAggregate> comparator = teamComparator(sort).reversed().thenComparing(t -> safe(t.name));
        teamRows.sort(comparator);
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        int from = Math.min(safePage * safeSize, teamRows.size());
        int to = Math.min(from + safeSize, teamRows.size());
        var pageResult = new StatisticsResponse.Page<>(teamRows.subList(from, to).stream()
                .map(t -> t.entry(teamValue(t, sort))).toList(), safePage, safeSize, teamRows.size(),
                (int)Math.ceil(teamRows.size() / (double)safeSize));
        return new StatisticsResponse.Marathon(leagueSystemId, edition == null ? "ALL" : edition, merge,
                filtered.all.size(), all.editions(), crossEdition ? COMPARABLE_PLAYER_KEYS : ALL_PLAYER_KEYS,
                pageResult, playerCategories(filtered, crossEdition, 100, null));
    }

    @Transactional(readOnly = true)
    public StatisticsResponse.Personal personal(String leagueSystemId, Collection<String> coachIds) {
        Set<String> ids = new HashSet<>(coachIds == null ? List.of() : coachIds);
        Dataset data = dataset(seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId).stream().map(Season::getId).toList());
        Dataset own = data.filterCoaches(ids);
        return new StatisticsResponse.Personal(leagueSystemId, ids.stream().sorted().toList(), own.all.size(),
                playerCategories(own, own.editions().size() > 1, TOP, ids), teamCategories(own, TOP, false, ids),
                versus(data, ids));
    }

    private void requireSeason(String system, String id) {
        Season season = seasons.findById(id).orElseThrow(() -> new IllegalArgumentException("Season not found: " + id));
        if (!Objects.equals(system, season.getLeagueSystemId())) throw new IllegalArgumentException("Season is not part of league system");
    }

    private Dataset dataset(List<String> seasonIds) {
        Map<String, Selected> unique = new LinkedHashMap<>();
        Map<String,String> seasonByStage = new HashMap<>();
        for (String seasonId : seasonIds) for (Stage stage : stages.findBySeasonIdOrderBySequenceAsc(seasonId)) {
            seasonByStage.put(stage.getId(), seasonId);
            try {
                for (StageMatchView view : stageMatches.getMatchesForStage(stage.getId())) {
                    if (view.sourceMatchId() == null) continue;
                    String key = view.sourceMatchKey() == null ? view.sourceMatchId().asMongoKey() : view.sourceMatchKey();
                    Selected previous = unique.get(key);
                    if (previous == null) unique.put(key, new Selected(view, null, seasonId));
                    else unique.put(key, previous.merge(view));
                }
            } catch (IllegalArgumentException | IllegalStateException ignored) { }
        }
        Map<String,Match> stored = new HashMap<>();
        List<Identity> ids = unique.values().stream().map(s -> s.view.sourceMatchId()).distinct().toList();
        for (Match match : matches.findAllById(ids)) stored.put(match.getId().asMongoKey(), match);
        List<Selected> selected = unique.values().stream().map(s -> new Selected(s.view,
                stored.get(s.view.sourceMatchId().asMongoKey()), s.seasonId)).filter(s -> s.match != null).toList();
        return new Dataset(selected);
    }

    private static final List<String> COMPARABLE_PLAYER_KEYS = List.of("games","touchdowns","casualties","kills","interceptions","passes","catches","blocks","knockouts","injuries","pushouts","fouls");
    private static final List<String> ALL_PLAYER_KEYS = List.of("touchdowns","casualties","kills","interceptions","spp","mvp","passes","catches","running","passing","blocks","knockouts","injuries","pushouts","fouls","armour-breaks","games");

    private List<StatisticsResponse.Category<StatisticsResponse.PlayerEntry>> playerCategories(Dataset data, boolean comparableOnly, int limit, Set<String> coachFilter) {
        Map<String,PlayerAggregate> players = new LinkedHashMap<>();
        for (Selected selected : data.all) if (selected.view.countsFor().playerStats()) addPlayers(players, selected, coachFilter);
        List<Metric<PlayerAggregate>> metrics = new ArrayList<>(List.of(
                pm("touchdowns","Touchdowns",p->p.touchdowns), pm("casualties","CAS",p->p.casualties),
                pm("kills","Kills",p->p.kills), pm("interceptions","Interceptions",p->p.interceptions),
                pm("passes","Passes",p->p.passes), pm("catches","Catches",p->p.catches),
                pm("blocks","Blocks",p->p.blocks), pm("knockouts","KOs",p->p.knockouts),
                pm("injuries","Injuries",p->p.injuries), pm("pushouts","Push-outs",p->p.pushouts),
                pm("fouls","Fouls",p->p.fouls), pm("games","Matcher",p->p.games)));
        if (!comparableOnly) metrics.addAll(List.of(pm("spp","SPP",p->p.spp), pm("mvp","MVP",p->p.mvp),
                pm("running","Running distance",p->p.running), pm("passing","Passing distance",p->p.passing),
                pm("armour-breaks","Armour breaks",p->p.armourBreaks)));
        return metrics.stream().map(m -> new StatisticsResponse.Category<>(m.key, m.label,
                players.values().stream().filter(p -> m.value.applyAsInt(p) > 0)
                        .sorted(Comparator.comparingInt(m.value).reversed().thenComparing(p -> safe(p.name)))
                        .limit(limit).map(p -> p.entry(m.value.applyAsInt(p))).toList())).filter(c -> !c.entries().isEmpty()).toList();
    }

    private void addPlayers(Map<String,PlayerAggregate> result, Selected selected, Set<String> coachFilter) {
        Team[] teams = selected.match.getTeams(); if (teams == null) return;
        for (int i=0;i<teams.length;i++) {
            Team team=teams[i]; if (team==null || team.getPlayers()==null) continue;
            String coachId=coachId(selected.match,team,i), coachName=coachName(selected.match,team,i);
            if (coachFilter != null && !coachFilter.contains(coachId)) continue;
            for (Player player:team.getPlayers()) if (player!=null) {
                String id=player.getId()==null ? norm(team.getName()+":"+player.getName()+":"+player.getNumber()) : player.getId().asMongoKey();
                result.computeIfAbsent(id,PlayerAggregate::new).add(player,team,coachId,coachName,edition(selected),selected.match.getStarted());
            }
        }
    }

    private List<StatisticsResponse.Category<StatisticsResponse.TeamEntry>> teamCategories(Dataset data, int limit, boolean merge, Set<String> coaches) {
        List<TeamAggregate> teams=teamAggregates(data,merge,coaches);
        return List.of(tm("points","Points",t->t.points),tm("wins","Wins",t->t.wins),tm("games","Games",t->t.games),
                tm("touchdowns","Touchdowns",t->t.tdFor),tm("td-difference","TD difference",t->t.tdFor-t.tdAgainst),
                tm("casualties","CAS",t->t.casFor),tm("cas-difference","CAS difference",t->t.casFor-t.casAgainst)).stream()
                .map(m -> new StatisticsResponse.Category<>(m.key,m.label,teams.stream()
                        .sorted(Comparator.comparingInt(m.value).reversed().thenComparing(t->safe(t.name))).limit(limit)
                        .map(t->t.entry(m.value.applyAsInt(t))).toList())).toList();
    }

    private List<TeamAggregate> teamAggregates(Dataset data, boolean merge, Set<String> coachFilter) {
        Map<String,TeamAggregate> result=new LinkedHashMap<>();
        for (Selected s:data.all) if (s.view.countsFor().teamStats() && s.match.getTeams()!=null) {
            Team[] ts=s.match.getTeams();
            for(int i=0;i<Math.min(2,ts.length);i++) { Team team=ts[i]; if(team==null)continue;
                String cid=coachId(s.match,team,i); if(coachFilter!=null&&!coachFilter.contains(cid))continue;
                String id=merge?norm(team.getName()):team.getId()==null?norm(team.getName()):team.getId().asMongoKey();
                TeamAggregate a=result.computeIfAbsent(id,TeamAggregate::new);
                int own=score(s,i),opp=score(s,1-i); Team other=ts.length>1?ts[1-i]:null;
                a.add(team,cid,coachName(s.match,team,i),edition(s),s.seasonId,own,opp,
                        val(team.getInflictedcasualties()),other==null?0:val(other.getInflictedcasualties()));
            }
        }
        return new ArrayList<>(result.values());
    }

    private List<StatisticsResponse.CoachVersus> versus(Dataset data, Set<String> own) {
        Map<String,VersusAggregate> result=new LinkedHashMap<>();
        for(Selected s:data.all) if(s.view.countsFor().teamStats()&&s.match.getTeams()!=null&&s.match.getTeams().length>1) {
            Team[] t=s.match.getTeams(); String c0=coachId(s.match,t[0],0),c1=coachId(s.match,t[1],1);
            int side=own.contains(c0)?0:own.contains(c1)?1:-1; if(side<0)continue;
            String oid=side==0?c1:c0; if(oid==null||own.contains(oid))continue;
            VersusAggregate a=result.computeIfAbsent(oid,k->new VersusAggregate(oid,coachName(s.match,t[1-side],1-side)));
            a.add(score(s,side),score(s,1-side),val(t[side].getInflictedcasualties()),val(t[1-side].getInflictedcasualties()));
        }
        return result.values().stream().sorted(Comparator.comparingInt((VersusAggregate v)->v.games).reversed())
                .map(VersusAggregate::entry).toList();
    }

    private int score(Selected s,int index) { Integer value=s.view.officialScore()==null?null:(index==0?s.view.officialScore().home():s.view.officialScore().away()); return value==null?val(s.match.getTeams()[index].getScore()):value; }
    private String edition(Selected s){ return s.view.game()==null?"UNKNOWN":s.view.game().name(); }
    private String coachId(Match m,Team t,int i){ if(t.getCoachId()!=null)return t.getCoachId().getValue(); return m.getCoaches()!=null&&i<m.getCoaches().length&&m.getCoaches()[i]!=null?m.getCoaches()[i].getId():null; }
    private String coachName(Match m,Team t,int i){ if(t.getCoachName()!=null)return t.getCoachName(); return m.getCoaches()!=null&&i<m.getCoaches().length&&m.getCoaches()[i]!=null?m.getCoaches()[i].getName():null; }
    private Comparator<TeamAggregate> teamComparator(String sort){ return Comparator.comparingInt(t->teamValue(t,sort)); }
    private int teamValue(TeamAggregate t,String sort){ return switch(sort==null?"points":sort){case"wins"->t.wins;case"games"->t.games;case"touchdowns"->t.tdFor;case"casualties"->t.casFor;default->t.points;}; }
    private static Metric<PlayerAggregate> pm(String k,String l,ToIntFunction<PlayerAggregate> f){return new Metric<>(k,l,f);} private static Metric<TeamAggregate> tm(String k,String l,ToIntFunction<TeamAggregate> f){return new Metric<>(k,l,f);}
    private static int val(Integer i){return i==null?0:i;} private static String safe(String s){return s==null?"":s;} private static String norm(String s){return Normalizer.normalize(safe(s).trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFKC).replaceAll("\\s+"," ");}

    private record Metric<T>(String key,String label,ToIntFunction<T> value){}
    private record Selected(StageMatchView view,Match match,String seasonId){ Selected merge(StageMatchView other){ var c=new StageMatchView.CountingRules(view.countsFor().standings()||other.countsFor().standings(),view.countsFor().teamStats()||other.countsFor().teamStats(),view.countsFor().playerStats()||other.countsFor().playerStats(),view.countsFor().bracket()||other.countsFor().bracket()); return new Selected(new StageMatchView(view.stageId(),view.stageSourceId(),view.game(),view.platform(),view.sourceMatchId(),view.sourceMatchKey(),view.sourceCompetitionId(),view.startedAt(),view.finishedAt(),view.status(),view.round(),view.teams(),view.coaches(),view.sourceScore(),view.officialScore(),view.adminResult(),view.conceded(),view.overtime(),view.quality(),view.capabilities(),c,view.interpretation()),match,seasonId);}}
    private static class Dataset { final List<Selected> all; final int playerMatchCount; Dataset(List<Selected>a){all=a;playerMatchCount=(int)a.stream().filter(s->s.view.countsFor().playerStats()&&s.match.getTeams()!=null&&Arrays.stream(s.match.getTeams()).filter(Objects::nonNull).anyMatch(t->t.getPlayers()!=null&&t.getPlayers().length>0)).count();} List<String> editions(){return all.stream().map(s->s.view.game()==null?"UNKNOWN":s.view.game().name()).distinct().sorted().toList();} Dataset filterEdition(String e){return new Dataset(all.stream().filter(s->s.view.game()!=null&&s.view.game().name().equalsIgnoreCase(e)).toList());} Dataset filterCoaches(Set<String> ids){return new Dataset(all.stream().filter(s->s.match.getTeams()!=null&&java.util.stream.IntStream.range(0,s.match.getTeams().length).anyMatch(i->{Team t=s.match.getTeams()[i];String id=t!=null&&t.getCoachId()!=null?t.getCoachId().getValue():s.match.getCoaches()!=null&&i<s.match.getCoaches().length&&s.match.getCoaches()[i]!=null?s.match.getCoaches()[i].getId():null;return ids.contains(id);})).toList());}}
    private static class PlayerAggregate { final String id;String name,teamId,teamName,coachId,coachName,race,position;Integer raceId,opus,playerValue;Set<String>skills=new LinkedHashSet<>();Date latest;int games,spp,touchdowns,casualties,kills,interceptions,mvp,passes,catches,running,passing,blocks,knockouts,injuries,pushouts,fouls,armourBreaks;PlayerAggregate(String id){this.id=id;}void add(Player p,Team t,String cid,String cn,String edition,Date d){games++;Player.Stats s=p.getStats();spp+=s==null?val(p.getXpGain()):val(s.getSpp_gained()!=null?s.getSpp_gained():p.getXpGain());mvp+=Boolean.TRUE.equals(p.getMvp())?1:0;if(s!=null){touchdowns+=val(s.getTouchdowns_scored()!=null?s.getTouchdowns_scored():s.getInflictedtouchdowns());casualties+=val(s.getCasualties_inflicted()!=null?s.getCasualties_inflicted():s.getInflictedcasualties());kills+=val(s.getKills_inflicted()!=null?s.getKills_inflicted():s.getInflicteddead());interceptions+=val(s.getInflictedinterceptions());passes+=val(s.getInflictedpasses());catches+=val(s.getInflictedcatches());running+=val(s.getYards_running()!=null?s.getYards_running():s.getInflictedmetersrunning());passing+=val(s.getInflictedmeterspassing());blocks+=val(s.getBlocks_succeeded()!=null?s.getBlocks_succeeded():s.getInflictedtackles());knockouts+=val(s.getKo_inflicted()!=null?s.getKo_inflicted():s.getInflictedko());injuries+=val(s.getInjuries_inflicted()!=null?s.getInjuries_inflicted():s.getInflictedinjuries());pushouts+=val(s.getInflictedpushouts());fouls+=val(s.getFoul_done());armourBreaks+=val(s.getArmour_breaks());}if(latest==null||d==null||!d.before(latest)){latest=d;name=p.getName();teamId=t.getId()==null?null:t.getId().asMongoKey();teamName=t.getName();coachId=cid;coachName=cn;race=t.getRace();raceId=t.getRaceId();opus=t.getId()==null?null:t.getId().getOpus();position=p.getType();playerValue=p.getValue();skills.clear();if(p.getSkillStrings()!=null)skills.addAll(Arrays.asList(p.getSkillStrings()));if(p.getSkills()!=null){if(p.getSkills().getInnateSkills()!=null)skills.addAll(Arrays.asList(p.getSkills().getInnateSkills()));if(p.getSkills().getAcquiredSkills()!=null)skills.addAll(Arrays.asList(p.getSkills().getAcquiredSkills()));}skills.remove(null);}}StatisticsResponse.PlayerEntry entry(int v){return new StatisticsResponse.PlayerEntry(id,name,teamId,teamName,coachId,coachName,race,raceId,opus,position,playerValue,games,spp,List.copyOf(skills),v);}}
    private static class TeamAggregate {final String id;String name,coachId,coachName,race;Integer raceId;Set<String>editions=new TreeSet<>(),seasons=new HashSet<>();int games,wins,draws,losses,points,tdFor,tdAgainst,casFor,casAgainst;TeamAggregate(String id){this.id=id;}void add(Team t,String cid,String cn,String e,String season,int own,int opp,int cf,int ca){name=t.getName();coachId=cid;coachName=cn;race=t.getRace();raceId=t.getRaceId();editions.add(e);seasons.add(season);games++;tdFor+=own;tdAgainst+=opp;casFor+=cf;casAgainst+=ca;if(own>opp){wins++;points+=3;}else if(own==opp){draws++;points++;}else losses++;}StatisticsResponse.TeamEntry entry(int v){return new StatisticsResponse.TeamEntry(id,name,coachId,coachName,race,raceId,List.copyOf(editions),seasons.size(),games,wins,draws,losses,points,tdFor,tdAgainst,casFor,casAgainst,games==0?0:Math.round(1000.0*wins/games)/10.0,v);}}
    private static class VersusAggregate {final String id,name;int games,wins,draws,losses,tf,ta,cf,ca;VersusAggregate(String i,String n){id=i;name=n;}void add(int a,int b,int c,int d){games++;tf+=a;ta+=b;cf+=c;ca+=d;if(a>b)wins++;else if(a==b)draws++;else losses++;}StatisticsResponse.CoachVersus entry(){return new StatisticsResponse.CoachVersus(id,name,games,wins,draws,losses,tf,ta,cf,ca);}}
}
