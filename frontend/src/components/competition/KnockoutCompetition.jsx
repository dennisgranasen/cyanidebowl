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
  HStack,
  Image,
} from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import { Icon, QuestionOutlineIcon } from '@chakra-ui/icons';
import { SingleEliminationBracket } from 'react-tournament-brackets/dist/cjs';
import imageUrls from '../../imageUrls';
import prettyPrint from '../../util/prettyPrint';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import formatter from '../../util/formatter';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import config from '../../config';
import useFetchContests from '../../hooks/useFetchContests';
import useFetchRanks from '../../hooks/useFetchRanks';
import Ranks from './Ranks';
import { FaRegFaceSadTear } from 'react-icons/fa6';

const { boxSize } = config;

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

function toParticipants(opponents, index, winner) {
  const participants = [];
  if (opponents) {
    opponents.forEach((opponent) => participants.push(toParticipant(opponent, winner)));
  }
  return participants;
}

function toBracketMatch(contest) {
  return {
    id: contest?.contestUuid,
    nextMatchId: contest?.nextContestUuid,
    participants: toParticipants(contest?.opponents, contest?.winner),
    startTime: formatter.formatAsDate(contest?.matchDate, '-'),
    state: contest?.matchDate ? 'DONE' : null,
    tournamentRoundText: `${contest?.round}`,
  };
}

function toBracketMatches(contests) {
  const matches = [];
  if (contests) {
    contests.forEach((contest) => matches.push(toBracketMatch(contest)));
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
            <Image
              src={imageUrls.logo(party.picture)}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="contain"
            />
          </Center>
        </GridItem>
        <GridItem
          pl="4px"
          area="team"
          w="100%"
          textAlign="left"
          fontWeight={won ? 'bold' : null}
          style={{ whiteSpace: 'nowrap' }}
        >
          <Box>{party.teamName || 'To be defined'}</Box>
        </GridItem>
        <GridItem pl="4px" area="coach" textAlign="left" fontSize="sm" color="grey" style={{ whiteSpace: 'nowrap' }}>
          {`${party.coachName || 'TBD'}, ${party.race ? prettyPrint(party.race) : 'unknown'}`}
        </GridItem>
        <GridItem area="score" textAlign="center" fontWeight={won ? 'bold' : null}>
          <Center w="100%" h="100%">
            {match.state === 'DONE' ? party.resultText : ''}
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
    <DelayedIconTooltip label={match.state === 'DONE' ? `Played ${topText}` : 'Scheduled'}>
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

function KnockoutCompetition({ competition, competitionLoading }) {
  const { fetchContests, contests, contestsLoading, error: contestError } = useFetchContests();
  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();
  const [matches, setMatches] = useState([]);

  useEffect(() => {
    if (competition) {
      fetchContests(competition);
      fetchRanks(competition);
    }
  }, [competition]);

  useEffect(() => {
    if (!competitionLoading && competition && !contestsLoading && contests) {
      const bracketMatches = toBracketMatches(contests);
      setMatches(bracketMatches);
    } else {
      setMatches([]);
    }
  }, [competition, competitionLoading, contests, contestsLoading]);

  return (
    <Accordion defaultIndex={[0]} allowMultiple>
      <AccordionItem>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            <Heading size="md">Knockout-Bracket</Heading>
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel overflow="auto">
          <LoadingOrErrorWrapper loading={competitionLoading || contestsLoading} error={contestError}>
            {matches && matches.length > 0 ? (
              <SingleEliminationBracket matches={matches} matchComponent={MatchComponent} />
            ) : (
              <HStack gap="1rem">
                <Icon as={FaRegFaceSadTear} boxSize={boxSize} />
                <Box>No matches yet...</Box>
              </HStack>
            )}
          </LoadingOrErrorWrapper>
        </AccordionPanel>
      </AccordionItem>
      <AccordionItem>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            <Heading size="md">Ranks</Heading>
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel>
          <Ranks
            competitionUuid={competition?.uuid}
            loading={competitionLoading || ranksLoading}
            ranks={ranks}
            error={ranksError}
          />
        </AccordionPanel>
      </AccordionItem>
    </Accordion>
  );
}

export default KnockoutCompetition;
