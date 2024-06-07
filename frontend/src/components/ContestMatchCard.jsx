import React from 'react';
import { Box, Card, CardBody, Center, Heading } from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';

function ContestMatchCard({ optionalIcon, optionalHeading, children }) {
  return (
    <Card direction="row" overflow="hidden" variant="outline" align="center">
      {optionalIcon && (
        <Center p="2">
          <Icon as={optionalIcon} boxSize="4em" />
        </Center>
      )}
      <CardBody p={2}>
        <Box w="100%">
          {optionalHeading && <Heading size="md">{optionalHeading}</Heading>}
          {children}
        </Box>
      </CardBody>
    </Card>
  );
}

export default ContestMatchCard;
