import React, { useEffect, useState } from 'react';
import { Box, Heading, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr, VStack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import CircuitLeg from '../components/circuit/CircuitLeg';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import config from '../config';

function TableColumns() {
  return (
    <Tr>
      <Th />
      <Th>Label</Th>
      <Th>League/Competition</Th>
      <Th>LegType</Th>
      <Th>Game version</Th>
      <Th>Platform</Th>
      <Th>Knockout?</Th>
      <Th>Collect?</Th>
    </Tr>
  );
}

function CircuitPage() {
  const { circuitId } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [circuit, setCircuit] = useState();

  const compareLegs = (leg, otherLeg) => {
    return leg.label.localeCompare(otherLeg.label);
  };

  const fetchCircuit = (id) => {
    setLoading(true);
    WarpScoresApiService.circuits(id)
      .then((res) => {
        if (res.circuitLegs == null) res.circuitLegs = [];
        else res.circuitLegs.sort(compareLegs);
        setCircuit(res);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchCircuit(circuitId);
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="circuits" circuit={[circuitId, circuit?.circuitName]} />
      </Box>
      <HeaderCard heading={circuit ? circuit.circuitName : 'Circuit'} detailsHeading="Circuit details" />
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <Heading size="md">Circuit legs</Heading>
        <TableContainer mb="1rem">
          <Table variant="simpleClickable" size="sm">
            <Thead>
              <TableColumns />
            </Thead>
            <Tbody>
              {circuit?.circuitLegs.map((circuitLeg) => (
                <CircuitLeg key={circuitLeg.circuitLegId} circuitLeg={circuitLeg} />
              ))}
            </Tbody>
            <Tfoot>
              <TableColumns />
            </Tfoot>
          </Table>
        </TableContainer>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CircuitPage;
