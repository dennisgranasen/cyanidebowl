import { Box, Center, Grid, GridItem, Heading, Image, SimpleGrid, Text } from '@chakra-ui/react';
import React from 'react';
import ContestMatchCard from './ContestMatchCard';
import ImageUrls from '../ImageUrls';
import formatter from '../util/Formatter';

function TeamAndCoach({ teamName, coachName }) {
  return (
    <Box>
      <Heading size="sm">{teamName}</Heading>
      <Text color="grey">{coachName}</Text>
    </Box>
  );
}

function ContestMatchCards({ contests, noContentIcon, noContentHeading, noContentText }) {
  return contests ? (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="20px">
      {contests.map((contest) => {
        const started = contest.match ? contest.match.started : contest.matchDate;
        const finished = contest.match && !contest.live ? contest.match.finished : new Date();

        return (
          <ContestMatchCard key={contest.contestUuid}>
            <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
              <GridItem colSpan={8}>
                <Center color="grey">{contest.competitionName}</Center>
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
                <Center h="100%">
                  <Heading size="md">{`${contest.opponents[0].score} - ${contest.opponents[1].score}`}</Heading>
                </Center>
              </GridItem>
              <GridItem colSpan={3} align="center">
                <Center h="100%">
                  <Image objectFit="contain" maxW="64px" src={ImageUrls.logo(contest.opponents[1].logo)} />
                </Center>
              </GridItem>
              <GridItem colSpan={8}>
                <Center color="grey">
                  {`Started: ${formatter.formatAsDate(started)}, ${
                    contest.live ? 'Live since:' : 'Duration:'
                  } ${formatter.formatAsDuration(started, finished)}`}
                </Center>
              </GridItem>
            </Grid>
          </ContestMatchCard>
        );
      })}
    </SimpleGrid>
  ) : (
    <ContestMatchCard optionalIcon={noContentIcon} optionalHeading={noContentHeading}>
      <Text>{noContentText}</Text>
    </ContestMatchCard>
  );
}

export default ContestMatchCards;
