import { bracketCanvasHeight, inferPlayoffRounds, matchesByMatchDay, nextPlayoffMatch } from './LeagueSystems';

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
    playoffMatch('different-document-id', '2023-01-06', 'Finalist A', 'Bronze A', 2, 1),
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

test('collapses drawn quarterfinal and semifinal replays into their knockout ties', () => {
  const matches = [
    playoffMatch('q1-draw-1', '2025-01-01', 'Bleak Night Boneguard', 'Hufvudstadens Ädla', 1, 1),
    playoffMatch('q1-draw-2', '2025-01-02', 'Hufvudstadens Ädla', 'Bleak Night Boneguard', 2, 2),
    playoffMatch('q1-wo', '2025-01-03', 'Bleak Night Boneguard', 'Hufvudstadens Ädla', 0, 2),
    playoffMatch('q2', '2025-01-04', 'Semifinalist B', 'Quarter loser B', 2, 0),
    playoffMatch('q3', '2025-01-05', 'Finalist C', 'Quarter loser C', 2, 1),
    playoffMatch('q4', '2025-01-06', 'Semifinalist D', 'Quarter loser D', 3, 1),
    playoffMatch('s1-draw', '2025-01-07', 'Hufvudstadens Ädla', 'Semifinalist B', 1, 1),
    playoffMatch('s1-replay', '2025-01-08', 'Semifinalist B', 'Hufvudstadens Ädla', 0, 1),
    playoffMatch('s2', '2025-01-09', 'Finalist C', 'Semifinalist D', 2, 0),
    playoffMatch('final', '2025-01-10', 'Hufvudstadens Ädla', 'Finalist C', 2, 1),
  ];

  const rounds = inferPlayoffRounds(matches);

  expect(rounds.map((round) => [round.name, round.matches.length])).toEqual([
    ['Quarterfinals', 4], ['Semifinals', 2], ['Final', 1],
  ]);
  const replayedQuarterfinal = rounds[0].matches.find((match) => match.seriesLength === 3);
  const replayedSemifinal = rounds[1].matches.find((match) => match.seriesLength === 2);
  expect(replayedQuarterfinal.sourceMatchKey).toBe('q1-wo');
  expect(replayedQuarterfinal.seriesWinnerName).toBe('Hufvudstadens Ädla');
  expect(replayedQuarterfinal.replayCount).toBe(2);
  expect(replayedSemifinal.sourceMatchKey).toBe('s1-replay');
  expect(replayedSemifinal.replayCount).toBe(1);
});

test('infers depth from advancing teams when a quarterfinal starts before all play-ins finish', () => {
  const matches = [
    ...Array.from({ length: 4 }, (_, index) => playoffMatch(`p${index}`, index === 3 ? '2025-02-09' : `2025-02-0${index + 1}`, `Play-in winner ${index}`, `Play-in loser ${index}`, 1, 0)),
    ...Array.from({ length: 4 }, (_, index) => playoffMatch(`q${index}`, index === 0 ? '2025-02-05' : `2025-02-1${index}`, `Semifinalist ${index}`, `Play-in winner ${index}`, 2, 0)),
    playoffMatch('s1', '2025-02-20', 'Semifinalist 0', 'Semifinalist 1', 2, 0),
    playoffMatch('s2', '2025-02-21', 'Semifinalist 2', 'Semifinalist 3', 1, 0),
    playoffMatch('f1', '2025-02-22', 'Semifinalist 0', 'Semifinalist 2', 2, 1),
  ];

  const rounds = inferPlayoffRounds(matches);

  expect(rounds.map((round) => [round.name, round.matches.length])).toEqual([
    ['Play-in', 4], ['Quarterfinals', 4], ['Semifinals', 2], ['Final', 1],
  ]);
  expect(rounds[0].matches.map((match) => match.sourceMatchKey)).toContain('p3');
  expect(rounds[1].matches.map((match) => match.sourceMatchKey)).toContain('q0');
  expect(new Date(matches.find((match) => match.sourceMatchKey === 'q0').finishedAt).getTime())
    .toBeLessThan(new Date(matches.find((match) => match.sourceMatchKey === 'p3').finishedAt).getTime());
  rounds.slice(0, -1).forEach((round) => round.matches.forEach((match) => {
    const next = nextPlayoffMatch(rounds, match);
    expect(next).not.toBeNull();
    expect(next.teams.some((team) => match.teams.some((participant) => participant.name === team.name))).toBe(true);
  }));
  expect(nextPlayoffMatch(rounds, rounds[3].matches[0])).toBeNull();
});

test('keeps every branch connected by position when legacy team identities do not match', () => {
  const rounds = [
    { matches: ['p1', 'p2', 'p3', 'p4'].map((key) => playoffMatch(key, '2025-03-01', `${key} A`, `${key} B`, 1, 0)) },
    { matches: ['q1', 'q2', 'q3', 'q4'].map((key) => playoffMatch(key, '2025-03-02', `${key} A`, `${key} B`, 1, 0)) },
    { matches: ['s1', 's2'].map((key) => playoffMatch(key, '2025-03-03', `${key} A`, `${key} B`, 1, 0)) },
    { matches: [playoffMatch('f1', '2025-03-04', 'f1 A', 'f1 B', 1, 0)] },
  ];

  expect(rounds[0].matches.map((match) => nextPlayoffMatch(rounds, match).sourceMatchKey)).toEqual(['q1', 'q2', 'q3', 'q4']);
  expect(rounds[1].matches.map((match) => nextPlayoffMatch(rounds, match).sourceMatchKey)).toEqual(['s1', 's1', 's2', 's2']);
  expect(rounds[2].matches.map((match) => nextPlayoffMatch(rounds, match).sourceMatchKey)).toEqual(['f1', 'f1']);
});

test('makes the canvas tall enough when play-ins and quarterfinals have equal counts', () => {
  const rounds = [
    { matches: Array.from({ length: 4 }, (_, index) => ({ sourceMatchKey: `p${index}` })) },
    { matches: Array.from({ length: 4 }, (_, index) => ({ sourceMatchKey: `q${index}` })) },
    { matches: Array.from({ length: 2 }, (_, index) => ({ sourceMatchKey: `s${index}` })) },
    { matches: [{ sourceMatchKey: 'final' }] },
  ];
  const style = { boxHeight: 86, spaceBetweenRows: 8, canvasPadding: 8, roundHeader: { isShown: true, height: 20, marginBottom: 6 } };

  expect(bracketCanvasHeight(rounds, style)).toBe(8 * 94 + 16 + 26);
});

test('derives match days from each teams sequence instead of the Cyanide round field', () => {
  const leagueMatch = (key, finishedAt, home, away) => ({
    sourceMatchKey: key,
    finishedAt,
    round: '1',
    teams: [{ name: home }, { name: away }],
  });
  const matches = [
    leagueMatch('a-b', '2026-01-01', 'A', 'B'),
    leagueMatch('c-d', '2026-01-05', 'C', 'D'),
    leagueMatch('a-c', '2026-01-08', 'A', 'C'),
    leagueMatch('b-d', '2026-01-10', 'B', 'D'),
    leagueMatch('a-d', '2026-01-12', 'A', 'D'),
    leagueMatch('b-c', '2026-01-14', 'B', 'C'),
  ];

  expect(matchesByMatchDay(matches).map(([matchDay, dayMatches]) => [
    matchDay, dayMatches.map((match) => match.sourceMatchKey),
  ])).toEqual([
    [1, ['a-b', 'c-d']],
    [2, ['a-c', 'b-d']],
    [3, ['a-d', 'b-c']],
  ]);
});

test('keeps a six-team round robin at five match days when an early match is postponed', () => {
  const leagueMatch = (key, finishedAt, home, away) => ({
    sourceMatchKey: key, finishedAt, round: '1', teams: [{ name: home }, { name: away }],
  });
  const intendedRounds = [
    [['A', 'F'], ['B', 'E'], ['C', 'D']],
    [['A', 'E'], ['F', 'D'], ['B', 'C']],
    [['A', 'D'], ['E', 'C'], ['F', 'B']],
    [['A', 'C'], ['D', 'B'], ['E', 'F']],
    [['A', 'B'], ['C', 'F'], ['D', 'E']],
  ];
  const matches = intendedRounds.flatMap((round, roundIndex) => round.map(([home, away], matchIndex) => {
    const day = roundIndex === 1 && matchIndex === 2 ? 18 : roundIndex * 3 + matchIndex + 1;
    return leagueMatch(`${home}-${away}`, `2026-01-${String(day).padStart(2, '0')}`, home, away);
  }));

  const matchDays = matchesByMatchDay(matches);

  expect(matchDays).toHaveLength(5);
  expect(matchDays.every(([, dayMatches]) => dayMatches.length === 3)).toBe(true);
  matchDays.forEach(([, dayMatches]) => {
    const teams = dayMatches.flatMap((match) => match.teams.map((team) => team.name));
    expect(new Set(teams).size).toBe(6);
  });
});
