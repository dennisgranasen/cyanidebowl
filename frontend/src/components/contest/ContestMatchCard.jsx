import React, { useState, useEffect } from 'react';
import { Box, Card, CardBody, Center, Grid, GridItem, Heading, Image, Spinner, Text, useDisclosure } from '@chakra-ui/react';
import { Icon, QuestionOutlineIcon } from '@chakra-ui/icons';
import imageUrls from '../../imageUrls';
import formatter from '../../util/formatter';
import config from '../../config';
import ScoreOrIcon from './ScoreOrIcon';
import prettyPrint from '../../util/prettyPrint';
import { toRace, getRaceLogo } from '../../util/raceUtil';
import { identityUtils } from '../../util/identityUtil';
import WarpScoresApiService from '../../WarpScoresApiService';
import MatchModal from './MatchModalWithRosters'; // Import the modal component
const { boxSize } = config;

function TeamAndCoach({ teamName, coachName, race, reverse }) {
  return (
    <Box>
      <Heading size="sm">{teamName}</Heading>
      <Text color="grey">
        {reverse ? `(${coachName}) ${prettyPrint(race)}` : `${prettyPrint(race)} (${coachName})`}
      </Text>
    </Box>
  );
}

function ContestMatchCard({ contestOrMatch, contestHeader, noContentIcon, noContentHeading, noContentText, variant }) {
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [contestLoading, setContestLoading] = useState(true);
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [contest, setContest] = useState(contestOrMatch);

  let started;
  let finished;
  let coaches;

  useEffect(() => {
    /*
    if (contestOrMatch && contestOrMatch.gameId) { // this is a contest
      setContest(contestOrMatch);
      setContestLoading(false);
    } else if (contestOrMatch && !contestOrMatch.gameId) { // this is a match, fetch the contest
      
      let newId = identityUtils.combine(contestOrMatch.leagueId, contestOrMatch.competitionId)
      console.log("com", contestOrMatch);
      console.log("ContestMatchCard useEffect for contestOrMatch", contestOrMatch, "with competition id", newId);
      WarpScoresApiService.competitionContests(newId)      
        .then((contests) => {
          // Find the contest that matches the current contestOrMatch
          console.log("Fetched contests for competition:", newId, contests);
          const c = contests.find(c => c.gameId && c.gameId.key === contestOrMatch.id.key);
          if (c && c.status === 'Validated' && !c.adminResult) {
            console.log("Opening match modal for contest", c);
            setContest(c);
          } else {  
            console.log("Freshly loaded contest is not valid for opening modal", c);
          }
          setContestLoading(false);
        })
        .catch((error) => { 
          console.error("Error fetching contests for competition:", error);
          setContestLoading(false);
        });
      }*/

        setContest(contestOrMatch);
        setContestLoading(false);
      }, [contestOrMatch]);

  useEffect(() => {
    let opus = contestOrMatch?.id ? identityUtils.opus(contestOrMatch?.id) : 3;
    if (contestOrMatch?.contestId) {
      setTeams(contestOrMatch.opponents);
    } else if (contestOrMatch?.matchId) {
      const teamsData = [...contestOrMatch.teams];
      setTeams(teamsData);
      
      // Fetch missing logos
      teamsData.forEach((team, index) => {
        if ((team.logo === null || team.logo === undefined) && team.teamId) {
          console.log(`Fetching logo for team ${team.id} with raceId ${team.raceId} and opus ${opus}`);
          WarpScoresApiService.team(team.id)
            .then((fetchedTeam) => {
              if (!fetchedTeam) {
                setTeams(prevTeams => {
                  const updatedTeams = [...prevTeams];
                  updatedTeams[index] = { ...updatedTeams[index], logo: getRaceLogo(team.raceId, opus) };
                  return updatedTeams;
                });
              } else {
                setTeams(prevTeams => {
                  const updatedTeams = [...prevTeams];
                  updatedTeams[index] = { ...updatedTeams[index], logo: fetchedTeam.logo };
                  return updatedTeams;
                });
              }
            })
            .catch((error) => {
              console.error(`Error fetching team ${team.id}:`, error);
              setTeams(prevTeams => {
                const updatedTeams = [...prevTeams];
                updatedTeams[index] = { ...updatedTeams[index], logo: getRaceLogo(team.raceId, opus) };
                return updatedTeams;
              });
            });
          }
      });
      setLoading(false);
    }
  }, [contestOrMatch]);

  const handleCardClick = () => {
    // Only open modal if we have a valid match with status 'Validated'
    if (contest) {
      if (contest.status === 'Validated' && !contest.adminResult) {
        onOpen();
      }
    } else if (identityUtils.opus(contestOrMatch.id) === 1 || ((contestOrMatch.status === 'Validated' && !contestOrMatch.adminResult))) 
    {
      console.log("Opening match modal for contest", contestOrMatch);
      onOpen();
    }
    else {
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
  
  let opus = contestOrMatch?.id ? identityUtils.opus(contestOrMatch?.id) : 3;
  
  // Determine if the card should be clickable
  const isClickable = contestOrMatch && contestOrMatch.status === 'Validated';
  
  return ( loading || contestLoading) ? <Spinner/> :
    (<>
      <Card 
        direction="row" 
        overflow="hidden" 
        variant={variant} 
        align="center"
        cursor={isClickable ? "pointer" : "default"}
        onClick={handleCardClick}
        _hover={isClickable ? { 
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
            {loading || !contestOrMatch && noContentHeading && <Heading size="md">{noContentHeading}</Heading>}
            {!loading && contestOrMatch ? (
              <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
                <GridItem colSpan={8}>
                  <Center color="grey">{contestHeader}</Center>
                </GridItem>
                <GridItem colSpan={4}>
                  <TeamAndCoach
                    teamName={teams[0]?.name}
                    coachName={coaches[0].coachName || coaches[0].name}
                    race={teams[0].race || toRace(teams[0].raceId, opus)}
                  />
                </GridItem>
                <GridItem colSpan={4} align="right">
                  <TeamAndCoach
                    teamName={teams[1].name}
                    coachName={coaches[1].coachName || coaches[1].name}
                    race={teams[1].race || toRace(teams[1].raceId, opus)}
                    reverse
                  />
                </GridItem>
                <GridItem colSpan={3}>
                  <Center>
                    <Image
                      objectFit="contain"
                      maxW="64px"
                      src={imageUrls.logo(teams[0].logo, opus)}
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
                      src={imageUrls.logo(teams[1].logo || getRaceLogo(teams[1].race, opus), opus)}
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
                  {isClickable && (
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
      <MatchModal isOpen={isOpen} onClose={onClose} contest={contest} />
    </>
  );
}

export default ContestMatchCard;
