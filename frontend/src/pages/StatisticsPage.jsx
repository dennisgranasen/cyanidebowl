import React from 'react';
import { Box, Heading, HStack, Stack } from '@chakra-ui/react';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import Navigation from '../components/misc/Navigation';

function StatisticsPage() {
  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" />
      </Box>
      <Box>
        <Heading size="md">Statistics</Heading>
        <HStack spacing={2} mt="10" align="left">
          <Icon as={FaRegFaceSadTear} size="lg" />
          <Box>Not available yet...</Box>
        </HStack>
      </Box>
    </Stack>
  );
}

export default StatisticsPage;
