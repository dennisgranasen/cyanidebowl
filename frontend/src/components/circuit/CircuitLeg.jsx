import React, { useEffect, useState } from 'react';
import { Checkbox, Image, Td, Tr, Button } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import imageUrls from '../../imageUrls';
import config from '../../config';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import logger from '../../util/logger';
import useFetchCompetition from '../../hooks/useFetchCompetition';

const { boxSize } = config;

function CircuitLeg({ circuitLeg, onRemoveLeg, onCollectDataChanged, onArchivedChanged }) {
  //const [league, setLeague] = useState(null);
  const [error, setError] = useState(null);
  // Example in CircuitLeg.jsx or parent
  const getOpusFromGame = (game) => {
    switch (game) {
      case 'BB1': return 1;
      case 'BB2': return 2;
      case 'BB3': return 3;
      default: return undefined;
    }
  };


    const { fetchCompetition, league, competition, competitionLoading, error: competitionError } = 
    useFetchCompetition(circuitLeg.entityId);


/*
  const fetchLeague = (leagueId, opus) => {
        logger.info('Fetched league: %o', opus);
    WarpScoresApiService.leagues(leagueId, opus)
      .then((data) => {
        setLeague(data, opus);
        logger.info('Fetched league: %o', data);
      })
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString() });
      });
  };
*/
  useEffect(() => {
    logger.info('Circuit leg is %o', circuitLeg);
    var opus = getOpusFromGame(circuitLeg.game)
    //if (circuitLeg && circuitLeg.legType === 'League') {
    //fetchLeague(circuitLeg.leagueId, opus);

    if (circuitLeg && circuitLeg.entityId)
      fetchCompetition(circuitLeg.entityId);


    
  }, []);

  return (
    //<LoadingOrErrorWrapper loading={competitionLoading} error={error || competitionError}>
      <Tr>
        <Td>
          {(competition?.logo || competition?.leagueLogo || league?.logo) && (
            <Image
              src={`${imageUrls.logo(competition?.logo || competition?.leagueLogo || league?.logo, league?.id?.opus)}`}
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
        <Td>{circuitLeg.ruleset}</Td>
        <Td>{circuitLeg.ladderOption}</Td>
        <Td>
          <Checkbox 
            defaultChecked={circuitLeg.isCollected} 
            onChange={() => onCollectDataChanged && 
              onCollectDataChanged(circuitLeg.circuitLegId, !circuitLeg.isCollected)}
          />
        </Td>
        <Td>
          <Checkbox 
            defaultChecked={circuitLeg.isArchived} 
            onChange={() => onArchivedChanged && 
              onArchivedChanged(circuitLeg.circuitLegId, !circuitLeg.isArchived)}
          />
        </Td>
        <Td>
          <Button
            colorScheme="red"
            size="xs"
            onClick={() => onRemoveLeg && onRemoveLeg(circuitLeg.circuitLegId)}
          >
            Remove
          </Button>
        </Td>

      </Tr>
    //</LoadingOrErrorWrapper>
  );
}

export default CircuitLeg;
