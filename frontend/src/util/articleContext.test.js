import { articleContext, articleEditorUrl, initialArticleContext, articleIsGlobal } from './articleContext';

test('home context retains the selected season and league', () => {
  const links = articleContext({ pathname: '/', search: '?leagueSystem=old' }, 'league', 's4');
  expect(initialArticleContext(articleEditorUrl(links).split('?')[1])).toEqual([
    { type: 'LEAGUE_SYSTEM', id: 'league' }, { type: 'SEASON', id: 's4' },
  ]);
});
test.each([
  ['/competition/c/team/3_123', 'TEAM', '3_123'],
  ['/player/p', 'PLAYER', 'p'], ['/community/f', 'FAN', 'f'],
  ['/staff/writer', 'STAFF', 'writer'], ['/staff/user/7', 'STAFF', '7'],
])('write from %s preserves the entity', (pathname, type, id) => {
  expect(articleContext({ pathname, search: '' })).toEqual([{ type, id }]);
});
test('fan and staff mentions alone do not restrict news distribution', () => {
  expect(articleIsGlobal([{ type: 'FAN', id: 'fan' }])).toBe(true);
  expect(articleIsGlobal([{ type: 'TEAM', id: 'team' }])).toBe(false);
});
test('malformed or unsupported context cannot break the editor', () => {
  expect(initialArticleContext('?context=oops')).toEqual([]);
  expect(initialArticleContext('?context=%7B%7D')).toEqual([]);
});

test('star players round trip through shared editorial context without changing distribution', () => {
  const links = [{ type: 'STAR_PLAYER', id: "Morg_'n'_Thorg" }];
  expect(initialArticleContext(articleEditorUrl(links).split('?')[1])).toEqual(links);
  expect(articleIsGlobal(links)).toBe(true);
});
