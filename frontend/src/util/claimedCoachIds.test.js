import { claimedCoachIds } from './claimedCoachIds';

test('matches claims to canonical coach identities without mixing games', () => {
  expect(claimedCoachIds([
    { game: 'BB1', coachId: '42' }, { game: 'BB3', coachId: '42' },
    { game: 'BB3', coachId: 'ABC' }, { game: 'BB3', coachId: 'abc' },
    { game: 'unknown', coachId: '42' }, { game: 'BB2', coachId: null },
  ])).toEqual(['1_42', '3_42', '3_abc']);
  expect(claimedCoachIds([])).toEqual([]);
});
