import { expect, test } from '@jest/globals';
import abbreviators from './Abbreviators';

test('abbreviate coach name works for "null" or "undefined".', () => {
  expect(abbreviators.abbreviateCoachName(null)).toBeNull();
  expect(abbreviators.abbreviateCoachName(undefined)).toBeUndefined();
});

test('abbreviate coach name defaults to 3 letters with dot.', () => {
  expect(abbreviators.abbreviateCoachName('hello')).toBe('hel.');
});

test('abbreviate coach name can use letter count', () => {
  expect(abbreviators.abbreviateCoachName('hello', 2)).toBe('he.');
});

test('abbreviate coach name will return unmodified coach name if shorter or samen length than letter count', () => {
  expect(abbreviators.abbreviateCoachName('hi', 3)).toBe('hi');
  expect(abbreviators.abbreviateCoachName('hiho', 3)).toBe('hiho');
});

test('abbreviate team name will return an abbreviation', () => {
  expect(abbreviators.abbreviateTeamName('Dark Gogetters')).toBe('DG');
  expect(abbreviators.abbreviateTeamName('DarkGogetters')).toBe('DG');
});

test('abbreviate team name will ignore special characters', () => {
  expect(abbreviators.abbreviateTeamName('[DBBC] DarkGogetters')).toBe('DDG');
  expect(abbreviators.abbreviateTeamName('(WCQ) DarkGogetters')).toBe('WDG');
});

test('abbreviate works', () => {
  expect(abbreviators.abbreviate('Round Robin')).toBe('RR');
  expect(abbreviators.abbreviate(null)).toBeNull();
  expect(abbreviators.abbreviate(undefined)).toBeUndefined();
});
