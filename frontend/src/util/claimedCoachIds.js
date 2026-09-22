export const MY_COACHES = '__my_coaches__';

export function claimedCoachIds(claims) {
  const games = { BB1: 1, BB2: 2, BB3: 3 };
  return [...new Set((claims || []).filter(claim => games[claim.game] && claim.coachId)
    .map(claim => `${games[claim.game]}_${claim.coachId}`.toLowerCase()))];
}
