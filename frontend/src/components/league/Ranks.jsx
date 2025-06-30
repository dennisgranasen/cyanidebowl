import React from 'react';
import { Center, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr, useBreakpointValue } from '@chakra-ui/react';
import Rank from './Rank';
import config from '../../config';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import WarpScoresApiService from '../../WarpScoresApiService';

const { smallScreenBreakpointValues } = config;
/*
function getLogoForTeam(teamId, teams) {
  const team = teams.find((t) => t.id.key === teamId.key);
  return team ? team.logo : null;
}
*/
function TableColumns() {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  return (
    <Tr>
      <Th>
        <Center>{isSmallScreen ? 'R' : 'Rank'}</Center>
      </Th>
      {isSmallScreen ? (
        <>
          <Th>Team/Coach</Th>
          <Th />
        </>
      ) : (
        <>
          <Th>Team-Name</Th>
          <Th />
          <Th>Coach-Name</Th>
          <Th>Race</Th>
        </>
      )}
      <Th>
        <Center>{isSmallScreen ? 'Sc.' : 'Score'}</Center>
      </Th>
      <Th>
        <Center>W</Center>
      </Th>
      <Th>
        <Center>D</Center>
      </Th>
      <Th>
        <Center>L</Center>
      </Th>
      <Th>
        <Center>{isSmallScreen ? 'GP' : 'Games'}</Center>
      </Th>
      {!isSmallScreen && (
        <>
          <Th>
            <Center>TD+</Center>
          </Th>
          <Th>
            <Center>TD-</Center>
          </Th>
          <Th>
            <Center>TDD</Center>
          </Th>
          <Th>
            <Center>CAS+</Center>
          </Th>
          <Th>
            <Center>CAS-</Center>
          </Th>
          <Th>
            <Center>CASD</Center>
          </Th>
        </>
      )}
    </Tr>
  );
}

function Ranks({ ranks, teams, leagueId, loading, error }) {
  return (
    <LoadingOrErrorWrapper loading={loading} error={error}>
      <TableContainer>
        <Table variant="stripedClickable" size="sm">
          <Thead>
            <TableColumns />
          </Thead>
          <Tbody>
            { ranks?.map((rank, index) => {
                //const teamLogo = getLogoForTeam(rank.teamId, teams);
                return <Rank leagueId={leagueId} /*logo={teamLogo}*/ rank={rank} position={index + 1} key={rank.teamId.key} />;
            })}
          </Tbody>
          <Tfoot>
            <TableColumns />
          </Tfoot>
        </Table>
      </TableContainer>
    </LoadingOrErrorWrapper>
  );
}

export default Ranks;
