import React, { useEffect, useState } from 'react';
import { Box, Heading, Table, Tbody, Td, Th, Thead, Tr, VStack } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import Race from '../components/common/Race';
import formatter from '../util/formatter';

function CompetitionStatsPage() {
  const { competitionId } = useParams();
  const [competition, setCompetition] = useState(null);
  const [competitionStats, setCompetitionStats] = useState(null);
  const [competitionStatsLoading, setCompetitionStatsLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchCompetitionStats = () => {
      if (!competition) return;
      setCompetitionStatsLoading(true);
      WarpScoresApiService.competitionStats(competition.id)
        .then((data) => {
          setCompetitionStats(data);
        })
        .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setCompetitionStatsLoading(false));
    };

    fetchCompetitionStats();
  }, [competition]);

  useEffect(() => {
    const fetchCompetition = () => {
      if (!competitionId) return;
      setCompetitionStatsLoading(true);
      WarpScoresApiService.competition(competitionId)
        .then((data) => {
          setCompetition(data);
        })
        .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setCompetitionStatsLoading(false));
    };

    fetchCompetition();
  }, [competitionId]);

  const raceStats = Object.keys(competitionStats?.teamAndRaceStats.raceStats ?? []) ?? [];

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="competition"
          league={competition ? [competition.leagueId.key, competition.leagueName] : []}
          competition={[competitionId.key, competition ? competition.name : '']}
        />
      </Box>
      <LoadingOrErrorWrapper loading={competitionStatsLoading} error={error}>
        <HeaderCard
          heading={competition?.name}
          subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}
          detailsHeading="Competition statistics"
          mainImageSrc={competition?.logo ? imageUrls.logo(competition?.logo, competition?.id?.opus) : imageUrls.logo(competition?.leagueLogo, competition?.id?.opus)}
          additionalImageSrc={competition?.logo ? imageUrls.logo(competition?.leagueLogo, competition?.id?.opus) : null}
        >
          <Heading>Competition statistics</Heading>
          Last updated: {formatter.formatAsDate(competitionStats?.lastUpdated)}
        </HeaderCard>
      </LoadingOrErrorWrapper>
      <LoadingOrErrorWrapper loading={competitionStatsLoading} error={error}>
        <Table>
          <Thead>
            <Tr>
              <Th>Race</Th>
              <Th />
              <Th>Matches</Th>
              <Th>Winrate</Th>
              <Th>Wins</Th>
              <Th>Draws</Th>
              <Th>Losses</Th>
              <Th>TD+</Th>
              <Th>TD-</Th>
              <Th>CAS+</Th>
              <Th>CAS-</Th>
            </Tr>
          </Thead>
          <Tbody>
            {raceStats
              .sort(
                (race1, race2) =>
                  (competitionStats?.teamAndRaceStats?.raceStats[race2].winrate ?? 0) -
                  (competitionStats?.teamAndRaceStats?.raceStats[race1].winrate ?? 0)
              )
              .map((race) => {
                const stats = competitionStats?.teamAndRaceStats?.raceStats[race];
                return (
                  <Tr key={race}>
                    <Td>
                      <Race race={race} />
                    </Td>
                    <Td>
                      <Race race={race} asAvatar />
                    </Td>
                    <Td>{stats.matchCount}</Td>
                    <Td>{stats.winrate}</Td>
                    <Td>{stats.wins}</Td>
                    <Td>{stats.draws}</Td>
                    <Td>{stats.losses}</Td>
                    <Td>{stats.inflictedTd}</Td>
                    <Td>{stats.sustainedTd}</Td>
                    <Td>{stats.inflictedCas}</Td>
                    <Td>{stats.sustainedCas}</Td>
                  </Tr>
                );
              })}
          </Tbody>
        </Table>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CompetitionStatsPage;
