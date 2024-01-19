import React from 'react';
import { Center, Heading, Image, LinkBox, LinkOverlay, Spinner, Td, Tr } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import Race from './Race';
import Formatter from '../util/Formatter';
import ImageUrls from '../ImageUrls';
import config from '../config';

const { boxSize } = config;

function Rank({ rank }) {
  return rank !== null ? (
    <LinkBox as={Tr}>
      <Td>
        <LinkOverlay as={RouteLink} to={`/team/${rank.team.id}`} />
        <Center>
          <Heading size="sm">{rank.rank}</Heading>
        </Center>
      </Td>
      <Td>{rank.team.name}</Td>
      <Td>
        <Image
          src={`${ImageUrls.logo(rank.team.logo)}`}
          boxSize={boxSize}
          fallback={<QuestionOutlineIcon boxSize={boxSize} />}
          objectFit="scale-down"
        />
      </Td>
      <Td>{rank.team.coachName}</Td>
      <Td>
        <Race race={rank.team.race} />
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.gamesPlayed)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.score)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.gamesWon)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.gamesDrawn)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.gamesLost)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.inflictedTouchdowns)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.sustainedTouchdowns)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.inflictedTouchdowns - rank.sustainedTouchdowns)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.inflictedCasualties)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.sustainedCasualties)}</Center>
      </Td>
      <Td>
        <Center>{Formatter.formatAsNumber(rank.inflictedCasualties - rank.sustainedCasualties)}</Center>
      </Td>
    </LinkBox>
  ) : (
    <Spinner />
  );
}

export default Rank;
