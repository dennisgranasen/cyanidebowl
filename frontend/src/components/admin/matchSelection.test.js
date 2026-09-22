import { isMatchSelected, toggleMatch, matchDuration } from './matchSelection';

test('configured defaults and explicit exclusion take precedence', () => {
  const match = { key: 'bb1-match', automaticSelected: true };
  expect(isMatchSelected(match, {})).toBe(true);
  expect(isMatchSelected(match, { includedMatchIds: [match.key], excludedMatchIds: [match.key] })).toBe(false);
  expect(isMatchSelected({ ...match, automaticSelected: false }, { includedMatchIds: [match.key] })).toBe(true);
});

test('checkbox changes are immutable and preserve overrides for other matches', () => {
  const source = { includedMatchIds: ['other'], excludedMatchIds: ['match', 'absent'] };
  const included = toggleMatch(source, 'match', true);
  expect(source.excludedMatchIds).toEqual(['match', 'absent']);
  expect(included.includedMatchIds).toEqual(['other', 'match']);
  expect(included.excludedMatchIds).toEqual(['absent']);
  const excluded = toggleMatch(included, 'match', false);
  expect(isMatchSelected({ key: 'match', automaticSelected: true }, excluded)).toBe(false);
  expect(excluded.includedMatchIds).toEqual(['other']);
});

test('legacy timestamps do not claim a zero duration', () => {
  const start = '2011-02-03T20:56:03Z';
  expect(matchDuration(start, start)).toBe('Unknown');
  expect(matchDuration(start, null)).toBe('Unknown');
  expect(matchDuration(start, '2011-02-03T22:26:03Z')).toBe('1h 30m');
});
