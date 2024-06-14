import React from 'react';
import { Box, Heading, Stack, Text, useMediaQuery, VStack } from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import Disclaimer from '../components/misc/Disclaimer';
import { Icon } from '@chakra-ui/icons';
import { FaRegHeart } from 'react-icons/fa6';

function AboutPage() {
  const [smallscreen] = useMediaQuery('(max-width: 768px)');

  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" smallscreen={smallscreen ? 'smallscreen' : undefined} />
      </Box>
      <Box>
        <Heading size="md">About</Heading>
        <VStack spacing={2} mt="10" align="left">
          <Text>
            warp-scores.net is a Spike-like (good old Spike <Icon as={FaRegHeart} /> for BB2 made by poncho) facade to BB3 data provided by
            Cyanide&apos;s BB3-API.
          </Text>
          <Disclaimer />
        </VStack>
      </Box>
    </Stack>
  );
}

export default AboutPage;
