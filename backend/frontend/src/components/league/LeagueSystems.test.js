import { inferPlayoffRounds } from './LeagueSystems';

const playoffMatch = (key, finishedAt, home, away, homeScore, awayScore) => ({
  sourceMatchKey: key,
  finishedAt,
  teams: [{ name: home }, { name: away }],
  officialScore: { home: homeScore, away: awayScore },
  countsFor: { bracket: true },
});

test('infers play-in, quarterfinal, semifinal and final rounds from a combined playoff stage', () => {
  const matches = [
    playoffMatch('p1', '2024-01-01', 'Karak-katalun', 'Play-in opponent 1', 2, 1),
    playoffMatch('p2', '2024-01-02', 'Blackburg', 'Play-in opponent 2', 2, 0),
    playoffMatch('q1', '2024-01-03', 'Nottingham', 'Karak-katalun', 2, 1),
    playoffMatch('q2', '2024-01-04', 'Hufvudstadens ädla', 'Hardest Blocked Life', 1, 0),
    playoffMatch('q3', '2024-01-05', 'The Fouling Gang', 'Blackburg', 3, 1),
    playoffMatch('q4', '2024-01-06', 'Råttfällan', 'Troublemakers', 2, 0),
    playoffMatch('s1', '2024-01-07', 'Nottingham', 'Hufvudstadens ädla', 2, 1),
    playoffMatch('s2', '2024-01-08', 'Råttfällan', 'The Fouling Gang', 1, 0),
    playoffMatch('f1', '2024-01-09', 'Nottingham', 'Råttfällan', 2, 1),
  ];

  const rounds = inferPlayoffRounds(matches);

  expect(rounds.map((round) => [round.name, round.matches.length])).toEqual([
    ['Play-in', 2],
    ['Quarterfinals', 4],
    ['Semifinals', 2],
    ['Final', 1],
  ]);
  expect(rounds[1].matches.map((match) => match.sourceMatchKey)).toEqual(['q1', 'q2', 'q4', 'q3']);
  expect(rounds[2].matches.map((match) => match.sourceMatchKey)).toEqual(['s1', 's2']);
});

test('separates a final and bronze match, has no play-in, and removes a semantic duplicate', () => {
  const matches = [
    playoffMatch('q1', '2023-01-02', 'Finalist A', 'Quarter loser A', 2, 0),
    playoffMatch('q2', '2023-01-03', 'Bronze A', 'Quarter loser B', 1, 0),
    playoffMatch('q3', '2023-01-04', 'Finalist B', 'Quarter loser C', 2, 1),
    playoffMatch('q4', '2023-01-05', 'Bronze B', 'Quarter loser D', 3, 1),
    playoffMatch('s1', '2023-01-06', 'Finalist A', 'Bronze A', 2, 1),
    playoffMatch('s2', '2023-01-07', 'Finalist B', 'Bronze B', 1, 0),
    playoffMatch('final', '2023-01-08', 'Finalist A', 'Finalist B', 2, 1),
    playoffMatch('bronze', '2023-01-09', 'Bronze A', 'Bronze B', 2, 0),
    playoffMatch('different-document-id', '2023-02-09', 'Finalist A', 'Bronze A', 2, 1),
  ];
  matches[4].teams = [{ name: 'Finalist A' }, { name: 'Bronze A' }];
  matches[5].teams = [{ name: 'Finalist B' }, { name: 'Bronze B' }];

  const rounds = inferPlayoffRounds(matches);

  expect(rounds.map((round) => [round.name, round.matches.length])).toEqual([
    ['Quarterfinals', 4], ['Semifinals', 2], ['Finals', 2],
  ]);
  expect(rounds[2].matchNames).toEqual({ final: 'Final', bronze: 'Bronze match' });
  expect(rounds[0].matches.map((match) => match.sourceMatchKey)).toEqual(['q1', 'q2', 'q3', 'q4']);
  expect(rounds[1].matches.map((match) => match.sourceMatchKey)).toEqual(['s1', 's2']);
  expect(rounds[2].matches.map((match) => match.sourceMatchKey)).toEqual(['final', 'bronze']);
});
