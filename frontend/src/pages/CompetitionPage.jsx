import React, { useEffect, useState } from 'react';
import { Box, Button, Spinner, VStack } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import imageUrls from '../imageUrls';
import prettyPrint from '../util/prettyPrint';
import CompetitionProgress from '../components/competition/CompetitionProgress';
import InfoArea from '../components/common/InfoArea';
import InfoItem from '../components/common/InfoItem';
import HeaderCard from '../components/common/HeaderCard';
import formatter from '../util/formatter';
import RoundRobinAndWissenCompetition from '../components/competition/RoundRobinAndWissenCompetition';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import NafExportButton from '../components/misc/NafExportButton';
import KnockoutCompetition from '../components/competition/KnockoutCompetition';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import ArenaCompetition from '../components/competition/ArenaCompetition';
import LadderCompetition from '../components/competition/LadderCompetition';

const getTeamsFor = (competition) => {
  if (!competition) return null;

  switch (competition.format) {
    case 'Arena':
      return null;
    default:
      return competition?.teamsMax;
  }
};

function TypedCompetition({ competition, competitionLoading }) {
  switch (competition?.format) {
    case 'Knockout':
      return <KnockoutCompetition competition={competition} competitionLoading={competitionLoading} />;
    case 'Arena':
      return <ArenaCompetition competition={competition} competitionLoading={competitionLoading} />;
    case 'Ladder':
      return <LadderCompetition competition={competition} competitionLoading={competitionLoading} />;
    case 'RoundRobin':
    case 'Wissen':
      return <RoundRobinAndWissenCompetition competition={competition} competitionLoading={competitionLoading} />;
    default:
      return competitionLoading ? <Spinner /> : null;
  }
}

function CompetitionPage() {
  const {
    authenticationReady,
    isAuthenticated,
    checkPermissions,
    userPermissions,
    getAccessTokenSilently,
    getAccessTokenWithPopup,
  } = useAuth0WithUserPermissions();
  const { competitionUuid } = useParams();
  const [competition, setCompetition] = useState(null);
  const [competitionLoading, setCompetitionLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchCompetition = () => {
      setCompetitionLoading(true);
      WarpScoresApiService.competition(competitionUuid)
        .then((data) => {
          setCompetition(data);
        })
        .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setCompetitionLoading(false));
    };

    fetchCompetition();
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
      <LoadingOrErrorWrapper loading={competitionLoading} error={error}>
        <HeaderCard
          heading={competition?.name}
          subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}
          detailsHeading="Competition details"
          mainImageSrc={competition?.logo ? imageUrls.logo(competition?.logo) : imageUrls.logo(competition?.leagueLogo)}
          additionalImageSrc={competition?.logo ? imageUrls.logo(competition?.leagueLogo) : null}
        >
          <InfoArea>
            <InfoItem key="Created" label="Created" info={formatter.formatAsDate(competition?.dateCreated)} />
            <InfoItem key="Format" label="Format" info={prettyPrint(competition?.format)} />
            <InfoItem
              key="Progress"
              label="Progress"
              info={
                <CompetitionProgress
                  status={competition?.status}
                  format={competition?.format}
                  currentRound={competition?.currentRound}
                  totalRounds={competition?.totalRounds}
                  totalMatches={competition?.totalMatches}
                  playedMatches={competition?.playedMatches}
                  notValidatedMatches={competition?.notValidatedMatches}
                  liveMatches={competition?.liveMatches}
                  withPadding
                />
              }
            />
            <InfoItem key="Teams" label="Teams" info={formatter.formatAsNumber(getTeamsFor(competition))} />
            <InfoItem
              key="TimeSettings"
              label="Time settings"
              info={`Turn: ${formatter.formatAsNumber((competition?.turnDuration ?? 0) / 60)}m`}
              additionalInfo={`Bonus: ${formatter.formatAsNumber((competition?.timeBonusDuration ?? 0) / 60)}m`}
            />
            {competition?.status === 'Finished' &&
              (!checkPermissions || (authenticationReady && userPermissions.writeLeagueAdmin)) && (
                <InfoItem
                  key="NafDataExport"
                  label="Export"
                  info={
                    <NafExportButton
                      authenticationReady={authenticationReady}
                      checkPermissions={checkPermissions}
                      isAuthenticated={isAuthenticated}
                      getAccessTokenSilently={getAccessTokenSilently}
                      getAccessTokenWithPopup={getAccessTokenWithPopup}
                      competitionUuid={competition?.uuid}
                    />
                  }
                />
              )}
          </InfoArea>
          <RouteLink to={`/competition/${competition?.uuid}/stats`}>
            <Button size="xs">Statistics</Button>
          </RouteLink>
        </HeaderCard>
      </LoadingOrErrorWrapper>
      <LoadingOrErrorWrapper loading={competitionLoading} error={error}>
        <TypedCompetition competition={competition} competitionLoading={competitionLoading} />
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CompetitionPage;
