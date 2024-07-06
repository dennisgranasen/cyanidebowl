import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Box, Heading, useMediaQuery, VStack } from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import Circuits from '../components/Circuits'


function AdminPage() {
  const [smallscreen] = useMediaQuery('(max-width: 768px)');
  const platforms = [
    "bb1.pc",
    "bb2.pc",
    "bb2.xbox",
    "bb2.playstation",
    "bb3.cross",
    "bb3.switch",
    "fumbbl.lrb6",
    "fumbbl.2020",
    "tt.lrb6",
    "tt.2020"
  ]
  const [platform, setPlatform] = useState();

  const changePlatform = (e) => {
    const platformId = e.target.value
    setPlatform(platformId)
  };

  return (
    <VStack align="left">7
      <Box>
        <Navigation
          currentPage="admin"
        />
      </Box>
        <>
          <HeaderCard
            smallscreen={smallscreen ? 'smallscreen' : undefined}
          >
          </HeaderCard>
          <Box>
            <Circuits/>
          </Box>
        </>
      
    </VStack>
  );
}

export default AdminPage;
