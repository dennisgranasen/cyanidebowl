import { uniquePlayers } from './MatchPlayerRatings';

describe('uniquePlayers', () => {
  it('collapses legacy and canonical IDs for the same player', () => {
    const players = [
      { playerId: '3_player-1', playerName: 'Player' },
      { playerId: 'player-1', playerName: 'Player duplicate' },
      { playerId: '3_player-2', playerName: 'Another player' },
    ];

    expect(uniquePlayers(players)).toEqual([players[0], players[2]]);
  });

  it('keeps separate rows when a player ID is missing', () => {
    const players = [{ playerName: 'Unknown 1' }, { playerName: 'Unknown 2' }];

    expect(uniquePlayers(players)).toEqual(players);
  });
});