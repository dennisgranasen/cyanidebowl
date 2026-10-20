import { formationPlayers } from './bb3Formation';

describe('BB3 formation pitch positions', () => {
  const roster = [
    { player_id: 'p1', number: 7, name: 'Runner' },
    { player_id: 'p2', number: 3, name: 'Blitzer' },
  ];

  test('maps roster players to a left-half replay coordinate', () => {
    const positions = formationPlayers({ p1: { x: 4, y: 8 } }, roster);

    expect(positions[0]).toMatchObject({
      name: 'Runner',
      label: '7',
      x: 4,
      y: 8,
      onPitch: true,
    });
  });

  test('mirrors the other replay half into the displayed team half', () => {
    const positions = formationPlayers({ p1: { X: 21, Y: 8 } }, roster);

    expect(positions[0]).toMatchObject({ x: 4, y: 8, onPitch: true });
  });

  test('keeps positions outside the pitch in the sideline list', () => {
    const positions = formationPlayers({ p1: { x: 4, y: 15 } }, roster);

    expect(positions[0].onPitch).toBe(false);
  });
});
