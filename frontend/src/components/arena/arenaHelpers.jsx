import comparators from '../../util/comparators';

function getLastMatch(matches) {
  const sortedMatches = [].concat(matches.sort(comparators.compareContestsByMatchOrContestUuidAsDatesAsc));
  return sortedMatches.pop();
}

const teamsByLastMatchDate = (team1, team2) => {
  const match1 = getLastMatch(team1.matches);
  const match2 = getLastMatch(team2.matches);
  return comparators.compareContestsByMatchOrContestUuidAsDatesDesc(match1, match2);
};

function isWinner(teamUuid, match) {
  const teams = [].concat(match.teams);
  teams.sort((team1, team2) => team2.score - team1.score);
  return teamUuid === teams[0].id;
}

const clusterMatches = (teamUuid, matches) => {
  let wins = 0;
  let losses = 0;
  const clusteredMatches = [];
  let currMatches = [];
  matches.forEach((match) => {
    currMatches.push(match);
    if (isWinner(teamUuid, match)) {
      wins += 1;
    } else {
      losses += 1;
    }
    if (wins === 7 || losses === 2) {
      clusteredMatches.push(currMatches);
      wins = 0;
      losses = 0;
      currMatches = [];
    }
  });
  if (currMatches.length > 0) {
    clusteredMatches.push(currMatches);
  }
  return clusteredMatches.sort((c1, c2) =>
    comparators.compareContestsByMatchOrContestUuidAsDatesAsc(getLastMatch(c1), getLastMatch(c2))
  );
};

const earliestFinished = (matches) => {
  return matches.reduce((earliest, curr) => {
    if (!earliest) return curr.finished;
    if (curr.finished) {
      return curr.finished < earliest ? curr.finished : earliest;
    }
    return earliest;
  }, null);
};

const latestFinished = (matches) => {
  return matches.reduce((latest, curr) => {
    if (!latest) return curr.finished;
    if (curr.finished) {
      return curr.finished > latest ? curr.finished : latest;
    }
    return latest;
  }, null);
};

export default {
  clusterMatches,
  isWinner,
  teamsByLastMatchDate,
  getLastMatch,
  earliestFinished,
  latestFinished,
};
