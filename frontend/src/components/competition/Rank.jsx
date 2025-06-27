import React from 'react';
import { Center, Heading, Image, Spinner, Td, Text, Tr, useBreakpointValue } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import formatter from '../../util/formatter';
import config from '../../config';
import imageUrls from '../../imageUrls';
import prettyPrint from '../../util/prettyPrint';
import Race from '../common/Race';

const { boxSize, smallScreenBreakpointValues } = config;

function Rank({ rank, competitionId }) {
  const navigate = useNavigate();
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  const goToTeam = () => {
    if (rank?.team?.competitionIds || competitionUuid) {
      const id = competitionId || rank?.team?.competitionIds[0];
      navigate(`/competition/${id.opus}/${id.value}/team/${rank.team.id}`);
    }
  };
  return rank !== null ? (
    <Tr onClick={goToTeam}>
      <Td>
        <Center>
          <Heading size="sm">{rank.rank}</Heading>
        </Center>
      </Td>
      {isSmallScreen ? (
        <>
          <Td>
            <Text fontSize="sm">{rank.team.name}</Text>
            <Text fontSize="sm" color="grey">
              {prettyPrint(rank.team.race)} ({rank.team.coachName})
            </Text>
          </Td>
          <Td>
            <Image
              src={`${imageUrls.logo(rank.team.logo, rank?.opus)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />
          </Td>
        </>
      ) : (
        <>
          <Td>{rank.team.name}</Td>
          <Td>
            <Image
              src={`${imageUrls.logo(rank.team.logo, rank?.opus)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />
          </Td>
          <Td>{rank.team.coachName}</Td>
          <Td>
            <Race size="sm" race={rank.team.race} />
          </Td>
        </>
      )}
      <Td>
        <Center>{formatter.formatAsNumber(rank.score)}</Center>
      </Td>
      <Td>
        <Center>{formatter.formatAsNumber(rank.gamesWon)}</Center>
      </Td>
      <Td>
        <Center>{formatter.formatAsNumber(rank.gamesDrawn)}</Center>
      </Td>
      <Td>
        <Center>{formatter.formatAsNumber(rank.gamesLost)}</Center>
      </Td>
      <Td>
        <Center> {formatter.formatAsNumber(rank.gamesPlayed)}</Center>
      </Td>
      {!isSmallScreen && (
        <>
          <Td>
            <Center>{formatter.formatAsNumber(rank.inflictedTouchdowns)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.sustainedTouchdowns)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.inflictedTouchdowns - rank.sustainedTouchdowns)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.inflictedCasualties)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.sustainedCasualties)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.inflictedCasualties - rank.sustainedCasualties)}</Center>
          </Td>
        </>
      )}
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Rank;
