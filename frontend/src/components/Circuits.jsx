import React, { useEffect, useState } from 'react';
import { Heading, Button, FormLabel, Input, SimpleGrid, Spinner } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import CircuitCard from './circuit/CircuitCard';

function Circuits() {
  const [circuits, setCircuits] = useState();
  const fetchCircuits = () => {
    WarpScoresApiService.circuits()
      .then((data) => {
        setCircuits(data);
      })
      .catch((reason) => console.log(reason));
  };

  const [newCircuitName, setNewCircuitName] = useState('');
  const updateNewCircuitName = (e) => {
    setNewCircuitName(e.target.value);
  };
  const addCircuit = (e) => {
    WarpScoresApiService.newCircuit(newCircuitName).then((data) => fetchCircuits());
  };
  useEffect(() => {
    fetchCircuits();
  }, []);

  return circuits ? (
    <>
      <Heading size="md">Circuits</Heading>
      {circuits && circuits.length > 0 && (
        <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="20px">
          {circuits.map((circuit) => (
            <CircuitCard key={circuit.circuitId} circuit={circuit} showConfigureLink variant="outline" />
          ))}
        </SimpleGrid>
      )}

      <FormLabel>Create Circuit</FormLabel>
      <Input placeholder="New circuit name" value={newCircuitName} onChange={updateNewCircuitName} />
      <Button onClick={addCircuit}>Add</Button>
    </>
  ) : (
    <Spinner />
  );
}

export default Circuits;
