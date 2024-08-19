import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Center,
  Grid,
  GridItem,
  Heading,
  Image,
} from '@chakra-ui/react';
import { SingleEliminationBracket } from 'react-tournament-brackets/dist/esm';
import React, { useEffect, useState } from 'react';
import logger from '../../util/Logger';
import ImageUrls from '../../ImageUrls';
import prettyPrint from '../../util/PrettyPrint';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import formatter from '../../util/Formatter';
import Ranks from './Ranks';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

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

function Participant({
  match,
  party,
  won,
  hovered,
  borderTopRadius,
  borderBottomRadius,
  connectorColor,
  teamNameFallback,
  resultFallback,
  onMouseEnter,
  onMouseLeave,
  onMatchClick,
  onPartyClick,
}) {
  const borderColor = hovered ? 'warpScoresHoverColor' : connectorColor;
  const backgroundColor = hovered ? 'warpScoresHoverColor' : null;
  return (
    <Box
      m="0"
      p="2px"
      borderColor={borderColor}
      borderWidth="1px"
      borderTopRadius={borderTopRadius}
      borderBottomRadius={borderBottomRadius}
      overflow="hidden"
      backgroundColor={backgroundColor}
      onMouseEnter={() => onMouseEnter(party.id)}
      onMouseLeave={() => onMouseLeave(party.id)}
      onPartyClick={onPartyClick}
      onMatchClick={onMatchClick}
    >
      <Grid
        w="100%"
        templateAreas={`"image team score"
                  "image coach score"`}
        gridTemplateColumns="40px 1fr 32px"
      >
        <GridItem pl="4px" pr="4px" area="image" textAlign="center">
          <Center w="100%" h="100%">
            <Image src={ImageUrls.logo(party.picture)} objectFit="contain" />
          </Center>
        </GridItem>
        <GridItem pl="4px" area="team" w="100%" textAlign="left" fontWeight={won ? 'bold' : null}>
          {party.teamName || teamNameFallback}
        </GridItem>
        <GridItem pl="4px" area="coach" textAlign="left" fontSize="sm" color="grey">
          {`${party.coachName}, ${prettyPrint(party.race)}`}
        </GridItem>
        <GridItem area="score" textAlign="center" fontWeight={won ? 'bold' : null}>
          <Center w="100%" h="100%">
            {party.resultText ?? resultFallback(party)}
          </Center>
        </GridItem>
      </Grid>
    </Box>
  );
}

function MatchComponent({
  match,
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
  onPartyClick,
  onMatchClick,
  onMouseEnter,
  onMouseLeave,
}) {
  return (
    <DelayedIconTooltip label={topText ? `Played ${topText}` : 'Scheduled'}>
      <div
        style={{
          cursor: 'pointer',
          marginTop: '4px',
        }}
      >
        <Participant
          match={match}
          party={topParty}
          won={topWon}
          hovered={topHovered}
          borderTopRadius="sm"
          teamNameFallback={teamNameFallback}
          resultFallback={resultFallback}
          connectorColor={connectorColor}
          onMouseEnter={onMouseEnter}
          onMouseLeave={onMouseLeave}
          onPartyClick={onPartyClick}
          onMatchClick={onMatchClick}
        />
        <Participant
          match={match}
          party={bottomParty}
          won={bottomWon}
          hovered={bottomHovered}
          borderBottomRadius="sm"
          teamNameFallback={teamNameFallback}
          resultFallback={resultFallback}
          connectorColor={connectorColor}
          onMouseEnter={onMouseEnter}
          onMouseLeave={onMouseLeave}
          onPartyClick={onPartyClick}
          onMatchClick={onMatchClick}
        />
      </div>
    </DelayedIconTooltip>
  );
}

function KnockoutCompetition({ ranks, contests, competition, ranksLoading, contestsLoading, competitionLoading }) {
  const [matches, setMatches] = useState([]);

  useEffect(() => {
    const bracketMatches =
      !competitionLoading && contests && competition ? toBracketMatches(contests, competition?.teamsMax) : [];
    setMatches(bracketMatches);
  }, [competition, competitionLoading, contests]);
  return (
    <>
      <Heading size="md">Knockout-Bracket</Heading>
      <Box align="center" height="100%" width="100%" overflowX="scroll">
        <LoadingOrErrorWrapper loading={contestsLoading}>
          {matches && matches.length > 0 && (
            <SingleEliminationBracket matches={matches} matchComponent={MatchComponent} />
          )}
        </LoadingOrErrorWrapper>
      </Box>
      <Accordion allowMultiple>
        <AccordionItem>
          <AccordionButton>
            <Box as="span" flex="1" textAlign="left">
              <Heading size="md">Ranks</Heading>
            </Box>
            <AccordionIcon />
          </AccordionButton>
          <AccordionPanel>
            <Ranks loading={ranksLoading} ranks={ranks} />
          </AccordionPanel>
        </AccordionItem>
      </Accordion>
    </>
  );
}

export default KnockoutCompetition;
