import React, { useEffect, useState } from 'react';
import { Checkbox, Heading, Button, FormLabel, Image, Input, SimpleGrid, Spinner, Td, Text, Tr } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import formatter from '../../util/Formatter';
import WarpScoresApiService from '../../WarpScoresApiService';
import CircuitCard from './CircuitCard';
import prettyPrint from '../../util/PrettyPrint';
import abbreviators from '../../util/Abbreviators';
import ImageUrls from '../../ImageUrls';
import config from '../../config';


const { boxSize } = config;

function CircuitLeg({ circuitLeg, smallscreen }) {
  const [competition, setCompetition] = useState(null);

  const fetchLeague = (compUuid, compType) => {
    WarpScoresApiService.league(compUuid).then((data) => {
      setCompetition(data);
      console.log(data)
    });
  };

  const fetchCompetition = (compUuid) => {
    WarpScoresApiService.competition(compUuid).then((data) => {
      setCompetition(data);
      console.log(data)
    });
  };

  useEffect(() => {
    console.log(circuitLeg)
    if (circuitLeg && circuitLeg.legType == "League") {
      fetchLeague(circuitLeg.competitionId);
    } else if (circuitLeg && circuitLeg.legType == "Competition") {
      fetchCompetition(circuitLeg.competitionId);
    }
  }, []);


  
  return circuitLeg !== null ? (
    <Tr>
      <Td>{competition && ((competition.logo && (<Image
              src={`${ImageUrls.logo(competition.logo)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />)) ||
            ((competition.leagueLogo && (<Image
              src={`${ImageUrls.logo(competition.leagueLogo)}`}
              boxSize={boxSize}
              fallback={<QuestionOutlineIcon boxSize={boxSize} />}
              objectFit="scale-down"
            />))
            
            ))}</Td>
      <Td>{circuitLeg.label}</Td>
      <Td>{competition ? (competition.name + (circuitLeg.legType == "Competition" ? " (" + competition.leagueName + ")" : "")): "*"}</Td>
      <Td>{circuitLeg.legType}</Td>
      <Td>{circuitLeg.game}</Td>
      <Td>{circuitLeg.platform}</Td>
      <Td><Checkbox defaultChecked={circuitLeg.isKnockout} readOnly={true}/></Td>
      <Td><Checkbox defaultChecked={circuitLeg.isCollected} readOnly={true}/></Td>
    </Tr>
  ) : (
    <Spinner />
  );
}

export default CircuitLeg;