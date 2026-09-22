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
import useFetchContestsWithMatches from '../../hooks/useFetchContestsWithMatches';
import useFetchRanks from '../../hooks/useFetchRanks';
import Ranks from './Ranks';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import MatchModalWithRosters from '../contest/MatchModalWithRosters'; // Add this import
import { useDisclosure } from '@chakra-ui/react';
import { identityUtils } from '../../util/identityUtil';
import { useMyTeams } from '../../context/MyTeamsContext';

const { boxSize } = config;

function toParticipant(opponent, winner) {
  return {
    id: opponent?.id,
    resultText: opponent ? `${opponent.score}` : null,
    isWinner: winner?.team?.id === opponent?.id.value,
    status: opponent ? 'PLAYED' : null,
    teamName: opponent?.name,
    coachName: opponent?.coachName,
    race: opponent?.race,
    picture: opponent?.logo,
    onMatchClick: (matchId) => {
      console.log('Match clicked:', matchId);
    },
    onPartyClick: (partyId) => {
      console.log('Party clicked:', partyId);
    }
  };
}

function toParticipants(opponents, winner) {
  const participants = [];
  if (opponents) {
    opponents.forEach((opponent) => participants.push(toParticipant(opponent, winner)));
  }
  return participants;
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
  compact,
  //onPartyClick,
}) {
  const { isMyTeam } = useMyTeams();
  const mine = isMyTeam(party?.id);
  const borderColor = hovered ? 'warpScoresHoverColor' : mine ? 'green.400' : connectorColor;
  const backgroundColor = hovered ? 'warpScoresHoverColor' : null;
  const details = [party.coachName, party.race ? prettyPrint(party.race) : null].filter(Boolean).join(', ');
  return (
    <Box
      m="0"
      p="2px"
      h={compact ? '50%' : undefined}
      borderColor={borderColor}
      borderWidth="1px"
      borderTopRadius={borderTopRadius}
      borderBottomRadius={borderBottomRadius}
      overflow="hidden"
      backgroundColor={backgroundColor}
      boxShadow={mine ? 'inset 3px 0 var(--chakra-colors-green-400)' : undefined}
      onMouseEnter={() => onMouseEnter(party.id)}
      onMouseLeave={() => onMouseLeave(party.id)}
      onClick={() => {
        console.log('Participant clicked:', party.id);
        onMatchClick && onMatchClick(match.id)}
      }
      style={{ cursor: 'pointer' }}
      //onPartyClick={onPartyClick}
      //onMatchClick={onMatchClick}
    >
      <Grid
        w="100%"
        templateAreas={`"image team score"
                  "image coach score"`}
        gridTemplateColumns={compact ? `28px minmax(0, 1fr) ${match.seriesLength > 1 ? '58px' : '24px'}` : '40px minmax(0, 1fr) 32px'}
        fontSize={compact ? 'xs' : 'md'}
      >
        <GridItem pl="4px" pr="4px" area="image" textAlign="center">
          <Center w="100%" h="100%">
            <Image
              src={imageUrls.logo(party.picture, party?.id.opus)}
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
          overflow="hidden"
          textOverflow="ellipsis"
          whiteSpace="nowrap"
        >
          <Box>{party.teamName || 'To be defined'}{mine ? ' ★' : ''}</Box>
        </GridItem>
        <GridItem pl="4px" area="coach" textAlign="left" fontSize={compact ? '2xs' : 'sm'} color="grey" overflow="hidden" textOverflow="ellipsis" whiteSpace="nowrap">
          {details}
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

export function MatchComponent({
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
  computedStyles,
}) {
  return (
    <DelayedIconTooltip label={`${match.replayAvailable ? 'Replay saved · ' : ''}${match.seriesLength > 1 ? `${match.seriesLength} matches (${match.replayCount} ${match.replayCount === 1 ? 'replay' : 'replays'})` : match.state === 'DONE' ? `Played ${topText}` : 'Scheduled'}`}>
      <div
        style={{
          cursor: 'pointer',
          marginTop: match.compact ? 0 : '4px',
          width: match.compact ? `${computedStyles?.width || 240}px` : undefined,
          height: match.compact ? `${computedStyles?.boxHeight || 86}px` : undefined,
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
          compact={match.compact}
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
          compact={match.compact}
        />
      </div>
    </DelayedIconTooltip>
  );
}

function KnockoutCompetition({ competition, competitionLoading }) {
  const { fetchContestsWithMatches, contests, contestsLoading, error: contestError } = useFetchContestsWithMatches();
  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();  
  const [matches, setMatches] = useState([]);

  // Modal state
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [selectedContest, setSelectedContest] = useState(null);

  useEffect(() => {
    if (competition) {
      fetchContestsWithMatches(competition);
      fetchRanks(competition);
    }
  }, [competition]);

  useEffect(() => {
    if (!competitionLoading && competition && !contestsLoading && contests) {
      // Sort contests by round (earlier rounds first)
      const sortedContests = [...contests].sort((a, b) => a.round - b.round);
      const bracketMatches = toBracketMatches(sortedContests);
      setMatches(bracketMatches);
      console.log('Matches updated:', bracketMatches);
    } else {
      setMatches([]);
    }
  }, [competition, competitionLoading, contests, contestsLoading]);

  /*
  useEffect(() => {
    if (contest && !contest.match && contest.matchId) {
      // fetch match and set it in local state
    }
  }, [contest]);
  */
  function getNextContest(contest, sortedContests) {
    if (contest === null || !contest.winner) {
      return null;
    }
    
    // Look for a contest in the next round where this winner participates
    const nextRound = contest.round + 1;
    
    for (let i = 0; i < sortedContests.length; i++) {
      const nextContest = sortedContests[i];
      
      // Skip if it's not the next round
      if (nextContest.round !== nextRound) {
        continue;
      }
      
      // Check if the winner of current contest participates in this next contest
      if (nextContest.opponents && nextContest.opponents.length > 0) {
        const winnerTeamId = contest.winner.team.id.toString();
        const participatesInNext = nextContest.opponents.some(opponent => 
          opponent && opponent.id && opponent.id.value === winnerTeamId
        );
        
        if (participatesInNext) {
          return nextContest;
        }
      }
    }
  
    console.log('No next contest found for', contest);
    return null;
  }

  function toBracketMatch(contest, index, sortedContests) {
    const nextContest = getNextContest(contest, sortedContests);
    
    return {
      id: contest?.id?.value || contest?.id,
      nextMatchId: contest?.nextContestId?.value || nextContest?.id?.value || null,
      participants: toParticipants(contest?.opponents, contest?.winner),
      startTime: formatter.formatAsDate(contest?.matchDate, '-'),
      state: contest?.status === 'Validated' || contest?.matchDate ? 'DONE' : null,
      tournamentRoundText: `${contest?.round}`,
    };
  }

  function toBracketMatches(sortedContests) {
    const matches = [];

    if (sortedContests) {
      // Process contests in round order
      sortedContests.forEach((contest, i) => {
        const match = toBracketMatch(contest, i, sortedContests);
        matches.push(match);
      });
    }
    return matches;
  }

  // Pass this handler to the bracket
  function handleMatchClick(matchId) {
    // Find the contest by matchId (or id, depending on your data)
    const contest = contests.find(c => identityUtils.value(c.id) === matchId);
    console.log('Contest found:', contest);

    if (contest) {
      setSelectedContest(contest);
      onOpen();
    }
  }

  // Update MatchComponent to use this handler
  function MatchComponentWithModal(props) {
    return (
      <MatchComponent
        {...props}
        onMatchClick={handleMatchClick}
      />
    );
  }

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
              <SingleEliminationBracket
                matches={matches}
                matchComponent={MatchComponentWithModal}
              />
            ) : (
              <HStack gap="1rem">
                <Icon as={FaRegFaceSadTear} boxSize={boxSize} />
                <Box>No matches yet...</Box>
              </HStack>
            )}
          </LoadingOrErrorWrapper>
          {/* Render the modal here */}
          {selectedContest && (
            <MatchModalWithRosters
              isOpen={isOpen}
              onClose={onClose}
              contest={selectedContest}
            />
          )}
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
            competitionId={competition?.id}
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
