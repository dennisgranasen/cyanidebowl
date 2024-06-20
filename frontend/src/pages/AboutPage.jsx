import React from 'react';
import { Box, Stack, Text, useMediaQuery } from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import { FaRegHeart } from 'react-icons/fa6';
import { Link as RouteLink } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import Disclaimer from '../components/misc/Disclaimer';
import ImageUrls from '../ImageUrls';
import HeaderCard from '../components/common/HeaderCard';

function AboutPage() {
  const [smallscreen] = useMediaQuery('(max-width: 768px)');

  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" smallscreen={smallscreen ? 'smallscreen' : undefined} />
      </Box>
      <HeaderCard
        heading="About"
        subHeading={<RouteLink to="/">warp-scores.net</RouteLink>}
        mainImageSrc={ImageUrls.warpscoresLogoPng('medium')}
        smallscreen={smallscreen ? 'smallscreen' : undefined}
      >
        <Text>
          This is a Spike-like (good old Spike made by poncho for BB2 <Icon as={FaRegHeart} />) facade to BB3 data
          provided by Cyanide&apos;s BB3-API.
        </Text>
        <Disclaimer />
      </HeaderCard>
    </Stack>
  );
}

export default AboutPage;
