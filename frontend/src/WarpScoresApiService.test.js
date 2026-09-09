jest.mock('axios', () => {
  const mockAxios = jest.fn();
  mockAxios.defaults = {};
  mockAxios.post = jest.fn();
    mockAxios.put = jest.fn();
  mockAxios.delete = jest.fn();
  return mockAxios;
});

import axios from 'axios';
import WarpScoresApiService from './WarpScoresApiService';

describe('WarpScoresApiService', () => {
  test.each(['leagueSystems', 'adminUsers'])('%s returns lists and rejects malformed responses', async (method) => {
    const systems = [{ id: 'nst', name: 'NST' }];
    axios.mockResolvedValueOnce({ data: systems });
    await expect(WarpScoresApiService[method](jest.fn(), jest.fn())).resolves.toEqual(systems);
    axios.mockResolvedValueOnce({ data: [] });
    await expect(WarpScoresApiService[method](jest.fn(), jest.fn())).resolves.toEqual([]);
    for (const data of ['', null, {}, '<html></html>']) {
      axios.mockResolvedValueOnce({ data });
      await expect(WarpScoresApiService[method](jest.fn(), jest.fn())).rejects.toThrow('expected a list');
    }
  });

  beforeEach(() => {
    axios.mockReset();
    axios.post.mockReset();
      axios.put.mockReset();
    axios.delete.mockReset();
    axios.mockResolvedValue({ data: [] });
    axios.post.mockResolvedValue({ data: {} });
      axios.put.mockResolvedValue({ data: {} });
  });

  test('uses canonical competition URLs and authenticated lookup requests', async () => {
    await WarpScoresApiService.competitionMatches({ key: 'competition-1' }, 10);
    await WarpScoresApiService.competitionTeams({ key: 'competition-1' });
    await WarpScoresApiService.lookup({ league_name: 'League' }, jest.fn(), jest.fn());

    expect(axios).toHaveBeenCalledWith('/matches/competition/competition-1?limit=10');
    expect(axios).toHaveBeenCalledWith('/teams/competition/competition-1');
    expect(axios.post).toHaveBeenCalledWith(
      '/lookup',
      { league_name: 'League' },
      { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
    );
  });

  test('URL-encodes a canonical match identity', async () => {
    await WarpScoresApiService.match({ key: '3_match/with space' });

    expect(axios).toHaveBeenCalledWith('/matches/3_match%2Fwith%20space');
  });

    test('uses URL-safe authenticated LeagueSystem CRUD routes', async () => {
      const source = { id: 'source-1', sourceEntityId: '3_league_competition' };

      await WarpScoresApiService.createStageSource('nst:s1/stage', source, jest.fn(), jest.fn());
      await WarpScoresApiService.updateStageSource('source/1', source, jest.fn(), jest.fn());
      await WarpScoresApiService.leagueSystemDiscoveryCandidates('nst/system', jest.fn(), jest.fn());
      await WarpScoresApiService.stageMatches('nst:s1/stage');
      await WarpScoresApiService.registerSource('season/31', source, jest.fn(), jest.fn());
      await WarpScoresApiService.createMatchSelection('stage/east', { registeredSourceId: 'source-1' }, jest.fn(), jest.fn());

      expect(axios.post).toHaveBeenCalledWith(
        '/admin/stages/nst%3As1%2Fstage/sources',
        source,
        { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
      );
      expect(axios.put).toHaveBeenCalledWith(
        '/admin/stage-sources/source%2F1',
        source,
        { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
      );
      expect(axios).toHaveBeenCalledWith(
        '/admin/league-systems/nst%2Fsystem/discovery-candidates',
        { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
      );
      expect(axios).toHaveBeenCalledWith('/stages/nst%3As1%2Fstage/matches');
      expect(axios.post).toHaveBeenCalledWith(
        '/admin/seasons/season%2F31/registered-sources', source,
        { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
      );
      expect(axios.post).toHaveBeenCalledWith(
        '/admin/stages/stage%2Feast/match-selections', { registeredSourceId: 'source-1' },
        { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
      );
    });

    test('uses authenticated site-admin user permission routes', async () => {
      await WarpScoresApiService.adminUsers(jest.fn(), jest.fn());
      await WarpScoresApiService.updateAdminUserPermissions(
        42,
        {
          siteAdmin: true,
          leagueAdmin: false,
          registerLeague: true,
          adminForLeagueSystems: ['nst'],
        },
        jest.fn(),
        jest.fn(),
      );

      expect(axios).toHaveBeenCalledWith(
        '/admin/users',
        { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
      );
      expect(axios.put).toHaveBeenCalledWith(
        '/admin/users/42/permissions',
        { siteAdmin: true, leagueAdmin: false, registerLeague: true, adminForLeagueSystems: ['nst'] },
        { withCredentials: true, headers: { Authorization: 'Bearer dev-token' } },
      );
    });
});
