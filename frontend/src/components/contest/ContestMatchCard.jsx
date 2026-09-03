import React, { useState, useEffect } from 'react';
import { Box, Card, CardBody, Center, Grid, GridItem, Heading, Image, Spinner, Text, useDisclosure } from '@chakra-ui/react';
import { Icon, QuestionOutlineIcon } from '@chakra-ui/icons';
import imageUrls from '../../imageUrls';
import formatter from '../../util/formatter';
import config from '../../config';
import ScoreOrIcon from './ScoreOrIcon';
import prettyPrint from '../../util/prettyPrint';
import { resolveRace, getRaceLogo } from '../../util/raceUtil';
import { identityUtils } from '../../util/identityUtil';
import WarpScoresApiService from '../../WarpScoresApiService';
import MatchModal from './MatchModalWithRosters'; // Import the modal component
import { useMyTeams } from '../../context/MyTeamsContext';
const { boxSize } = config;

function TeamAndCoach({ teamId, teamName, coachName, race, reverse }) {
  const { isMyTeam } = useMyTeams();
  const mine = isMyTeam(teamId);
  return (
    <Box>
      <Heading size="sm">{teamName}{mine ? ' ★' : ''}</Heading>
      <Text color="grey">
        {reverse ? `(${coachName}) ${prettyPrint(race)}` : `${prettyPrint(race)} (${coachName})`}
      </Text>
    </Box>
  );
}

function ContestMatchCard({ contestOrMatch, contestHeader, noContentIcon, noContentHeading, noContentText, variant, clickable }) {
  const [teams, setTeams] = useState([]);
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [contest, setContest] = useState(contestOrMatch);

  let started;
  let finished;
  let coaches;

  useEffect(() => {
      if (contestOrMatch) {
        if (contestOrMatch.matchId) {
          setContest(contestOrMatch);
          setTeams(contestOrMatch.teams || []);
        } else if (identityUtils.opus(contestOrMatch.id) > 1) {
          setContest(contestOrMatch);
          setTeams(contestOrMatch.opponents);
        } else {

          // For opus 1, we need to fetch the team details
          const ids = contestOrMatch.teams.map(team => team.id);
          WarpScoresApiService.teams(ids)
            .then((fetchedTeams) => {
                const teamsWithLogos = contestOrMatch.teams.map(team => {
                  const fetched = fetchedTeams.find(ft => identityUtils.key(ft.id) === identityUtils.key(team.id));
                  return fetched ? { ...team, logo: fetched.logo } : team;
                });
                setTeams(teamsWithLogos);
            })
            .catch((error) => {
              console.warn("Error fetching teams for contestOrMatch", contestOrMatch, error);
              setTeams(contestOrMatch.teams);
            })
            .finally(() => {              
              //setContest(contestOrMatch);
            });
        }
      }
    }, [contestOrMatch]);

  function getIsClickable() {
    // Use the current contest state, not the original prop!
    if (!contest || !contest.status) {
      if (contestOrMatch && (identityUtils.opus(contestOrMatch?.id) === 1))
        return false; // This can be clickable, but need to change the modal
      return false;
    }
    return clickable && contest.status === 'Validated';
  }

  const handleCardClick = () => {
    // Only open modal if we have a valid match with status 'Validated'
    if (!getIsClickable())
      return;
    if (contest.match === null){ 
      WarpScoresApiService.match(contest.gameId)
        .then((match) => {
          if (match) {
            setContest(prevContest => ({ ...prevContest, match }));
            onOpen();
          } else {
            console.warn("No match found for contest", contest);
            setContest(prevContest => ({ ...prevContest, match: undefined }));
          }
        })
        .catch((error) => {
          console.warn("Error fetching match for contest", contest, error);
        });
    } else {
      onOpen();
    }
  };

  if (contestOrMatch?.contestId) {
    started = contestOrMatch.match ? contestOrMatch.match.started : contestOrMatch.matchDate;
    finished = contestOrMatch.match && !contestOrMatch.live ? contestOrMatch.match.finished : null;
    coaches = contestOrMatch.opponents;
  } else if (contestOrMatch?.matchId) {
    started = contestOrMatch.started;
    finished = contestOrMatch.finished;
    coaches = contestOrMatch.coaches;
  }
  
  const opusIdentity = contestOrMatch?.id
    || contestOrMatch?.matchResourceId
    || contestOrMatch?.matchId
    || teams[0]?.id;
  const opus = identityUtils.opus(opusIdentity);
  const firstRace = resolveRace(teams[0], opus);
  const secondRace = resolveRace(teams[1], opus);
  // Determine if the card should be clickable
  
  return (
    <>
      <Card 
        direction="row" 
        overflow="hidden" 
        variant={variant} 
        align="center"
        cursor={getIsClickable() ? "pointer" : "default"}
        onClick={handleCardClick}
        _hover={getIsClickable() ? { 
          transform: "scale(1.02)", 
          transition: "transform 0.2s",
          boxShadow: "lg" 
        } : {}}
      >
        {!contestOrMatch && noContentIcon && (
          <Center p="2">
            <Icon as={noContentIcon} boxSize="4em" />
          </Center>
        )}
        <CardBody p={2}>
          <Box w="100%">
            {!contestOrMatch && noContentHeading && <Heading size="md">{noContentHeading}</Heading>}
            {contestOrMatch && teams && teams.length > 0? (
              <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
                <GridItem colSpan={8}>
                  <Center color="grey">{contestHeader}</Center>
                </GridItem>
                <GridItem colSpan={4}>
                  <TeamAndCoach
                    teamId={teams[0]?.id}
                    teamName={teams[0]?.name}
                    coachName={coaches[0].coachName || coaches[0].name}
                    race={firstRace}
                  />
                </GridItem>
                <GridItem colSpan={4} align="right">
                  <TeamAndCoach
                    teamId={teams[1]?.id}
                    teamName={teams[1].name}
                    coachName={coaches[1].coachName || coaches[1].name}
                    race={secondRace}
                    reverse
                  />
                </GridItem>
                <GridItem colSpan={3}>
                  <Center>
                    <Image
                      objectFit="contain"
                      maxW="64px"
                      src={imageUrls.logo(teams[0].logo || getRaceLogo(teams[0].raceId ?? firstRace, opus), opus)}
                      fallback={<QuestionOutlineIcon boxSize={boxSize} />}
                    />
                  </Center>
                </GridItem>
                <GridItem colSpan={2}>
                  <ScoreOrIcon contestOrMatch={contestOrMatch} size="lg" boxSize={boxSize} />
                </GridItem>
                <GridItem colSpan={3} align="center">
                  <Center h="100%">
                    <Image
                      objectFit="contain"
                      maxW="64px"
                      src={imageUrls.logo(teams[1].logo || getRaceLogo(teams[1].raceId ?? secondRace, opus), opus)}
                      fallback={<QuestionOutlineIcon boxSize={boxSize} />}
                    />
                  </Center>
                </GridItem>
                <GridItem colSpan={8}>
                  <Center color="grey">{`Started: ${formatter.formatAsDate(started, '-')}`}</Center>
                  <Center color="grey">
                    {`${contestOrMatch.live ? 'Live since:' : 'Duration:'} ${formatter.formatAsDuration(
                      started,
                      finished
                    )}`}
                  </Center>
                  {getIsClickable() && (
                    <Center color="blue.500" fontSize="sm" mt={1}>
                      Click for detailed stats
                    </Center>
                  )}
                </GridItem>
              </Grid>
            ) : (
              <Text>{noContentText}</Text>
            )}
          </Box>
        </CardBody>
      </Card>
      {getIsClickable() && <MatchModal isOpen={isOpen} onClose={onClose} contest={contest} />}
    </>
);}

export default ContestMatchCard;
