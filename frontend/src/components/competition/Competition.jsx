import React from 'react';
import { HStack, Image, Spinner, Td, Text, Tr, useBreakpointValue } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import CompetitionProgress from './CompetitionProgress';
import prettyPrint from '../../util/prettyPrint';
import abbreviators from '../../util/abbreviators';
import config from '../../config';
import ImageUrls from '../../imageUrls';

const { boxSize, smallScreenBreakpointValues } = config;

function Competition({ competition, league }) {
  const navigate = useNavigate();
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);

  const goToCompetition = () => {
    navigate(`/competition/${competition.uuid}`);
  };

  return competition !== null ? (
    <Tr onClick={goToCompetition}>
      <Td>
        <HStack>
          <Image
            src={ImageUrls.logo(competition.logo || league?.logo)}
            boxSize={boxSize}
            objectFit="scale-down"
            fallback={<QuestionOutlineIcon boxSize={boxSize} />}
          />
          <Text>{competition.name}</Text>
        </HStack>
      </Td>
      <Td>{isSmallScreen ? abbreviators.makeInitials(competition.format) : prettyPrint(competition.format)}</Td>
      <Td>
        <CompetitionProgress
          status={competition.status}
          format={competition.format}
          teamsMax={competition.teamsMax}
          currentRound={competition.currentRound}
          totalRounds={competition.totalRounds}
          totalMatches={competition.totalMatches}
          playedMatches={competition.playedMatches}
          notValidatedMatches={competition.notValidatedMatches}
          liveMatches={competition.liveMatches}
        />
      </Td>
      <Td isNumeric>{competition.teamsMax}</Td>
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Competition;
