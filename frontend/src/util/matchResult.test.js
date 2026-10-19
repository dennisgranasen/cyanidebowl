import { matchScore, scoreDiffersFromTouchdowns } from './matchResult';

test('detects a final score that differs from match touchdown statistics', () => {
  const match = {
    sourceScore: { home: 2, away: 0 },
    teams: [{ touchdowns: 1 }, { touchdowns: 0 }],
  };

  expect(scoreDiffersFromTouchdowns(match)).toBe(true);
  expect([matchScore(match, 0), matchScore(match, 1)]).toEqual([2, 0]);
});

test('reads inflicted touchdowns from individual match data', () => {
  const match = {
    teams: [{ score: 2, inflictedtouchdowns: 1 }, { score: 0, inflictedtouchdowns: 0 }],
  };

  expect(scoreDiffersFromTouchdowns(match)).toBe(true);
});

test('does not infer a mismatch when touchdown statistics are missing', () => {
  expect(scoreDiffersFromTouchdowns({
    teams: [{ score: 2 }, { score: 0 }],
  })).toBe(false);
});