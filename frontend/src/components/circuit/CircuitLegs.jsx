import React from 'react';
import { Center, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr, useBreakpointValue } from '@chakra-ui/react';
import CircuitLeg from './CircuitLegInfo';
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
        <Center>#</Center>
      </Th>
      {isSmallScreen ? (
        <>
          <Th>Label</Th>
          <Th LegType/>
        </>
      ) : (
        <>
          <Th>Label</Th>
          <Th>League</Th>
          <Th>Competition</Th> 
          <Th>LegType</Th>
          <Th>Game</Th>
          <Th>Platform</Th>
          <Th>Ruleset</Th>
        </>
      )}
    </Tr>
  );
}

function CircuitLegs({ circuit, loading, error }) {
  return (
    <LoadingOrErrorWrapper loading={loading} error={error}>
      <TableContainer>
        <Table variant="stripedClickable" size="sm">
          <Thead>
            <TableColumns />
          </Thead>
          <Tbody>
            { 
              circuit?.circuitLegs.map((circuitLeg) => (
                <CircuitLeg key={circuitLeg.competitionId} circuitLeg={circuitLeg} circuitId={circuit.circuitId} />
              ))
            }           
          </Tbody>
          <Tfoot>
            <TableColumns />
          </Tfoot>
        </Table>
      </TableContainer>
    </LoadingOrErrorWrapper>
  );
}

export default CircuitLegs;
