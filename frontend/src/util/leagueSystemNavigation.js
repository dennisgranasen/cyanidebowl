export function leagueSystemMenuTarget(location, leagueSystemId) {
  const encoded = encodeURIComponent(leagueSystemId);
  if (location?.pathname === '/community') return `/community?leagueSystem=${encoded}`;

  if (location?.pathname !== '/') {
    return `/?leagueSystem=${encoded}`;
  }

  const params = new URLSearchParams(location.search || '');
  params.set('leagueSystem', leagueSystemId);
  return `/?${params.toString()}`;
}
