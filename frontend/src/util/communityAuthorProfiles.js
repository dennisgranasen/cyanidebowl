const identityKey = (value) => value && typeof value === 'object'
  ? value.key || value.id
  : value;

const coachProfilePath = (match, coachIndex) => {
  const competitionId = identityKey(match?.competitionId);
  const coachId = identityKey(match?.coaches?.[coachIndex]?.id);
  if (!competitionId || !coachId) return null;
  return `/competition/${encodeURIComponent(competitionId)}/arena/coach/${encodeURIComponent(coachId)}`;
};

const profilePath = (type, id) => id ? `/${type}/${encodeURIComponent(id)}` : null;

export const commentAuthorProfilePath = (comment, match) => {
  if (comment.authorSubject?.startsWith('ai:')) {
    return profilePath('staff', comment.authorSubject.slice(3));
  }
  if (comment.authorContext === 'HOME_COACH' || comment.authorContext === 'AWAY_COACH') {
    const coachPath = coachProfilePath(match, comment.authorContext === 'HOME_COACH' ? 0 : 1);
    if (coachPath) return coachPath;
  }
  return profilePath('community', comment.authorFanProfileId);
};

export const articleAuthorProfilePath = (article) => {
  if (article.authorType === 'AI_REPORTER' || article.authorAgentId || article.authorSubject?.startsWith('ai:')) {
    return profilePath('staff', article.authorAgentId || article.authorSubject?.slice(3));
  }
  return profilePath('community', article.authorFanProfileId);
};

export const matchArticleAuthorProfilePath = (article, match) => {
  if (article.authorType === 'AI' && article.reporterId) {
    return profilePath('staff', article.reporterId);
  }
  if (article.kind === 'COACH_CONTRIBUTION') {
    const teamId = String(identityKey(article.teamId) || '');
    const teamIndex = (match?.teams || []).findIndex((team) => String(identityKey(team.id)) === teamId);
    const coachPath = teamIndex >= 0 ? coachProfilePath(match, teamIndex) : null;
    if (coachPath) return coachPath;
  }
  return profilePath('community', article.authorFanProfileId);
};
