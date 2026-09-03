import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { identityUtils } from '../util/identityUtil';

const MyTeamsContext = createContext({ teams: [], loading: false, isMyTeam: () => false, refresh: () => {} });

const canonicalId = (id) => {
  if (!id) return null;
  const value = typeof id === 'object' ? (id.value ?? id.key) : identityUtils.value(id);
  if (!value || typeof value === 'object') return null;
  return String(value).toLowerCase();
};

export function MyTeamsProvider({ children }) {
  const { authenticationReady, isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    if (!authenticationReady || !isAuthenticated) { setTeams([]); return; }
    setLoading(true);
    try {
      const connection = await WarpScoresApiService.steamConnection(getAccessTokenSilently, getAccessTokenWithPopup);
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
  const value = useMemo(() => ({ teams, loading, refresh, isMyTeam: (id) => ids.has(canonicalId(id)) }), [teams, loading, refresh, ids]);
  return <MyTeamsContext.Provider value={value}>{children}</MyTeamsContext.Provider>;
}

export const useMyTeams = () => useContext(MyTeamsContext);
