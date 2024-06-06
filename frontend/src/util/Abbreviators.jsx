import prettyPrint from './PrettyPrint';

const abbreviate = (name) => {
  if (!name) return name;

  const prettyName = prettyPrint(name);
  const regex = /\b([A-Za-z1-9])/g;
  const parts = [...prettyName.matchAll(regex)];
  let abbreviatedName = '';
  parts.forEach((partMatch) => {
    abbreviatedName = `${abbreviatedName}${partMatch[0]}`;
  });
  return abbreviatedName;
};

const abbreviateCoachName = (coachName, letterCount = 3) => {
  const regex = new RegExp(`(^.{0,${letterCount}}).*`, 'g');
  return coachName && coachName.length > letterCount + 1 ? coachName.replace(regex, '$1.') : coachName;
};

const abbreviateTeamName = (teamName) => {
  return abbreviate(teamName);
};

export default {
  abbreviate,
  abbreviateCoachName,
  abbreviateTeamName,
};
