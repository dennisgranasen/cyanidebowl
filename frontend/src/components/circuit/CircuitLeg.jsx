import React, { useEffect, useState } from 'react';
import { Checkbox, Image, Td, Tr } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import imageUrls from '../../imageUrls';
import config from '../../config';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import logger from '../../util/logger';
import useFetchCompetition from '../../hooks/useFetchCompetition';

const { boxSize } = config;

function CircuitLeg({ circuitLeg }) {
  const [league, setLeague] = useState(null);
  const [error, setError] = useState(null);
  const { fetchCompetition, competition, competitionLoading, error: competitionError } = 
    useFetchCompetition(circuitLeg.leagueId, circuitLeg.competitionId, 3);

  const fetchLeague = (leagueId) => {
    WarpScoresApiService.leagues(leagueId)
      .then((data) => {
        setLeague(data);
        logger.info('Fetched league: %o', data);
      })
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString() });
      });
  };

  useEffect(() => {
    logger.info('Circuit leg is %o', circuitLeg);
    //if (circuitLeg && circuitLeg.legType === 'League') {
    fetchLeague(circuitLeg.leagueId);

    if (circuitLeg && circuitLeg.legType === 'Competition' && circuitLeg.competitionId)
    {
      fetchCompetition(circuitLeg.leagueId, circuitLeg.competitionId, 3);
    }

    
  }, []);

  return (
    //<LoadingOrErrorWrapper loading={competitionLoading} error={error || competitionError}>
      <Tr>
        <Td>
          {(competition?.logo || competition?.leagueLogo || league?.logo) && (
            <Image
              src={`${imageUrls.logo(competition?.logo || competition?.leagueLogo || league?.logo, league?.opus)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />
          )}
        </Td>
        <Td>{circuitLeg.label}</Td>
        <Td>{league?.name || circuitLeg.leagueId }</Td>
        <Td>{competition?.name || "*"}</Td>
        <Td>{circuitLeg.legType}</Td>
        <Td>{circuitLeg.game}</Td>
        <Td>{circuitLeg.platform}</Td>
        <Td>
          <Checkbox defaultChecked={circuitLeg.isKnockout} readOnly />
        </Td>
        <Td>
          <Checkbox defaultChecked={circuitLeg.isCollected} readOnly />
        </Td>
      </Tr>
    //</LoadingOrErrorWrapper>
  );
}

export default CircuitLeg;
