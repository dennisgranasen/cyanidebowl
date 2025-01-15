import { Box, Heading, HStack, Image, VStack } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import React from 'react';
import imageUrls from '../../imageUrls';
import prettyPrint from '../../util/prettyPrint';
import formatter from '../../util/formatter';
import config from '../../config';

const { boxSize } = config;

function ArenaMatchLabel({ teamUuid, match }) {
  const opponent = match.teams.find((team) => team.id !== teamUuid);
  return (
    <VStack align="left">
      <HStack>
        <Image
          src={`${imageUrls.logo(opponent.logo)}`}
          boxSize={boxSize}
          fallback={<QuestionOutlineIcon boxSize={boxSize} />}
        />
        <Heading>{opponent.name}</Heading>
      </HStack>
      <VStack align="left">
        <Box>Coach: {opponent.coachName}</Box>
        <Box>Race: {prettyPrint(opponent.race)}</Box>
        <Box>Played: {formatter.formatAsDate(match.finished)}</Box>
      </VStack>
    </VStack>
  );
}

export default ArenaMatchLabel;
