import React from 'react';
import { Box, Card, CardBody, Flex, Heading, Image } from '@chakra-ui/react';
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import imageUrls from '../../imageUrls';
import LeagueInfo from './LeagueInfo';
import WarpScoresApiService from '../../WarpScoresApiService';

function LeagueCard({ league, ...props }) {
  const navigate = useNavigate();

  const [competitionCountByStatus, setCompetitionCountByStatus] = useState({});
  

  const goToLeague = () => {
    console.log("Navigating to league:", league);
    navigate(`/league/${league.id.opus}/${league.id.value}`);
  };

  useEffect(() => {
    WarpScoresApiService.competitionCountByStatus([league.id])
      .then((counts) => setCompetitionCountByStatus(counts))
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
  }, []);

  return (
    <Card
      backgroundColor="warpScoresBackgroundColor"
      direction={{ base: 'column', sm: 'row' }}
      _hover={{ background: 'warpScoresHoverColor' }}
      cursor="pointer"
      onClick={goToLeague}
      {...props}
    >
      <Box p="0.5rem">
        <Image objectFit="contain" maxW="140px" src={imageUrls.logo(league.logo, league?.id?.opus)} fallback={null} />
      </Box>
      <Flex minWidth="max-content" w="100%">
        <CardBody>
          <Box flex="1" overflow="hidden" minWidth="max-content" w="100%">
            <Heading size="md" mb="0.5rem">
              {league.name}
            </Heading>
            <LeagueInfo league={league} competitionCountByStatus={competitionCountByStatus}/>
          </Box>
        </CardBody>
      </Flex>
    </Card>
  );
}

export default LeagueCard;
