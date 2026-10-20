import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { identityUtils } from '../util/identityUtil';

const MyTeamsContext = createContext({ teams: [], claims: [], coachIds: [], loading: false,
  teamsError: null, isMyTeam: () => false, isMyCoach: () => false, refresh: () => {} });

const canonicalId = (id) => {
  if (!id) return null;
  const value = typeof id === 'object' ? (id.value ?? id.key) : identityUtils.value(id);
  if (!value || typeof value === 'object') return null;
  return String(value).toLowerCase();
};

export const coachClaimKey = (game, coachId) => `${game}:${canonicalId(coachId)}`;
export const isCoachClaimed = (claims, coachId, opus) => {
  if (!coachId || !opus) return false;
  const wanted = coachClaimKey(`BB${opus}`, coachId);
  return (claims || []).some(claim => coachClaimKey(claim.game, claim.coachId) === wanted);
};

export function MyTeamsProvider({ children }) {
  const { authenticationReady, isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const [teams, setTeams] = useState([]);
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(false);
  const [teamsError, setTeamsError] = useState(null);

  const refresh = useCallback(async () => {
    if (!authenticationReady || !isAuthenticated) {
      setTeams([]);
      setClaims([]);
      setTeamsError(null);
      return;
    }
    setLoading(true);
    setTeamsError(null);
    try {
      setClaims(await WarpScoresApiService.coachClaims(getAccessTokenSilently, getAccessTokenWithPopup));
      const connection = await WarpScoresApiService.steamConnection(getAccessTokenSilently, getAccessTokenWithPopup);
      if (!connection.connected) { setTeams([]); return; }
      const ownedTeams = [];
      let start = 0;
      let response;
      do {
        response = await WarpScoresApiService.myBb3Teams(getAccessTokenSilently, getAccessTokenWithPopup, 100, start);
        ownedTeams.push(...(response.items || []));
        start += response.size || 100;
      } while (response.hasMore);
      setTeams(ownedTeams);
      setClaims(await WarpScoresApiService.coachClaims(getAccessTokenSilently, getAccessTokenWithPopup));
    } catch (error) {
      setTeams([]);
      setTeamsError(error?.response?.data?.message || error?.message || 'Unknown error');
    } finally { setLoading(false); }
  }, [authenticationReady, isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup]);

  useEffect(() => { refresh(); }, [refresh]);
  const coachIds = useMemo(() => claims.map(claim => claim.coachId), [claims]);
  const ids = useMemo(() => new Set(teams.map((team) => canonicalId(team.id)).filter(Boolean)), [teams]);
  const value = useMemo(() => ({ teams, claims, coachIds, loading, teamsError, refresh,
    isMyTeam: (id) => ids.has(canonicalId(id)),
    isMyCoach: (id, opus) => isCoachClaimed(claims, id, opus),
  }), [teams, claims, coachIds, loading, teamsError, refresh, ids]);
  return <MyTeamsContext.Provider value={value}>{children}</MyTeamsContext.Provider>;
}

export const useMyTeams = () => useContext(MyTeamsContext);
