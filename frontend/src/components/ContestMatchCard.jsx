import React from 'react';
import { Box, Card, CardBody, Center, Grid, GridItem, Heading, Image, Progress, Text, VStack } from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import { FaTowerBroadcast } from 'react-icons/fa6';
import ImageUrls from '../ImageUrls';
import formatter from '../util/Formatter';
import config from '../config';
import ScoreOrIcon from './ScoreOrIcon';

const { boxSize } = config;

function TeamAndCoach({ teamName, coachName }) {
  return (
    <Box>
      <Heading size="sm">{teamName}</Heading>
      <Text color="grey">{coachName}</Text>
    </Box>
  );
}

function ContestMatchCard({ contest, contestHeader, noContentIcon, noContentHeading, noContentText, variant }) {
  let started;
  let finished;
  if (contest) {
    started = contest.match ? contest.match.started : contest.matchDate;
    finished = contest.match && !contest.live ? contest.match.finished : null;
  }
  return (
    <Card direction="row" overflow="hidden" variant={variant} align="center">
      {!contest && noContentIcon && (
        <Center p="2">
          <Icon as={noContentIcon} boxSize="4em" />
        </Center>
      )}
      <CardBody p={2}>
        <Box w="100%">
          {!contest && noContentHeading && <Heading size="md">{noContentHeading}</Heading>}
          {contest ? (
            <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
              <GridItem colSpan={8}>
                <Center color="grey">{contestHeader}</Center>
              </GridItem>
              <GridItem colSpan={4}>
                <TeamAndCoach teamName={contest.opponents[0].name} coachName={contest.opponents[0].coachName} />
              </GridItem>
              <GridItem colSpan={4} align="right">
                <TeamAndCoach teamName={contest.opponents[1].name} coachName={contest.opponents[1].coachName} />
              </GridItem>
              <GridItem colSpan={3}>
                <Center>
                  <Image objectFit="contain" maxW="64px" src={ImageUrls.logo(contest.opponents[0].logo)} />
                </Center>
              </GridItem>
              <GridItem colSpan={2}>
                <ScoreOrIcon contest={contest} size="lg" boxSize={boxSize} />
              </GridItem>
              <GridItem colSpan={3} align="center">
                <Center h="100%">
                  <Image objectFit="contain" maxW="64px" src={ImageUrls.logo(contest.opponents[1].logo)} />
                </Center>
              </GridItem>
              <GridItem colSpan={8}>
                <Center color="grey">{`Started: ${formatter.formatAsDate(started)}`}</Center>
                <Center color="grey">
                  {`${contest.live ? 'Live since:' : 'Duration:'} ${formatter.formatAsDuration(started, finished)}`}
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
