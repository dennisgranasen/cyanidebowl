import React, { useEffect, useState } from 'react';
import { Checkbox, Image, Td, Tr } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import ImageUrls from '../../ImageUrls';
import config from '../../config';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import logger from '../../util/Logger';

const { boxSize } = config;

function CircuitLeg({ circuitLeg }) {
  const [competition, setCompetition] = useState(null);
  const [error, setError] = useState(null);

  const fetchLeague = (compUuid, compType) => {
    WarpScoresApiService.leagues(compUuid)
      .then((data) => {
        setCompetition(data);
      })
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString(config.locale) });
      });
  };

  const fetchCompetition = (compUuid) => {
    WarpScoresApiService.competition(compUuid)
      .then((data) => {
        setCompetition(data);
      })
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString(config.locale) });
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
    <LoadingOrErrorWrapper loading={circuitLeg === null} error={error}>
      <Tr>
        <Td>
          {(competition?.logo || competition?.leagueLogo) && (
            <Image
              src={`${ImageUrls.logo(competition.logo || competition.leagueLogo)}`}
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
