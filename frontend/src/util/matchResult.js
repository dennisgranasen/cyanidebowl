export function matchScore(match, index) {
  const side = index === 0 ? 'home' : 'away';
  return match?.officialScore?.[side]
    ?? match?.sourceScore?.[side]
    ?? match?.teams?.[index]?.score
    ?? null;
}

export function touchdownScore(match, index) {
  const team = match?.teams?.[index];
  return team?.touchdowns ?? team?.inflictedtouchdowns ?? null;
}

export function scoreDiffersFromTouchdowns(match) {
  if (!match?.teams || match.teams.length < 2) return false;
  const scores = [matchScore(match, 0), matchScore(match, 1)];
  const touchdowns = [touchdownScore(match, 0), touchdownScore(match, 1)];
  return scores.every(Number.isFinite)
    && touchdowns.every(Number.isFinite)
    && (scores[0] !== touchdowns[0] || scores[1] !== touchdowns[1]);
}