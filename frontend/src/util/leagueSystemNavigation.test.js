import { homeSeasonTarget, leagueSystemMenuTarget } from './leagueSystemNavigation';

describe('leagueSystemMenuTarget', () => {
  test('keeps the community view when switching league system', () => {
    expect(leagueSystemMenuTarget({ pathname: '/community', search: '?leagueSystem=old' }, 'new'))
      .toBe('/community?leagueSystem=new');
  });
  test('preserves home-page query state when switching LeagueSystem', () => {
    expect(leagueSystemMenuTarget(
      { pathname: '/', search: '?view=playoffs&leagueSystem=old' },
      'nst'
    )).toBe('/?view=playoffs&leagueSystem=nst');
  });
  test('drops the old season when switching LeagueSystem on the home page', () => {
    expect(leagueSystemMenuTarget(
      { pathname: '/', search: '?season=old-season&leagueSystem=old' },
      'new'
    )).toBe('/?leagueSystem=new');
  });

  test('falls back to selected LeagueSystem landing page from unrelated routes', () => {
    expect(leagueSystemMenuTarget(
      { pathname: '/staff/user/42', search: '?tab=articles' },
      'nst'
    )).toBe('/?leagueSystem=nst');
  });

  test('encodes LeagueSystem ids safely', () => {
    expect(leagueSystemMenuTarget(
      { pathname: '/statistics', search: '' },
      'league/system'
    )).toBe('/?leagueSystem=league%2Fsystem');
  });
});

describe('homeSeasonTarget', () => {
  test('stores selected LeagueSystem and season in the URL', () => {
    expect(homeSeasonTarget('system/1', 'season 2'))
      .toBe('/?leagueSystem=system%2F1&season=season+2');
  });
});
