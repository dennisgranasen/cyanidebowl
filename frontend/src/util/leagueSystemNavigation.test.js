import { leagueSystemMenuTarget } from './leagueSystemNavigation';

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
