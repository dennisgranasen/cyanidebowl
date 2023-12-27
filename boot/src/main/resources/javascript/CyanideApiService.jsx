import axios from 'axios';
import logger from "./util/Logger";
import config from "./config";

axios.defaults.baseURL = config.backendUrl;

const handleError = (reason) => {
    logger.error("FAILED!!!", reason);
}

const returnData = (result) => {
    logger.debug("Result: %o",result.data);
    return result.data
}

export default {
    status: async () => axios(`/status`).then(returnData).catch(handleError),
    league: async () => axios(`/league`).then(returnData).catch(handleError),
    teams: async (leagueName) => axios(`/teams/${leagueName}`).then(returnData).catch(handleError),
    team: async (teamUuid) => axios(`/team/${teamUuid}`).then(returnData).catch(handleError),
    matches: async (teamUuid) => axios(`/matches/${teamUuid}`).then(returnData).catch(handleError),
    lookup: async (lookupRequest) => axios.post("/lookup", lookupRequest).then(returnData).catch(handleError),
};
