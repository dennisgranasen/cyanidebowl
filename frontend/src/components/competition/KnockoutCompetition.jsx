import { Box, Center, Heading } from '@chakra-ui/react';
import { Match, SingleEliminationBracket } from 'react-tournament-brackets/dist/esm';
import React from 'react';
import logger from '../../util/Logger';
import ImageUrls from '../../ImageUrls';

function toParticipant(opponent, winner) {
  return {
    id: opponent?.id,
    resultText: opponent ? `${opponent.score}` : null,
    isWinner: winner?.team?.id === opponent?.id,
    status: opponent ? 'PLAYED' : null,
    name: opponent?.name,
    picture: opponent ? ImageUrls.logo(opponent.logo) : null,
  };
}

function toParticipants(opponents, winner) {
  logger.debug('Opponents: %o', opponents);
  const participants = [];
  if (opponents) {
    opponents.forEach((opponent) => participants.push(toParticipant(opponent, winner)));
  }
  logger.debug('Participants: %o', participants);
  return participants;
}

function getNextMatchId(teamId, currentRound, contests) {
  let nextMatch = null;
  if (contests) {
    contests.forEach((contest) => {
      if (contest.round === currentRound + 1 && contest.opponents.map((opponent) => opponent.id).includes(teamId)) {
        nextMatch = contest;
      }
    });
  }
  logger.debug('Next match: %o', nextMatch);
  return nextMatch?.contestUuid;
}

function toBracketMatch(contest, contests) {
  return {
    id: contest?.contestUuid,
    nextMatchId: getNextMatchId(contest?.winner?.team?.id, contest?.round, contests),
    participants: toParticipants(contest?.opponents, contest?.winner),
    startTime: contest?.matchDate,
    state: contest?.matchDate ? 'DONE' : null,
    tournamentRoundText: `${contest?.round}`,
  };
}

function toBracketMatches(contests) {
  const matches = [];
  if (contests) {
    contests.forEach((contest) => matches.push(toBracketMatch(contest, contests)));
  }
  return matches;
}

function KnockoutCompetition({ contests, competition }) {
  logger.debug('Contests: %o', contests);
  const matches = toBracketMatches(contests, competition.teamsMax);
  logger.debug('Matches: %o', matches);
  return (
    <>
      <Heading size="md">Knockout-Bracket</Heading>
      <Box align="center">{matches && <SingleEliminationBracket matches={matches} matchComponent={Match} />}</Box>
    </>
  );
}

export default KnockoutCompetition;
