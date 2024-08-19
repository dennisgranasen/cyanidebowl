import React from 'react';
import { Box, Card, CardBody, Center, Grid, GridItem, Heading, HStack, Link, Text } from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import { FaGear } from 'react-icons/fa6';
import { Link as RouteLink } from 'react-router-dom';

function CircuitCard({ circuit, showConfigureLink, noContentIcon, noContentHeading, noContentText, variant }) {
  return (
    <Card
      backgroundColor="warpScoresBackgroundColor"
      direction="row"
      overflow="hidden"
      variant={variant}
      align="center"
    >
      {!circuit && noContentIcon && (
        <Center p="2">
          <Icon as={noContentIcon} boxSize="4em" />
          {noContentText}
        </Center>
      )}
      <CardBody p={2}>
        <Box w="100%">
          {!circuit && noContentHeading && <Heading size="md">{noContentHeading}</Heading>}
          {circuit && (
            <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
              <GridItem colSpan={8}>
                <Center color="grey">
                  <HStack>
                    <Link as={RouteLink} to={`circuit/${circuit.circuitId}`}>
                      {circuit.circuitName}
                    </Link>
                    <Link as={RouteLink} to={`circuit/${circuit.circuitId}`}>
                      {showConfigureLink && <Icon as={FaGear} />}
                    </Link>
                  </HStack>
                </Center>
              </GridItem>
              {circuit.leagues &&
                circuit.leagues.map((league) => (
                  <GridItem colSpan={8} key={league.id}>
                    <Text>
                      {league.label} ({league.id}) {league.platform}
                    </Text>
                  </GridItem>
                ))}
            </Grid>
          )}
        </Box>
      </CardBody>
    </Card>
  );
}

export default CircuitCard;
