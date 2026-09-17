export const articleTypes = ['LEAGUE_SYSTEM', 'SEASON', 'TEAM', 'PLAYER', 'FAN', 'STAFF', 'STAR_PLAYER'];
export const articleIsGlobal = (links) => !links.some(({ type }) => ['LEAGUE_SYSTEM', 'SEASON', 'TEAM', 'PLAYER'].includes(type));
export function articleContext(location, system, season) {
  const query = new URLSearchParams(location.search);
  const links = [];
  const add = (type, id) => { if (id) links.push({ type, id }); };
  add('LEAGUE_SYSTEM', system || query.get('leagueSystem'));
  add('SEASON', season || query.get('season'));
  const team = location.pathname.match(/\/team\/([^/]+)/);
  const player = location.pathname.match(/\/player\/([^/]+)/);
  const fan = location.pathname.match(/^\/community\/([^/]+)\/?$/);
  const staff = location.pathname.match(/^\/staff\/(?:user\/)?([^/]+)/);
  add('TEAM', team && decodeURIComponent(team[1]));
  add('PLAYER', (player && decodeURIComponent(player[1])) || query.get('player'));
  add('FAN', fan && decodeURIComponent(fan[1]));
  add('STAFF', staff && decodeURIComponent(staff[1]));
  return links;
}
export const articleEditorUrl = (links) => `/editor/articles/new?${new URLSearchParams({ context: JSON.stringify(links) })}`;
export function initialArticleContext(search) {
  try {
    const links = JSON.parse(new URLSearchParams(search).get('context') || '[]');
    return Array.isArray(links) ? links.filter(l => l && articleTypes.includes(l.type) && typeof l.id === 'string' && l.id) : [];
  } catch { return []; }
}
