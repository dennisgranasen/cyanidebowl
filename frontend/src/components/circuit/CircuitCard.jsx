import React, { useState } from 'react';
import { 
  Box, 
  Button,
  Card, 
  CardBody, 
  Center,
  Grid,
  GridItem,
  HStack,
  Heading, 
  IconButton,
  Image,
  Progress,
  Text,
  VStack
} from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import { FaGear, FaTowerBroadcast } from 'react-icons/fa6';
import config from '../../config';
import { Link as ReactRouterLink } from 'react-router-dom'
import { Link as ChakraLink, LinkProps } from '@chakra-ui/react'

const { boxSize } = config;

function CircuitCard({ circuit, showConfigureLink, noContentIcon, noContentHeading, noContentText, variant }) {
  return (
    <Card direction="row" overflow="hidden" variant={variant} align="center">
      {!circuit && noContentIcon && (
        <Center p="2">
          <Icon as={noContentIcon} boxSize="4em" />
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
                      <ChakraLink as={ReactRouterLink} to={`circuit/${circuit.circuitId}`}>{circuit.circuitName}</ChakraLink>
                      <ChakraLink 
                        as={ReactRouterLink}
                        to={`circuit/${circuit.circuitId}`}>
                        {showConfigureLink ? <FaGear /> : <></>}
                      </ChakraLink>
                  </HStack>
                </Center>
              </GridItem>
              {
                circuit.leagues && 
                circuit.leagues.map((league) => 
                    (<GridItem colSpan={8}>
                        <Text>{league.label} ({league.id}) {league.platform}</Text>
                    </GridItem>
                    ))}
            </Grid>)}
        </Box>
      </CardBody>
    </Card>
  );
}

export default CircuitCard;
