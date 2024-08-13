import React from 'react';
import { Box, VStack } from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import Circuits from '../components/Circuits';
import ImageUrls from '../ImageUrls';

function AdminPage() {
  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="admin" />
      </Box>
      <HeaderCard
        mainImageSrc={ImageUrls.warpscoresLogoPng('medium')}
        heading="Admin"
        subHeading="Configure circuits for collecting data..."
      />
      <Box>
        <Circuits />
      </Box>
    </VStack>
  );
}

export default AdminPage;
