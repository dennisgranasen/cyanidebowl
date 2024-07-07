import React from 'react';
import { Box, VStack } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import Circuits from '../components/Circuits';
import ImageUrls from '../ImageUrls';
import config from '../config';

const { isProduction } = config;

function AdminPage() {
  const navigate = useNavigate();
  if (isProduction) {
    setTimeout(() => navigate(`/`), 1000);
  }

  return (
    !isProduction && (
      <VStack align="left">
        <Box>
          <Navigation currentPage="admin" />
        </Box>
        <>
          <HeaderCard
            mainImageSrc={ImageUrls.warpscoresLogoPng('medium')}
            heading="Admin"
            subHeading="Configure circuits for collecting data..."
          />
          <Box>
            <Circuits />
          </Box>
        </>
      </VStack>
    )
  );
}

export default AdminPage;
