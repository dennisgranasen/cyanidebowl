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
      { headers: { Authorization: 'Bearer dev-token' } },
    );
  });

    test('uses URL-safe authenticated LeagueSystem CRUD routes', async () => {
      const source = { id: 'source-1', sourceEntityId: '3_league_competition' };

      await WarpScoresApiService.createStageSource('nst:s1/stage', source, jest.fn(), jest.fn());
      await WarpScoresApiService.updateStageSource('source/1', source, jest.fn(), jest.fn());
      await WarpScoresApiService.leagueSystemDiscoveryCandidates('nst/system', jest.fn(), jest.fn());

      expect(axios.post).toHaveBeenCalledWith(
        '/admin/stages/nst%3As1%2Fstage/sources',
        source,
        { headers: { Authorization: 'Bearer dev-token' } },
      );
      expect(axios.put).toHaveBeenCalledWith(
        '/admin/stage-sources/source%2F1',
        source,
        { headers: { Authorization: 'Bearer dev-token' } },
      );
      expect(axios).toHaveBeenCalledWith(
        '/admin/league-systems/nst%2Fsystem/discovery-candidates',
        { headers: { Authorization: 'Bearer dev-token' } },
      );
    });
});
