import {
  articleAuthorProfilePath,
  commentAuthorProfilePath,
  matchArticleAuthorProfilePath,
} from './communityAuthorProfiles';

test('comment authors link to fan and reporter profiles', () => {
  expect(commentAuthorProfilePath({ authorFanProfileId: 'fan 1' })).toBe('/community/fan%201');
  expect(commentAuthorProfilePath({ authorSubject: 'ai:writer-1' })).toBe('/staff/writer-1');
});

test('coach comments link to the coach profile for the match competition', () => {
  const match = {
    competitionId: { key: 'BB3_competition' },
    coaches: [{ id: { key: 'home coach' } }, { id: { key: 'away coach' } }],
  };
  expect(commentAuthorProfilePath({ authorContext: 'AWAY_COACH' }, match))
    .toBe('/competition/BB3_competition/arena/coach/away%20coach');
});

test('article authors link to their fan or reporter profile', () => {
  expect(articleAuthorProfilePath({ authorFanProfileId: 'fan-2' })).toBe('/community/fan-2');
  expect(articleAuthorProfilePath({ authorType: 'AI_REPORTER', authorAgentId: 'reporter-2' }))
    .toBe('/staff/reporter-2');
});

test('match article coach contributions use the matching team coach profile', () => {
  const match = {
    competitionId: 'BB3_competition',
    teams: [{ id: { key: 'home' } }, { id: { key: 'away' } }],
    coaches: [{ id: 'coach-home' }, { id: 'coach-away' }],
  };
  expect(matchArticleAuthorProfilePath({ kind: 'COACH_CONTRIBUTION', teamId: 'away' }, match))
    .toBe('/competition/BB3_competition/arena/coach/coach-away');
});
