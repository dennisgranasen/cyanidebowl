import React from 'react';
import { Box, Card, CardBody, Center, Grid, GridItem, HStack, Link } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';

function CircuitLegCard({ circuitLeg }) {
  return (
    <Card direction="row" overflow="hidden" align="center">
      <CardBody p={2}>
        <Box w="100%">
          {circuitLeg && (
            <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
              <GridItem colSpan={8}>
                <Center color="grey">
                  <HStack>
                    <Link as={RouteLink} to={`competition/${circuitLeg.circuitLegId}`}>
                      {circuitLeg.label}
                    </Link>
                    <Link as={RouteLink} to={`competition/${circuitLeg.circuitLegId}`} />
                  </HStack>
                </Center>
              </GridItem>
            </Grid>
          )}
        </Box>
      </CardBody>
    </Card>
  );
}

export default CircuitLegCard;
