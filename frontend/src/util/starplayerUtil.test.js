import { getStarPlayerDisplayName, isStarPlayer } from './starplayerUtil';

describe('star-player display names', () => {
  test.each(['sp_dribblesnot', 'name_sp_dribblesnot', 'PLAYER_NAMES_CHAMPION_DRIBBLESNOT'])(
    'resolves %s to the canonical display name',
    (playerName) => {
      expect(getStarPlayerDisplayName(playerName)).toBe('Bomber Dribblesnot');
      expect(isStarPlayer(playerName)).toBe(true);
    },
  );
});