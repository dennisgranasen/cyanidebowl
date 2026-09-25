export function leagueSystemMenuTarget(location, leagueSystemId) {
  const encoded = encodeURIComponent(leagueSystemId);
  if (location?.pathname === '/community') return `/community?leagueSystem=${encoded}`;

  if (location?.pathname !== '/') {
    return `/?leagueSystem=${encoded}`;
  }

  const params = new URLSearchParams(location.search || '');
  if (params.get('leagueSystem') !== leagueSystemId) params.delete('season');
  params.set('leagueSystem', leagueSystemId);
  return `/?${params.toString()}`;
}

export function homeSeasonTarget(leagueSystemId, seasonId) {
  const params = new URLSearchParams();
  if (leagueSystemId) params.set('leagueSystem', leagueSystemId);
  if (seasonId) params.set('season', seasonId);
  const query = params.toString();
  return query ? `/?${query}` : '/';
}
