import { Box, Center, Grid, GridItem, Heading, Image } from '@chakra-ui/react';
import { SingleEliminationBracket } from 'react-tournament-brackets/dist/esm';
import React from 'react';
import logger from '../../util/Logger';
import ImageUrls from '../../ImageUrls';
import prettyPrint from '../../util/PrettyPrint';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import formatter from '../../util/Formatter';

function toParticipant(opponent, winner) {
  return {
    id: opponent?.id,
    resultText: opponent ? `${opponent.score}` : null,
    isWinner: winner?.team?.id === opponent?.id,
    status: opponent ? 'PLAYED' : null,
    teamName: opponent?.name,
    coachName: opponent?.coachName,
    race: opponent?.race,
    picture: opponent?.logo,
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
    startTime: formatter.formatAsDate(contest?.matchDate),
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

function Participant({ party, won, hovered, teamNameFallback, resultFallback, onMouseEnter, connectorColor }) {
  let borderColor = hovered ? 'gray.600' : connectorColor;
  const backgroundColor = hovered ? 'gray.600' : null;
  if (won) {
    borderColor = 'white';
  }
  return (
    <Box
      m="0"
      p="0"
      borderColor={borderColor}
      borderWidth="1px"
      overflow="hidden"
      backgroundColor={backgroundColor}
      onMouseEnter={() => onMouseEnter(party.id)}
    >
      <Grid
        w="100%"
        onMouseEnter={() => onMouseEnter(party.id)}
        templateAreas={`"image team score"
                  "image coach score"`}
        gridTemplateColumns="32px 1fr 32px"
      >
        <GridItem p="2px" area="image" textAlign="center">
          <Center w="100%" h="100%">
            <Image src={ImageUrls.logo(party.picture)} objectFit="contain" />
          </Center>
        </GridItem>
        <GridItem pl="2px" area="team" w="100%" textAlign="left">
          {party.teamName || teamNameFallback}
        </GridItem>
        <GridItem pl="2px" area="coach" textAlign="left" color="grey">
          {`${party.coachName}, ${prettyPrint(party.race)}`}
        </GridItem>
        <GridItem area="score" textAlign="center" fontWeight="bold">
          <Center w="100%" h="100%">
            {party.resultText ?? resultFallback(party)}
          </Center>
        </GridItem>
      </Grid>
    </Box>
  );
}

function MatchComponent({
  onPartyClick,
  onMouseEnter,
  topParty,
  bottomParty,
  topWon,
  bottomWon,
  topHovered,
  bottomHovered,
  topText,
  connectorColor,
  teamNameFallback,
  resultFallback,
}) {
  return (
    <DelayedIconTooltip label={topText ? `Played ${topText}` : 'Scheduled'}>
      <div
        style={{
          cursor: 'pointer',
          marginTop: '5px',
        }}
      >
        <Participant
          party={topParty}
          won={topWon}
          hovered={topHovered}
          teamNameFallback={teamNameFallback}
          resultFallback={resultFallback}
          connectorColor={connectorColor}
          onMouseEnter={onMouseEnter}
          onPartyClick={onPartyClick}
        />
        <Participant
          party={bottomParty}
          won={bottomWon}
          hovered={bottomHovered}
          teamNameFallback={teamNameFallback}
          resultFallback={resultFallback}
          connectorColor={connectorColor}
          onMouseEnter={onMouseEnter}
          onPartyClick={onPartyClick}
        />
      </div>
    </DelayedIconTooltip>
  );
}

function KnockoutCompetition({ contests, competition }) {
  logger.debug('Contests: %o', contests);
  const matches = toBracketMatches(contests, competition.teamsMax);
  logger.debug('Matches: %o', matches);
  return (
    <>
      <Heading size="md">Knockout-Bracket</Heading>
      <Box align="center" height="100%" width="100%" overflowX="scroll">
        {matches && <SingleEliminationBracket matches={matches} matchComponent={MatchComponent} />}
      </Box>
    </>
  );
}

export default KnockoutCompetition;
