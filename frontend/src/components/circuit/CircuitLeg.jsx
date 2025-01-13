import React, { useEffect, useState } from 'react';
import { Checkbox, Image, Td, Tr } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import ImageUrls from '../../ImageUrls';
import config from '../../config';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import logger from '../../util/Logger';
import useFetchCompetition from '../../hooks/useFetchCompetition';

const { boxSize } = config;

function CircuitLeg({ circuitLeg }) {
  const { fetchCompetition, competition, competitionLoading, error: competitionError } = useFetchCompetition();
  const [league, setLeague] = useState(null);
  const [error, setError] = useState(null);

  const fetchLeague = (compUuid, compType) => {
    WarpScoresApiService.leagues(compUuid)
      .then((data) => {
        setLeague(data);
      })
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString() });
      });
  };

  useEffect(() => {
    logger.debug('Circuit leg is %o', circuitLeg);
    if (circuitLeg && circuitLeg.legType === 'League') {
      fetchLeague(circuitLeg.competitionId);
    } else if (circuitLeg && circuitLeg.legType === 'Competition') {
      fetchCompetition(circuitLeg.competitionId);
    }
  }, []);

  return (
    <LoadingOrErrorWrapper loading={competitionLoading} error={error || competitionError}>
      <Tr>
        <Td>
          {(competition?.logo || competition?.leagueLogo || league?.logo) && (
            <Image
              src={`${ImageUrls.logo(competition.logo || competition.leagueLogo || league?.logo)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />
          )}
        </Td>
        <Td>{circuitLeg.label}</Td>
        <Td>
          {competition
            ? competition.name + (circuitLeg.legType === 'Competition' ? ` (${competition.leagueName})` : '')
            : '*'}
        </Td>
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
    </LoadingOrErrorWrapper>
  );
}

export default CircuitLeg;
