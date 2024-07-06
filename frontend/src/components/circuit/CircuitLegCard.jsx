import React, { useState } from 'react';
import { 
  Box, 
  Button,
  Card, 
  CardBody, 
  Center,
  FormLabel,
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

function CircuitKegCard({ circuitLeg }) {
  return (
    <Card direction="row" overflow="hidden" align="center">
      <CardBody p={2}>
        <Box w="100%">
          {circuitLeg && (
            <Grid templateRows="repeat(4)" templateColumns="repeat(8, 1fr)" gap={4} w="100%">
              <GridItem colSpan={8}>
                <Center color="grey">
                  <HStack>
                      <ChakraLink as={ReactRouterLink} to={`competition/${circuitLeg.circuitLegId}`}>{circuitLeg.label}</ChakraLink>
                      <ChakraLink 
                        as={ReactRouterLink}
                        to={`competition/${circuitLeg.circuitLegId}`}>                        
                      </ChakraLink>
                  </HStack>
                </Center>
              </GridItem>
            </Grid>)}
        </Box>
      </CardBody>
    </Card>
  );
}

export default CircuitLegCard;
