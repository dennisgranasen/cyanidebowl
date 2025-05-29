import prettyPrint from './prettyPrint';

const makeInitials = (name) => {
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

const abbreviateText = (text, letterCount, suffix = '') => {
  const regex = new RegExp(`(^.{0,${letterCount}}).*`, 'g');
  return text && text.length > letterCount + 1 ? `${text.replace(regex, '$1')}${suffix}` : text;
};

const abbreviateCoachName = (text, letterCount = 3) => {
  return abbreviateText(text, letterCount, '.');
};

const abbreviateTeamName = (teamName) => {
  return makeInitials(teamName);
};

export default {
  makeInitials,
  abbreviateText,
  abbreviateCoachName,
  abbreviateTeamName,
};
