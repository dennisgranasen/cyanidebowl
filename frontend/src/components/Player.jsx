import React from 'react';
import { Td, Text, Tr } from '@chakra-ui/react';
import { FaBandage } from 'react-icons/fa6';
import Skills from './Skills';
import prettyPrint from '../util/PrettyPrint';
import Injuries from './Injuries';

const NO_PA = '-';

const ATTR_MA = 'ma';
const ATTR_ST = 'st';
const ATTR_AG = 'ag';
const ATTR_PA = 'pa';
const ATTR_AV = 'av';

const INJURY_MA = 'smashed_knee';
const INJURY_ST = 'dislocated_shoulder';
const INJURY_AG = 'neck_injury';
const INJURY_PA = 'broken_arm';
const INJURY_AV = 'head_injury';

function isInjured(attributeType, playerCasualties) {
  const casualties = [].concat(playerCasualties);
  switch (attributeType) {
    case ATTR_MA:
      return casualties.includes(INJURY_MA);
    case ATTR_ST:
      return casualties.includes(INJURY_ST);
    case ATTR_AG:
      return casualties.includes(INJURY_AG);
    case ATTR_PA:
      return casualties.includes(INJURY_PA);
    case ATTR_AV:
      return casualties.includes(INJURY_AV);
    default:
      return false;
  }
}

function countInjuries(playerCasualties, injury) {
  const modifier = playerCasualties.reduce((acc, element) => (element === injury ? acc + 1 : acc), 0);
  return modifier > 2 ? 2 : modifier;
}

function getModifier(type, playerCasualties) {
  switch (type) {
    case ATTR_AG:
      return countInjuries(playerCasualties, INJURY_AG);
    case ATTR_PA:
      return countInjuries(playerCasualties, INJURY_PA);
    default:
      return 0;
  }
}

function format(type, value) {
  switch (type) {
    case ATTR_AG:
    case ATTR_AV:
      return `${value}+`;
    case ATTR_PA:
      return value ? `${value}+` : NO_PA;
    default:
      return value;
  }
}

function minMax(value, min, max) {
  return value ? Math.max(Math.min(value, max), min) : value;
}

function getRealValue(type, playerCasualties, value) {
  const modifier = getModifier(type, playerCasualties, value);
  let realValue = value ? value + 2 * modifier : value;
  switch (type) {
    case ATTR_MA:
      realValue = minMax(realValue, 1, 9);
      break;
    case ATTR_ST:
      realValue = minMax(realValue, 1, 8);
      break;
    case ATTR_AG:
      realValue = minMax(realValue, 1, 6);
      break;
    case ATTR_PA:
      realValue = minMax(realValue, 1, 6);
      break;
    case ATTR_AV:
      realValue = minMax(realValue, 3, 11);
      break;
    default:
      realValue = value;
      break;
  }
  return realValue;
}

function Attribute({ type, value, injured }) {
  return <Text color={injured ? 'red' : null}>{format(type, value)}</Text>;
}

function Player({ player }) {
  return (
    <Tr>
      <Td>{player.number}</Td>
      <Td>{player.name}</Td>
      <Td>{prettyPrint(player.type, '_')}</Td>
      <Td>
        <Skills skills={player.skills} />
      </Td>
      <Td>
        <Injuries injuries={player.casualtiesStates} />
      </Td>
      <Td>{player.suspendedNextMatch ? <FaBandage color="orange" size="24px" /> : ''}</Td>
      <Td>
        <Attribute
          type={ATTR_MA}
          injured={isInjured(ATTR_MA, player.casualtiesStates)}
          value={getRealValue(ATTR_MA, player.casualtiesStates, player.attributes.ma)}
        />
      </Td>
      <Td>
        <Attribute
          type={ATTR_ST}
          injured={isInjured(ATTR_ST, player.casualtiesStates)}
          value={getRealValue(ATTR_ST, player.casualtiesStates, player.attributes.st)}
        />
      </Td>
      <Td>
        <Attribute
          type={ATTR_AG}
          injured={isInjured(ATTR_AG, player.casualtiesStates)}
          value={getRealValue(ATTR_AG, player.casualtiesStates, player.attributes.ag)}
        />
      </Td>
      <Td>
        <Attribute
          type={ATTR_PA}
          injured={isInjured(ATTR_PA, player.casualtiesStates)}
          value={getRealValue(ATTR_PA, player.casualtiesStates, player.attributes.pa)}
        />
      </Td>
      <Td>
        <Attribute
          type={ATTR_AV}
          injured={isInjured(ATTR_AV, player.casualtiesStates)}
          value={getRealValue(ATTR_AV, player.casualtiesStates, player.attributes.av)}
        />
      </Td>
      <Td>{player.value}</Td>
      <Td>{player.xp}</Td>
    </Tr>
  );
}

export default Player;
