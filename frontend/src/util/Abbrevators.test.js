import { expect, test } from '@jest/globals';
import abbrevators from './Abbrevators';

test('abbrevate coach name works for "null".', () => {
  expect(abbrevators.abbrevateCoachName(null)).toBeNull();
});

test('abbrevate coach name works for "undefined".', () => {
  expect(abbrevators.abbrevateCoachName(undefined)).toBeUndefined();
});

test('abbrevate coach name defaults to 3 letters with dot.', () => {
  expect(abbrevators.abbrevateCoachName('hello')).toBe('hel.');
});

test('abbrevate coach name can use letter count', () => {
  expect(abbrevators.abbrevateCoachName('hello', 2)).toBe('he.');
});

test('abbrevate coach name will return unmodified coach name if shorter than letter count', () => {
  expect(abbrevators.abbrevateCoachName('hi', 3)).toBe('hi');
});

test('abbrevate coach name will return unmodified coach name if same length as abbrevation', () => {
  expect(abbrevators.abbrevateCoachName('hiho', 3)).toBe('hiho');
});

test('abbrevate team name will return an abbrevation', () => {
  expect(abbrevators.abbrevateTeamName('Dark Gogetters')).toBe('DG');
});

test('abbrevate team name will return an abbrevation for camel case', () => {
  expect(abbrevators.abbrevateTeamName('DarkGogetters')).toBe('DG');
});

test('abbrevate team name will ignore special characters', () => {
  expect(abbrevators.abbrevateTeamName('[DBBC] DarkGogetters')).toBe('DDG');
});

test('abbrevate team name will ignore special characters', () => {
  expect(abbrevators.abbrevateTeamName('(WCQ) DarkGogetters')).toBe('WDG');
});
