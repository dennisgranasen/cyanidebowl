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
      withCredentials: true,
      headers: {
        Authorization: 'Bearer dev-token',
      },
    }; 
  }
  const token = await getToken(getAccessTokenSilently, getAccessTokenWithPopup);
  return {
    // Required for the opaque HttpOnly BB3 session cookie when frontend/backend use different ports.
    withCredentials: true,
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
    publicLeagueSystems: async () =>
      axios('/league-systems')
        .then(returnData)
        .catch(handleError),
      leagueSystemOverview: async (leagueSystemId, seasonId) =>
        axios(`/league-systems/${encodeURIComponent(leagueSystemId)}/overview${seasonId ? `?seasonId=${encodeURIComponent(seasonId)}` : ''}`)
          .then(returnData)
          .catch(handleError),
      seasonStatistics: async (leagueSystemId, seasonId) =>
        axios(`/league-systems/${encodeURIComponent(leagueSystemId)}/seasons/${encodeURIComponent(seasonId)}/statistics`)
          .then(returnData).catch(handleError),
      marathonStatistics: async (leagueSystemId, options = {}) => {
        const query = new URLSearchParams({ edition: options.edition || 'ALL',
          mergeTeamsByName: String(Boolean(options.mergeTeamsByName)), page: String(options.page || 0),
          size: String(options.size || 25), sort: options.sort || 'points' });
        return axios(`/league-systems/${encodeURIComponent(leagueSystemId)}/statistics/marathon?${query}`)
          .then(returnData).catch(handleError);
      },
      personalStatistics: async (leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup) =>
        getDataWithAuthentication(`/user/statistics?leagueSystemId=${encodeURIComponent(leagueSystemId)}`,
          getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),

  leagueSystems: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication('/admin/league-systems', getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  replaySweeperStatus: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication('/admin/replay-sweeper', getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  replaySweeperLogs: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication('/admin/replay-sweeper/logs', getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  replaySweeperReplays: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication('/admin/replay-sweeper/replays', getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  analyzeReplay: async (matchId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/replay-sweeper/replays/${encodeURIComponent(matchId)}/analyze`, {}, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  importReplays: async (files, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const data = new FormData(); files.forEach(file => data.append('files', file));
    return postDataWithAuthentication('/admin/replay-sweeper/replays/import', data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError);
  },
  inspectReplay: async (matchId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/replay-sweeper/replays/${encodeURIComponent(matchId)}/inspect`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  updateReplaySweeper: async (data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    putDataWithAuthentication('/admin/replay-sweeper', data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  runReplaySweeper: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication('/admin/replay-sweeper/run', {}, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  authenticateReplaySweeper: async (data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication('/admin/replay-sweeper/auth', data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  replaySweeperGuardCode: async (id, code, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/replay-sweeper/challenges/${encodeURIComponent(id)}/code`, { code }, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  confirmReplaySweeperGuard: async (id, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/replay-sweeper/challenges/${encodeURIComponent(id)}/confirm`, {}, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  leagueSystemDiscoveryCandidates: async (leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/league-systems/${encodeURIComponent(leagueSystemId)}/discovery-candidates`, getAccessTokenSilently, getAccessTokenWithPopup)
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
  phases: async (seasonId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/seasons/${encodeURIComponent(seasonId)}/phases`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  createPhase: async (seasonId, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/seasons/${encodeURIComponent(seasonId)}/phases`, data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  updatePhase: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    putDataWithAuthentication(`/admin/phases/${encodeURIComponent(id)}`, data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  deletePhase: async (id, getAccessTokenSilently, getAccessTokenWithPopup) =>
    deleteDataWithAuthentication(`/admin/phases/${encodeURIComponent(id)}`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  phaseStages: async (phaseId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/phases/${encodeURIComponent(phaseId)}/stages`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  createPhaseStage: async (phaseId, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/phases/${encodeURIComponent(phaseId)}/stages`, data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  registeredSources: async (seasonId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/seasons/${encodeURIComponent(seasonId)}/registered-sources`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  registeredSourceInspections: async (seasonId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/seasons/${encodeURIComponent(seasonId)}/registered-source-inspections`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  inspectRegisteredSource: async (sourceId, limit, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/registered-sources/${encodeURIComponent(sourceId)}/matches?limit=${limit || 10}`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  inspectCyanideCompetition: async (competitionId, limit, getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication(`/admin/cyanide-competitions/${encodeURIComponent(competitionId)}/inspection?limit=${limit || 5}`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  registerSource: async (seasonId, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/seasons/${encodeURIComponent(seasonId)}/registered-sources`, data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  createMatchSelection: async (stageId, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/admin/stages/${encodeURIComponent(stageId)}/match-selections`, data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  updateMatchSelection: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    putDataWithAuthentication(`/admin/match-selections/${encodeURIComponent(id)}`, data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
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
  stageMatches: async (stageId) =>
    axios(`/stages/${encodeURIComponent(stageId)}/matches`)
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
    axios(`/matches/${encodeURIComponent(matchId.key || matchId)}`)
      .then(returnData)
      .catch(handleError),
  replay: async (matchId) =>
    axios(`/matches/${encodeURIComponent(matchId.key || matchId)}/replay`)
      .then(returnData)
      .catch(handleError),
  downloadOriginalReplay: async (matchId) => {
    const key = matchId.key || matchId;
    const result = await axios(`/matches/${encodeURIComponent(key)}/replay/original`, { responseType: 'blob' });
    const disposition = result.headers?.['content-disposition'] || '';
    const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    const plainName = disposition.match(/filename="?([^";]+)"?/i)?.[1];
    const filename = encodedName ? decodeURIComponent(encodedName) : (plainName || `${key}.bbr`);
    const url = window.URL.createObjectURL(result.data);
    const link = document.createElement('a');
    link.href = url; link.download = filename; document.body.appendChild(link); link.click(); link.remove();
    window.URL.revokeObjectURL(url);
  },
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
  steamConnection: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    getDataWithAuthentication('/user/steam', getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  startSteamAuthentication: async (data, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication('/user/steam/auth', data, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  submitSteamGuardCode: async (challengeId, code, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/user/steam/challenges/${encodeURIComponent(challengeId)}/code`, { code }, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  confirmSteamGuard: async (challengeId, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/user/steam/challenges/${encodeURIComponent(challengeId)}/confirm`, {}, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  disconnectSteam: async (getAccessTokenSilently, getAccessTokenWithPopup) =>
    deleteDataWithAuthentication('/user/steam', getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
  myBb3Teams: async (getAccessTokenSilently, getAccessTokenWithPopup, size = 50, start = 0) =>
    getDataWithAuthentication(`/user/steam/teams?size=${size}&start=${start}`, getAccessTokenSilently, getAccessTokenWithPopup).then(returnData).catch(handleError),
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
