import React from 'react';
import { Center, Heading, Image, Spinner, Td, Text, Tr, useBreakpointValue } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import formatter from '../../util/formatter';
import config from '../../config';
import imageUrls from '../../imageUrls';
import prettyPrint from '../../util/prettyPrint';
import Race from '../common/Race';
import { getRaceLogo, toRace } from '../../util/raceUtil'
import { identityUtils } from '../../util/identityUtil';

const { boxSize, smallScreenBreakpointValues, showRaceLogo } = config;

function Rank({ rank, position }) {
  const navigate = useNavigate();
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  const goToTeam = () => {
      navigate(/*`/league/${identityUtils.key(leagueId)}*/`/team/${identityUtils.key(rank.teamId)}`);
  };
  const opus = identityUtils.opus(rank.teamId);
  const race = rank?.race || (rank?.raceId ? toRace(rank.raceId, opus) : null);
  const raceLogo = rank?.raceId || rank?.race ? getRaceLogo(rank.raceId || rank.race, opus) : null;
  //console.log(rank.teamLogo || raceLogo,imageUrls.logo(rank.teamLogo || raceLogo, opus));
  return rank !== null ? (
    <Tr onClick={goToTeam}>
      <Td>
        <Center>
          <Heading size="sm">{position}</Heading>
        </Center>
      </Td>
      {isSmallScreen ? (
        <>
          <Td>
            <Text fontSize="sm">{rank.teamName}</Text>
            <Text fontSize="sm" color="grey">
              {prettyPrint(race)} ({rank.coachName})
            </Text>
          </Td>
          <Td>
            <Image
              src={`${imageUrls.logo(rank?.teamLogo || raceLogo, opus)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />
          </Td>
        </>
      ) : (
        <>
          <Td>{rank.teamName}</Td>
          <Td>
            <Image
              src={`${imageUrls.logo(rank.teamLogo || raceLogo, opus)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />
          </Td>
          <Td>{rank.coachName}</Td>
          <Td>
              { showRaceLogo && raceLogo ?  
              <Image
                src={`${imageUrls.logo(raceLogo, opus)}`}
                boxSize={boxSize}
                title={prettyPrint(race)}
                fallback={<Race size="sm" race={prettyPrint(race)} />}
                objectFit="scale-down"
              /> : 
              <Race size="sm" race={prettyPrint(race)} />
            }
          </Td>
        </>
      )}
      <Td>
        <Center>{formatter.formatAsNumber(rank.points)}</Center>
      </Td>
      <Td>
        <Center>{formatter.formatAsNumber(rank.wins)}</Center>
      </Td>
      <Td>
        <Center>{formatter.formatAsNumber(rank.draws)}</Center>
      </Td>
      <Td>
        <Center>{formatter.formatAsNumber(rank.losses)}</Center>
      </Td>
      <Td>
        <Center> {formatter.formatAsNumber(rank.matchCount)}</Center>
      </Td>
      {!isSmallScreen && (
        <>
          <Td>
            <Center>{formatter.formatAsNumber(rank.totalTouchdownsFor)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.totalTouchdownsAgainst)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.netTouchdowns)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.totalCasualtiesFor)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.totalCasualtiesAgainst)}</Center>
          </Td>
          <Td>
            <Center>{formatter.formatAsNumber(rank.netCasualties)}</Center>
          </Td>
        </>
      )}
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Rank;
