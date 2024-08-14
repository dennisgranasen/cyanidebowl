import React from 'react';
import {
  Box,
  Card,
  CardBody,
  Center,
  Grid,
  GridItem,
  Heading,
  HStack,
  Link as ChakraLink,
  Text,
} from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import { FaGear } from 'react-icons/fa6';
import { Link as ReactRouterLink } from 'react-router-dom';

function LeagueCollectionCard({ leagueCollection, showConfigureLink, noContentIcon, noContentHeading, variant }) {
  return (
    <Card direction="row" overflow="hidden" variant={variant} align="center">
      {!leagueCollection && noContentIcon && (
        <Center p="2">
          <Icon as={noContentIcon} boxSize="4em" />
        </Center>
      )}
      <CardBody p={2}>
        <Box w="100%">
          {!leagueCollection && noContentHeading && <Heading size="md">{noContentHeading}</Heading>}
          {leagueCollection && (
            <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
              <GridItem colSpan={8}>
                <Center color="grey">
                  <HStack>
                    <ChakraLink as={ReactRouterLink} to={`circuit/${leagueCollection.circuitId}`}>
                      {leagueCollection.circuitName}-!!-
                    </ChakraLink>
                    <ChakraLink as={ReactRouterLink} to={`configureCircuit/${leagueCollection.circuitId}`}>
                      {showConfigureLink && <FaGear />}
                    </ChakraLink>
                  </HStack>
                </Center>
              </GridItem>
              {leagueCollection?.leagues &&
                leagueCollection.leagues.map((league) => (
                  <GridItem key={league.id} colSpan={8}>
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

export default LeagueCollectionCard;
