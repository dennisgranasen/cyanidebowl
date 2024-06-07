import React, { useEffect, useState } from 'react';
import { Box, Heading, Spinner, useMediaQuery, VStack } from '@chakra-ui/react';
import { useParams, Link as RouteLink } from 'react-router-dom';
import CyanideApiService from '../CyanideApiService';
import Navigation from '../components/Navigation';
import Formatter from '../util/Formatter';
import Contests from '../components/Contests';
import comparators from '../util/Comparators';
import ImageUrls from '../ImageUrls';
import Ranks from '../components/Ranks';
import prettyPrint from '../util/PrettyPrint';
import CompetitionProgress from '../components/CompetitionProgress';
import InfoArea from '../components/InfoArea';
import InfoItem from '../components/InfoItem';
import HeaderCard from '../components/HeaderCard';

function CompetitionPage() {
  const [smallscreen] = useMediaQuery('(max-width: 768px)');
  const { competitionUuid } = useParams();
  const [competition, setCompetition] = useState();
  const [ranks, setRanks] = useState();
  const [contests, setContests] = useState();

  useEffect(() => {
    const fetchCompetition = () => {
      CyanideApiService.competition(competitionUuid).then((data) => {
        setCompetition(data);
      });
    };
    const fetchTeams = () => {
      CyanideApiService.competitionRanks(competitionUuid).then((data) => {
        data.sort((rankA, rankB) => rankA.rank - rankB.rank);
        setRanks(data);
      });
    };

    const fetchContests = () => {
      CyanideApiService.competitionContests(competitionUuid).then((data) => {
        data.sort((compA, compB) => comparators.compareAsDates(compA.matchDate, compB.matchDate));
        setContests(data);
      });
    };

    fetchCompetition();
    fetchTeams();
    fetchContests();
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="competition"
          league={competition ? [competition.leagueId, competition.leagueName] : []}
          competition={[competitionUuid, competition ? competition.name : '']}
        />
      </Box>
      {competition ? (
        <>
          <HeaderCard
            heading={competition.name}
            subHeading={<RouteLink to={`/${competition.leagueId}`}>League: {competition.leagueName}</RouteLink>}
            detailsHeading="Competition details"
            mainImageSrc={ImageUrls.logo(competition.leagueLogo)}
            additionalImageSrc={ImageUrls.logo(competition.logo)}
            smallscreen={smallscreen ? 'smallscreen' : undefined}
          >
            <InfoArea
              infoItems={[
                <InfoItem key="Created" label="Created" info={Formatter.formatAsDate(competition.dateCreated)} />,
                <InfoItem key="Format" label="Format" info={prettyPrint(competition.format)} />,
                <InfoItem
                  key="Progress"
                  label="Progress"
                  info={
                    <CompetitionProgress
                      teamsMax={competition.teamsMax}
                      status={competition.status}
                      format={competition.format}
                      currentRound={competition.currentRound}
                      totalRounds={competition.totalRounds}
                      totalMatches={competition.totalMatches}
                      playedMatches={competition.playedMatches}
                      validatedMatches={competition.validatedMatches}
                      liveMatches={competition.liveMatches}
                    />
                  }
                />,
                <InfoItem key="Teams" label="Teams" info={Formatter.formatAsNumber(competition.teamsMax)} />,
                <InfoItem
                  key="TimeSettings"
                  label="Time settings"
                  info={`Turn: ${Formatter.formatAsNumber(competition.turnDuration / 60)}m`}
                  additionalInfo={`Bonus: ${Formatter.formatAsNumber(competition.timeBonusDuration / 60)}m`}
                />,
              ]}
            />
          </HeaderCard>
          <Heading size="md">Ranking</Heading>
          {ranks ? <Ranks smallscreen={smallscreen ? 'smallscreen' : undefined} ranks={ranks} /> : <Spinner />}
          <Heading size="md">Contests</Heading>
          {contests ? (
            <Contests smallscreen={smallscreen ? 'smallscreen' : undefined} contests={contests} currentRound={competition.currentRound} />
          ) : (
            <Spinner />
          )}
        </>
      ) : (
        <Spinner />
      )}
    </VStack>
  );
}

export default CompetitionPage;
