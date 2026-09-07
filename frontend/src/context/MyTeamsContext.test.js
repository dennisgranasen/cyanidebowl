import { coachClaimKey, isCoachClaimed } from './MyTeamsContext';

describe('MyTeams claim matching', () => {
  const claims = [
    { game: 'BB3', coachId: 'ABC-123', coachName: 'Dennis' },
    { game: 'BB1', coachId: 'old-coach', coachName: 'Dennis' },
  ];

  test('matches a coach only in the claimed Blood Bowl edition', () => {
    expect(isCoachClaimed(claims, 'ABC-123', 3)).toBe(true);
    expect(isCoachClaimed(claims, 'ABC-123', 2)).toBe(false);
    expect(isCoachClaimed(claims, 'ABC-123', 1)).toBe(false);
  });

  test('normalizes coach identity casing', () => {
    expect(isCoachClaimed(claims, 'abc-123', 3)).toBe(true);
    expect(coachClaimKey('BB3', 'ABC-123')).toBe('BB3:abc-123');
  });

  test('does not match missing coach or opus', () => {
    expect(isCoachClaimed(claims, null, 3)).toBe(false);
    expect(isCoachClaimed(claims, 'ABC-123', null)).toBe(false);
  });
});
