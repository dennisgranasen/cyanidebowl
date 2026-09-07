import { getRaceLogo, resolveRace, toRace } from './raceUtil';

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

  test('uses local logos for races missing from the Cyanide logo repository', () => {
    expect(getRaceLogo(16, 2)).toBe('/img/raceLogos/khemri.png');
    expect(getRaceLogo(16, 3)).toBe('ChaosDwarf_01');
    expect(getRaceLogo(1001, 3)).toBe('/img/raceLogos/chaosRenegades.jpg');
    expect(getRaceLogo(1002, 3)).toBe('/img/raceLogos/owa.png');
  });

  test('uses local logos for textual race fallbacks too', () => {
    expect(getRaceLogo('Tomb Kings', 2)).toBe('/img/raceLogos/khemri.png');
    expect(getRaceLogo('Chaos Renegades', 3)).toBe('/img/raceLogos/chaosRenegades.jpg');
    expect(getRaceLogo('Old World Alliance', 3)).toBe('/img/raceLogos/owa.png');
  });

});
