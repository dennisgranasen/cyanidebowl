import React, { useEffect, useState } from 'react';
import { Box, Heading, Table, TableContainer, Tbody, Td, Tfoot, Th, Thead, Tr, VStack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import Circuit from '../components/circuit/Circuit';
import CircuitLegs from '../components/circuit/CircuitLegs';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function TableColumns() {
  return (
    <Tr>
      <Th>Label</Th>
      <Th>LegType</Th>
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
        <Heading size="md">{circuit ? circuit.circuitName : 'Loading...'}</Heading>
        <CircuitLegs circuit={circuit} loading={loading} error={error} />
        <Circuit circuit={circuit} circuitLoading={loading} />
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CircuitPage;
