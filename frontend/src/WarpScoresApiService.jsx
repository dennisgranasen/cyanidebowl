import axios from 'axios';
import logger from './util/logger';
import config from './config';

const { isProduction } = config;

axios.defaults.baseURL = config.backendUrl;

const authorizationParams = {
  authorizationParams: {
    audience: config.auth0Audience,
  },
};

const handleError = (reason) => {
  logger.error('Backend call failed.', reason);
  throw reason;
};

const returnData = async (result) => {
  logger.debug('Backend call succeeded. Result: [%o].', result?.data);
  return result?.data !== null ? result.data : [];
};

const offerDownloadData = (result, filename, contentType) => {
  if (!result) return;
  const link = document.createElement('a');
  const blob = new Blob([result.data], { type: contentType });
  link.href = window.URL.createObjectURL(blob);
  link.download = filename;
  link.click();
};

const getToken = async (getAccessTokenSilently, getAccessTokenWithPopup) => {
  let token;
  try {
    token = await getAccessTokenSilently(authorizationParams);
  } catch (e) {
    token = await getAccessTokenWithPopup(authorizationParams);
  }
  return token;
};

const getAuthHeaders = async (getAccessTokenSilently, getAccessTokenWithPopup, requestToken = true) => {
  if (!requestToken) {
    return undefined;
  }
  if (!isProduction) {
    return {
      headers: {
        Authorization: 'Bearer dev-token',
      },
    }; 
  }
  const token = await getToken(getAccessTokenSilently, getAccessTokenWithPopup);
  return {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  };
};

const postDataWithAuthentication = async (endpoint, data, getAccessTokenSilently, getAccessTokenWithPopup, requestToken = true) => {
  const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup, requestToken);
  return axios.post(endpoint, data, authHeaders);
};

const getDataWithAuthentication = async (endpoint, getAccessTokenSilently, getAccessTokenWithPopup, requestToken = true) => {
  const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup, requestToken);
  return axios(endpoint, authHeaders);
};

const putDataWithAuthentication = async (endpoint, data, getAccessTokenSilently, getAccessTokenWithPopup, requestToken = true) => {
  const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup, requestToken);
  return axios.put(endpoint, data, authHeaders);
};

const deleteDataWithAuthentication = async (endpoint, getAccessTokenSilently, getAccessTokenWithPopup, requestToken = true) => {
  const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup, requestToken);
  return axios.delete(endpoint, authHeaders);
};

export default {
  // misc
  backendVersion: async () => axios(`/version.json`).then(returnData).catch(handleError),


  lookup: async(lookupFields, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup);
    return axios.post('/lookup', lookupFields, authHeaders)
        .then(returnData)
        .catch(handleError)
  },
  status: async () => axios(`/status`).then(returnData).catch(handleError),
  // circuits
  newCircuit: async (name, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/circuits`, { circuitName: name }, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  circuits: async (circuitId) =>
    axios(`/circuits${circuitId ? `/${circuitId}` : ''}`)
      .then(returnData)
      .catch(handleError),

  leagueSystems: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication('/admin/league-systems', getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  createLeagueSystem: async (data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication('/admin/league-systems', data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  updateLeagueSystem: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    putDataWithAuthentication(`/admin/league-systems/${encodeURIComponent(id)}`, data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  deleteLeagueSystem: async (id, getAccessTokenSilently, getAccessTokenWithPopup) =>
    deleteDataWithAuthentication(`/admin/league-systems/${encodeURIComponent(id)}`, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  seasons: async (leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/league-systems/${encodeURIComponent(leagueSystemId)}/seasons`, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  createSeason: async (leagueSystemId, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/league-systems/${encodeURIComponent(leagueSystemId)}/seasons`, data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  updateSeason: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    putDataWithAuthentication(`/admin/seasons/${encodeURIComponent(id)}`, data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  deleteSeason: async (id, getAccessTokenSilently, getAccessTokenWithPopup) =>
    deleteDataWithAuthentication(`/admin/seasons/${encodeURIComponent(id)}`, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  stages: async (seasonId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/seasons/${encodeURIComponent(seasonId)}/stages`, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  createStage: async (seasonId, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/seasons/${encodeURIComponent(seasonId)}/stages`, data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  updateStage: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    putDataWithAuthentication(`/admin/stages/${encodeURIComponent(id)}`, data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  deleteStage: async (id, getAccessTokenSilently, getAccessTokenWithPopup) =>
    deleteDataWithAuthentication(`/admin/stages/${encodeURIComponent(id)}`, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  stageSources: async (stageId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/stages/${encodeURIComponent(stageId)}/sources`, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  createStageSource: async (stageId, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/stages/${encodeURIComponent(stageId)}/sources`, data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  updateStageSource: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    putDataWithAuthentication(`/admin/stage-sources/${encodeURIComponent(id)}`, data, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  deleteStageSource: async (id, getAccessTokenSilently, getAccessTokenWithPopup) =>
    deleteDataWithAuthentication(`/admin/stage-sources/${encodeURIComponent(id)}`, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),

  addEntityToCircuitLeg: async (
    circuitId,
    circuitLegId,
    entityData,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  ) => 
    postDataWithAuthentication(
      `/circuits/${circuitId}/legs/${circuitLegId}/addEntity`,
      entityData,
      getAccessTokenSilently,
      getAccessTokenWithPopup,
    )
        .then(returnData)
        .catch(handleError),
      
  addLegToCircuit: async (
    circuitId,
    label,
    entityData,
    isCollected,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  ) =>
    postDataWithAuthentication(
      `/circuits/${circuitId}/legs`,
      {
        label: label,
        entity: entityData,
        isCollected: isCollected
      },
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(returnData)
      .catch(handleError),
  removeCircuitLeg: async (
    circuitId, 
    circuitLegId,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  ) =>
    deleteDataWithAuthentication(
      `/circuits/${circuitId}/legs/${circuitLegId}`,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(returnData)
      .catch(handleError),
  updateCircuitLeg: async (
    circuitId, 
    circuitLegId, 
    updateFields, 
    getAccessTokenSilently, 
    getAccessTokenWithPopup
  ) =>
    postDataWithAuthentication(
      `/circuits/${circuitId}/legs/${circuitLegId}/update`,
      updateFields,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(returnData)
      .catch(handleError),

  circuitRanks: async (circuitId, limit) =>
    axios(`/ranks/circuit/${circuitId.key || circuitId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  circuitLegRanks: async (circuitId, circuitLegId, limit) =>
    axios(`/ranks/circuit/${circuitId.key || circuitId}/leg/${circuitLegId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  circuitLegEntityRanks: async (circuitId, circuitLegId, entityId, limit) =>
    axios(`/ranks/circuit/${circuitId.key || circuitId}/leg/${circuitLegId}/${entityId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),  
  circuitTeams: async (circuitId, limit) =>
    axios(`/teams/circuit/${circuitId.key || circuitId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  circuitLegTeams: async (circuitId, circuitLegId, limit) =>
    axios(`/teams/circuit/${circuitId.key || circuitId}/leg/${circuitLegId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),  
  circuitLegEntityTeams: async (circuitId, circuitLegId, entityId, limit) =>
    axios(`/teams/circuit/${circuitId.key || circuitId}/leg/${circuitLegId}/${entityId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  
  // leagues
  leagues: async (leagueId) =>
    axios(`/leagues${leagueId ? `/${leagueId.key || leagueId}` : ''}`)
      .then(returnData)
      .catch(handleError),
  competitionCountByStatus: async (leagues) =>
    axios(`/league/competitionCountByStatus?leagueIds=${leagues.map((l) => l.key || l).join(',')}`)
      .then(returnData).catch(handleError),
  leagueRanks: async (leagueId, limit) =>
    axios(`/ranks/league/${leagueId.key || leagueId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  leagueTeams: async (leagueId) =>
    axios(`/teams/league/${leagueId.key || leagueId}`)
      .then(returnData)
      .catch(handleError),
  // contests
  liveLeagueContests: async (leagueId, limit) => 
    axios(`/contests/league/${leagueId.key || leagueId}/live${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  liveCompetitionContests: async (competitionId, limit) =>
    axios(`/contests/competition/${competitionId.key || competitionId}/live${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  competitionContests: async (competitionId, limit) =>
    axios(`/contests/competition/${competitionId.key || competitionId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  latestCompetitionContests: async (competitionId, limit) =>
    axios(`/contests/competition/${competitionId.key || competitionId}/latest${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  latestLeagueContests: async (leagueId, limit) =>
    axios(`/contests/league/${leagueId.key || leagueId}/latest${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),

  arenaTopCoaches: async (competitionId) =>
    axios(`/arena/${competitionId.key || competitionId}/topCoaches`).then(returnData).catch(handleError),
  arenaInfos: async (competitionId, race) =>
    axios(`/arena/${competitionId.key || competitionId}/info${race ? `/${race}` : ''}`)
      .then(returnData)
      .catch(handleError),
  arenaTeams: async (competitionId, race, runType) =>
    axios(`/arena/${competitionId.key || competitionId}/race/${race}/${runType}`).then(returnData).catch(handleError),
  arenaCoachTeams: async (competitionId, coachId) =>
    axios(`/arena/${competitionId.key || competitionId}/coach/${coachId.key || coachId}`).then(returnData).catch(handleError),
  // matches
  latestCompetitionMatches: async (competitionId, limit) =>
    axios(`/matches/competition/${competitionId.key || competitionId}/latest${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  latestLeagueMatches: async (leagueId, limit) =>
    axios(`/matches/league/${leagueId.key || leagueId}/latest${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  leagueMatches: async (leagueId, limit) =>
    axios(`/matches/league/${leagueId.key || leagueId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  competitionMatches: async (competitionId, limit) =>
    axios(`/matches/competition/${competitionId.key || competitionId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),

  circuitMatches: async (circuitId, limit) =>
    axios(`/matches/circuit/${circuitId.key || circuitId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  circuitLegMatches: async (circuitId, circuitLegId, limit) =>
    axios(`/matches/circuit/${circuitId.key || circuitId}/leg/${circuitLegId.key || circuitLegId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  circuitLegEntityMatches: async (circuitId, circuitLegId, entityId, limit) =>
    axios(`/matches/circuit/${circuitId.key || circuitId}/leg/${circuitLegId.key || circuitLegId}/${entityId.key || entityId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),

  match: async (matchId) =>
    axios(`/matches/${matchId.key || matchId}`)
      .then(returnData)
      .catch(handleError),
  // competitions
  leagueCompetitions: async (leagueId, initialized) =>
    axios(
      `/competitions/league/${leagueId.key || leagueId}${initialized ? '/initialized' : ''}`
    )
      .then(returnData)
      .catch(handleError),
  competition: async (competitionId) =>
    axios(`/competition${competitionId ? `/${competitionId.key || competitionId}` : ''}`)
      .then(returnData)
      .catch(handleError),

  competitionStats: async (competitionId) =>
    axios(`/competitions/${competitionId.key || competitionId}/stats`).then(returnData).catch(handleError),
  
  competitionTeam: async (competitionId, teamId) =>
    axios(`/competitions/${competitionId.key || competitionId}/team/${teamId.key || teamId}`).then(returnData).catch(handleError),
  competitionTeams: async (competitionId) =>
    axios(`/teams/competition/${competitionId.key || competitionId}`)
      .then(returnData)
      .catch(handleError),
  competitionRanks: async (competitionId, limit) =>
    axios(`/ranks/competition/${competitionId.key || competitionId}${limit ? `?limit=${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  // team
  team: async (teamId) => {
    try {
      const response = await axios(`/team/${teamId.key || teamId}`);
      return returnData(response);
    } catch (error) {
      if (error.response && error.response.status === 404) {
        // Silently return null for 404s without logging
        return null;
      } else {
        handleError(error);
      }
    }
  },
  teams: async (teamIds) => {
    try {
      teamIds = Array.isArray(teamIds) ? teamIds : [teamIds];
      const teamIdString = teamIds.map((id) => id.key || id).join(',');
      const response = await axios(`/teams/${teamIdString}`);
      return returnData(response);
    } catch (error) {
      if (error.response && error.response.status === 404) {
        // Silently return null for 404s without logging
        return null;
      } else {
        handleError(error);
      }
    }
  },
  teamMatches: async (teamId) => axios(`/teams/${teamId.key || teamId}/matches`).then(returnData).catch(handleError),
  // user
  userPermissions: async (getAccessTokenSilently, getAccessTokenWithPopup, requestToken) =>
    getDataWithAuthentication('/userPermissions', getAccessTokenSilently, getAccessTokenWithPopup, requestToken)
      .then(returnData)
      .catch(handleError),
  exportNafXml: async (competitionId, getAccessTokenSilently, getAccessTokenWithPopup, requestToken) =>
    getDataWithAuthentication(
      `/competitions/${competitionId.key || competitionId}/exportNafData`,
      getAccessTokenSilently,
      getAccessTokenWithPopup,
      requestToken
    )
      .then((result) => offerDownloadData(result, `${competitionId.key || competitionId}-nafReport.xml`, 'application/xml'))
      .catch(handleError),
};
