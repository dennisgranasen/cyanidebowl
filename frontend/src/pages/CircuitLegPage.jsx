import React, { useEffect, useState } from 'react';
import { Box, Heading, VStack } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import CircuitLeg from '../components/circuit/CircuitLeg';
import HeaderCard from '../components/common/HeaderCard';
import config from '../config';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function CircuitLegPage() {
  const { circuitId, legId } = useParams();
  const [circuit, setCircuit] = useState();
  const [circuitLeg, setCircuitLeg] = useState();
  const [error, setError] = useState();

  useEffect(() => {
    const leg = circuit.circuitLegs.find((data) => data.circuitLegId === legId);
    setCircuitLeg(leg);
  }, [circuit]);

  useEffect(() => {
    const fetchCircuit = () => {
      WarpScoresApiService.circuits(circuitId)
        .then((data) => {
          setCircuit(data);
        })
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString(config.locale) });
        });
    };

    fetchCircuit();
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="circuitLeg" />
      </Box>
      <LoadingOrErrorWrapper loading={!circuitLeg} error={error}>
        <HeaderCard
          heading={circuitLeg.label}
          subHeading={<RouteLink to={`/${circuitLeg.circuitLegId}`}>Circuit: {circuit.name}</RouteLink>}
          detailsHeading="Circuit Leg details"
        >
          {/*
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
        <CircuitLeg circuitLeg={circuitLeg} />
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CircuitLegPage;
