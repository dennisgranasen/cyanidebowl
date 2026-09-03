import { resolveRace, toRace } from './raceUtil';

describe('race resolution', () => {
  test('Vampire uses race id 20 in BB2 and 13 in BB3', () => {
    expect(toRace(20, 2)).toBe('Vampire');
    expect(toRace(13, 3)).toBe('Vampire');
  });

  test('numeric race id overrides a stale textual race', () => {
    expect(resolveRace({ raceId: 13, race: 'Amazon' }, 3)).toBe('Vampire');
  });

  test('textual race remains a fallback for old records without race id', () => {
    expect(resolveRace({ race: 'Vampire' }, 3)).toBe('Vampire');
  });

  test('shared ids are resolved by opus', () => {
    expect(toRace(13, 2)).toBe('Amazon');
    expect(toRace(13, 3)).toBe('Vampire');
    expect(toRace(16, 2)).toBe('Khemri');
    expect(toRace(16, 3)).toBe('Chaos Dwarf');
  });

});
