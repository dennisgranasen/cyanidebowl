import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { identityUtils } from '../util/identityUtil';

const MyTeamsContext = createContext({ teams: [], coachIds: [], loading: false,
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
  const [coachIds, setCoachIds] = useState([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    if (!authenticationReady || !isAuthenticated) { setTeams([]); setCoachIds([]); return; }
    setLoading(true);
    try {
      const connection = await WarpScoresApiService.steamConnection(getAccessTokenSilently, getAccessTokenWithPopup);
      setCoachIds(connection.coachIds || []);
      if (!connection.connected) { setTeams([]); return; }
      const response = await WarpScoresApiService.myBb3Teams(getAccessTokenSilently, getAccessTokenWithPopup);
      setTeams(response.items || []);
    } catch (_error) {
      // Being signed into BlaskScore without an active Steam session is normal.
      setTeams([]);
    } finally { setLoading(false); }
  }, [authenticationReady, isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup]);

  useEffect(() => { refresh(); }, [refresh]);
  const ids = useMemo(() => new Set(teams.map((team) => canonicalId(team.id)).filter(Boolean)), [teams]);
  const coaches = useMemo(() => new Set(coachIds.map(canonicalId).filter(Boolean)), [coachIds]);
  const value = useMemo(() => ({ teams, coachIds, loading, refresh,
    isMyTeam: (id) => ids.has(canonicalId(id)),
    isMyCoach: (id) => coaches.has(canonicalId(id)),
  }), [teams, coachIds, loading, refresh, ids, coaches]);
  return <MyTeamsContext.Provider value={value}>{children}</MyTeamsContext.Provider>;
}

export const useMyTeams = () => useContext(MyTeamsContext);
