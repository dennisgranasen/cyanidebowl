import axios from 'axios';
import logger from "./util/Logger";
import config from "./config";

axios.defaults.baseURL = config.backendUrl;

const handleError = (reason) => {
    logger.error("FAILED!!!", reason);
    return [];
}

const returnData = (result) => {
    logger.debug("Result: %o",result.data);
    return result.data && result.data !== null ? result.data : [];
}

export default {
    status: async () => axios(`/status`).then(returnData).catch(handleError),
    league: async () => axios(`/league`).then(returnData).catch(handleError),
    leagueCompetitions: async (leagueUuid) => axios(`/competitions/league/${leagueUuid}/InProgress`).then(returnData).catch(handleError),
    competition: async (competitionUuid) => axios(`/competition/${competitionUuid}`).then(returnData).catch(handleError),
    leagueTeams: async (leagueUuid) => axios(`/teams/league/${leagueUuid}`).then(returnData).catch(handleError),
    competitionTeams: async (competitionUuid) => axios(`/teams/competition/${competitionUuid}`).then(returnData).catch(handleError),
    team: async (teamUuid) => axios(`/team/${teamUuid}`).then(returnData).catch(handleError),
    teamMatches: async (teamUuid) => axios(`/matches/team/${teamUuid}`).then(returnData).catch(handleError),
    competitionMatches: async (competitionUuid) => axios(`/matches/competition/${competitionUuid}`).then(returnData).catch(handleError),
    competitionRanks: async (competitionUuid) => axios(`/ranks/competition/${competitionUuid}`).then(returnData).catch(handleError),
    competitionContests: async (competitionUuid) => axios(`/contests/competition/${competitionUuid}`).then(returnData).catch(handleError),
    lookup: async (lookupRequest) => axios.post("/lookup", lookupRequest).then(returnData).catch(handleError),
};
