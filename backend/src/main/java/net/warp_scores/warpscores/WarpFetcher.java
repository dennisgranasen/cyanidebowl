package net.warp_scores.warpscores;

import net.warp_scores.warpscores.service.FetchDataService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(scanBasePackages = "net.warp_scores.warpscores")
public class WarpFetcher {

    public static void main(String[] args) {
        System.out.println("Starting WarpFetcher...");
        SpringApplication app = new SpringApplication(WarpFetcher.class);
        app.setDefaultProperties(java.util.Map.of("scheduler.enabled", "false"));
        ApplicationContext context = app.run(args);
        FetchDataService fetcher = context.getBean(FetchDataService.class);
        System.out.println("Running WarpFetcher...");
        if (args.length > 0)  {
            String arg = args[0];
            if (arg.equals("leagues")) {
                //fetcher.fetchLeagues();
                fetcher.fetchLeagueData(); // 20s -> 10m
            } else if (arg.equals("competitions")) {
                //fetcher.fetchCompetitions();
                fetcher.fetchCompetitionData(); // 5m -> 1h
            } else if (arg.equals("contests")) {
                fetcher.fetchCompetitionContests();
            } else if (arg.equals("matches")) {
                //fetcher.fetchMissingMatches();
                fetcher.fetchNewMatches(); // 3m -> 1h
            } else if (arg.equals("teams")) {
                //fetcher.fetchTeams();
                fetcher.fetchTeamData(); // kl 03 varje dag            
            } else {
                System.out.println("Unknown argument: " + arg);
                System.exit(1);
            }
        } else {

            System.out.println("Fetching leagues ...");
            fetcher.fetchLeagueData(); // 20s -> 10m
            System.out.println("Fetching competitions ...");
            fetcher.fetchCompetitionData(); // 5m -> 1h
            //System.out.println("Fetching contests ...");
            //fetcher.fetchCompetitionContests(); // 3m -> 15 m
            System.out.println("Fetching matches ...");
            fetcher.fetchNewMatches(); // 3m -> 1h
            System.out.println("Fetching teams ...");
            fetcher.fetchTeamData(); // kl 03 varje dag            
        }
        System.out.println("All done for now, bye bye...");
        System.exit(0);
    }
}