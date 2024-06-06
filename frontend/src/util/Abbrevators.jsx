import prettyPrint from './PrettyPrint';

const abbrevateCoachName = (coachName, letterCount = 3) => {
  const regex = new RegExp(`(^.{0,${letterCount}}).*`, 'g');
  return coachName && coachName.length > letterCount + 1 ? coachName.replace(regex, '$1.') : coachName;
};

const abbrevateTeamName = (teamName) => {
  if (!teamName) return teamName;

  const prettyTeamName = prettyPrint(teamName);
  const regex = /\b([A-Za-z1-9])/g;
  const matched = [...prettyTeamName.matchAll(regex)];
  let abbrevatedTeamName = '';
  matched.forEach((match) => {
    abbrevatedTeamName = `${abbrevatedTeamName}${match[0]}`;
  });
  return abbrevatedTeamName;
};

export default {
  abbrevateCoachName,
  abbrevateTeamName,
};
