import React from 'react';
import { Box, Card, CardBody, Center, Grid, GridItem, Heading, Image, Text } from '@chakra-ui/react';
import { Icon, QuestionOutlineIcon } from '@chakra-ui/icons';
import imageUrls from '../../imageUrls';
import formatter from '../../util/formatter';
import config from '../../config';
import ScoreOrIcon from './ScoreOrIcon';
import prettyPrint from '../../util/prettyPrint';

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
  let started;
  let finished;
  let teams;
  let coaches;
  if (contestOrMatch?.contestUuid) {
    started = contestOrMatch.match ? contestOrMatch.match.started : contestOrMatch.matchDate;
    finished = contestOrMatch.match && !contestOrMatch.live ? contestOrMatch.match.finished : null;
    teams = contestOrMatch.opponents;
    coaches = contestOrMatch.opponents;
  } else if (contestOrMatch?.matchId) {
    started = contestOrMatch.started;
    finished = contestOrMatch.finished;
    teams = contestOrMatch.teams;
    coaches = contestOrMatch.coaches;
  }
  return (
    <Card direction="row" overflow="hidden" variant={variant} align="center">
      {!contestOrMatch && noContentIcon && (
        <Center p="2">
          <Icon as={noContentIcon} boxSize="4em" />
        </Center>
      )}
      <CardBody p={2}>
        <Box w="100%">
          {!contestOrMatch && noContentHeading && <Heading size="md">{noContentHeading}</Heading>}
          {contestOrMatch ? (
            <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
              <GridItem colSpan={8}>
                <Center color="grey">{contestHeader}</Center>
              </GridItem>
              <GridItem colSpan={4}>
                <TeamAndCoach
                  teamName={teams[0]?.name}
                  coachName={coaches[0].coachName || coaches[0].name}
                  race={teams[0].race}
                />
              </GridItem>
              <GridItem colSpan={4} align="right">
                <TeamAndCoach
                  teamName={teams[1].name}
                  coachName={coaches[1].coachName || coaches[1].name}
                  race={teams[1].race}
                  reverse
                />
              </GridItem>
              <GridItem colSpan={3}>
                <Center>
                  <Image
                    objectFit="contain"
                    maxW="64px"
                    src={imageUrls.logo(teams[0].logo)}
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
                    src={imageUrls.logo(teams[1].logo)}
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
              </GridItem>
            </Grid>
          ) : (
            <Text>{noContentText}</Text>
          )}
        </Box>
      </CardBody>
    </Card>
  );
}

export default ContestMatchCard;
