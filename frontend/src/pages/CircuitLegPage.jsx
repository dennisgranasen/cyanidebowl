import React, { useEffect, useState } from 'react';
import { Box, Heading, Spinner, useMediaQuery, VStack } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import Contests from '../components/contest/Contests';
import CircuitLeg from '../components/circuit/CircuitLeg';
import comparators from '../util/Comparators';
import ImageUrls from '../ImageUrls';
import Ranks from '../components/competition/Ranks';
import prettyPrint from '../util/PrettyPrint';
import CompetitionProgress from '../components/competition/CompetitionProgress';
import InfoArea from '../components/common/InfoArea';
import InfoItem from '../components/common/InfoItem';
import HeaderCard from '../components/common/HeaderCard';
import formatter from '../util/Formatter';

function CircuitLegPage() {
  const [smallscreen] = useMediaQuery('(max-width: 768px)');
  const {circuitId, competitionId } = useParams();
  const [circuit, setCircuit] = useState();
  const [circuitLeg, setCircuitLeg] = useState();
  const [contests, setContests] = useState();

  useEffect(() => {
    const fetchCircuit = () => {
      WarpScoresApiService.circuit(circuitId).then((data) => {
        setCircuit(data);
        const comp = data.circuitLegs.find((cc) => cc.circuitLegId == legId);
        comp && setCircuitLeg(comp);
      });
    };
   
    fetchCircuit();
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="circuitLeg"
        />
      </Box>
      {circuitLeg ? (
        <>
          <HeaderCard
            heading={circuitLeg.label}
            subHeading={<RouteLink to={`/${circuitLeg.circuitLegId}`}>Circuit: {circuit.name}</RouteLink>}
            detailsHeading="Circuit Leg details"
            smallscreen={smallscreen ? 'smallscreen' : undefined}
          >
            {
            /*
            <InfoArea
              infoItems={[
                <InfoItem key="Created" label="Created" info={formatter.formatAsDate(competition.dateCreated)} />,
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
                <InfoItem key="Teams" label="Teams" info={formatter.formatAsNumber(competition.teamsMax)} />,
                <InfoItem
                  key="TimeSettings"
                  label="Time settings"
                  info={`Turn: ${formatter.formatAsNumber(competition.turnDuration / 60)}m`}
                  additionalInfo={`Bonus: ${formatter.formatAsNumber(competition.timeBonusDuration / 60)}m`}
                />,
              ]}
            />
            */}
          </HeaderCard>          
          <Heading size="md">Ranking</Heading>
          {/*
          {ranks ? <Ranks smallscreen={smallscreen ? 'smallscreen' : undefined} ranks={ranks} /> : <Spinner />}
          */}
          <Heading size="md">Legs</Heading>
          {circuitLeg ? (
            <CircuitLeg
              smallscreen={smallscreen ? 'smallscreen' : undefined}
              circuitLeg={circuitLeg}
            />
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

export default CircuitLegPage;
