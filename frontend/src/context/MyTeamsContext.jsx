import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { identityUtils } from '../util/identityUtil';

const MyTeamsContext = createContext({ teams: [], claims: [], coachIds: [], loading: false,
  isMyTeam: () => false, isMyCoach: () => false, refresh: () => {} });

const canonicalId = (id) => {
  if (!id) return null;
  const value = typeof id === 'object' ? (id.value ?? id.key) : identityUtils.value(id);
  if (!value || typeof value === 'object') return null;
  return String(value).toLowerCase();
};

export function MyTeamsProvider({ children }) {
  const { authenticationReady, isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const [teams, setTeams] = useState([]);
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    if (!authenticationReady || !isAuthenticated) { setTeams([]); setClaims([]); return; }
    setLoading(true);
    try {
      setClaims(await WarpScoresApiService.coachClaims(getAccessTokenSilently, getAccessTokenWithPopup));
      const connection = await WarpScoresApiService.steamConnection(getAccessTokenSilently, getAccessTokenWithPopup);
      if (!connection.connected) { setTeams([]); return; }
      const response = await WarpScoresApiService.myBb3Teams(getAccessTokenSilently, getAccessTokenWithPopup);
      setTeams(response.items || []);
      setClaims(await WarpScoresApiService.coachClaims(getAccessTokenSilently, getAccessTokenWithPopup));
    } catch (_error) {
      // Claims remain useful even without an active Steam session.
      setTeams([]);
    } finally { setLoading(false); }
  }, [authenticationReady, isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup]);

  useEffect(() => { refresh(); }, [refresh]);
  const coachIds = useMemo(() => claims.map(claim => claim.coachId), [claims]);
  const ids = useMemo(() => new Set(teams.map((team) => canonicalId(team.id)).filter(Boolean)), [teams]);
  const coaches = useMemo(() => new Set(claims.map(claim => `${claim.game}:${canonicalId(claim.coachId)}`).filter(key => !key.endsWith(':null'))), [claims]);
  const value = useMemo(() => ({ teams, claims, coachIds, loading, refresh,
    isMyTeam: (id) => ids.has(canonicalId(id)),
    isMyCoach: (id, opus) => coaches.has(`BB${opus}:${canonicalId(id)}`),
  }), [teams, claims, coachIds, loading, refresh, ids, coaches]);
  return <MyTeamsContext.Provider value={value}>{children}</MyTeamsContext.Provider>;
}

export const useMyTeams = () => useContext(MyTeamsContext);
