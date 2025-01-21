import React from 'react';
import { Center, Image, Td, Text, Tr } from '@chakra-ui/react';
import { FaBandage, FaStar } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import {
  TbHexagon,
  TbHexagonNumber0,
  TbHexagonNumber1,
  TbHexagonNumber2,
  TbHexagonNumber3,
  TbHexagonNumber4,
  TbHexagonNumber5,
  TbHexagonNumber6,
  TbHexagonNumber7,
  TbHexagonNumber8,
  TbHexagonNumber9,
} from 'react-icons/tb';
import Skills from './Skills';
import prettyPrint from '../../util/prettyPrint';
import Injuries from './Injuries';
import config from '../../config';

const { smallBoxSize, tinyBoxSize } = config;

const NO_PA = '-';

const ATTR_MA = 'ma';
const ATTR_ST = 'st';
const ATTR_AG = 'ag';
const ATTR_PA = 'pa';
const ATTR_AV = 'av';

const ATTR_MIN_VALUES = {};
ATTR_MIN_VALUES[ATTR_MA] = 1;
ATTR_MIN_VALUES[ATTR_ST] = 1;
ATTR_MIN_VALUES[ATTR_AG] = 1;
ATTR_MIN_VALUES[ATTR_PA] = 1;
ATTR_MIN_VALUES[ATTR_AV] = 3;
const ATTR_MAX_VALUES = {};
ATTR_MAX_VALUES[ATTR_MA] = 9;
ATTR_MAX_VALUES[ATTR_ST] = 8;
ATTR_MAX_VALUES[ATTR_AG] = 6;
ATTR_MAX_VALUES[ATTR_PA] = 6;
ATTR_MAX_VALUES[ATTR_AV] = 11;

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

function getRealValue(type, defaultValue, bonusValue, malusValue) {
  let realValue;
  switch (type) {
    case ATTR_MA:
    case ATTR_ST:
    case ATTR_AV:
      realValue = defaultValue ? defaultValue + bonusValue - malusValue : defaultValue;
      break;
    case ATTR_AG:
    case ATTR_PA:
      realValue = defaultValue ? defaultValue - bonusValue + malusValue : defaultValue;
      break;
    default:
      realValue = defaultValue;
      break;
  }
  return minMax(realValue, ATTR_MIN_VALUES[type], ATTR_MAX_VALUES[type]);
}

function getValueFrom(arrayOfObjects, neededType) {
  const filtered = arrayOfObjects.map((object) =>
    Object.keys(object)[0] === neededType ? object[Object.keys(object)] : 0
  );
  return filtered[0];
}

function Attribute({ type, defaultAttributes, bonus, malus }) {
  const defaultValue = defaultAttributes[type];
  const bonusValue = getValueFrom(bonus, type) || 0;
  const malusValue = getValueFrom(malus, type) || 0;
  let color = null;
  if (malusValue > 0) color = bonusValue > 0 ? 'orange' : 'red';
  else if (bonusValue > 0) color = 'green';

  return <Text color={color}>{format(type, getRealValue(type, defaultValue, bonusValue, malusValue))}</Text>;
}

function lookupStarPlayerName(name) {
  const nameWithoutPrefix = name.replace('name_sp_', '');
  return `"${prettyPrint(nameWithoutPrefix)}" (Starplayer)`;
}

function iconFor(level) {
  switch (level) {
    case 0:
      return TbHexagonNumber0;
    case 1:
      return TbHexagonNumber1;
    case 2:
      return TbHexagonNumber2;
    case 3:
      return TbHexagonNumber3;
    case 4:
      return TbHexagonNumber4;
    case 5:
      return TbHexagonNumber5;
    case 6:
      return TbHexagonNumber6;
    case 7:
      return TbHexagonNumber7;
    case 8:
      return TbHexagonNumber8;
    case 9:
      return TbHexagonNumber9;
    default:
      return TbHexagon;
  }
}

function PlayerLevel({ level, starPlayer }) {
  if (!level && !starPlayer) return null;

  return starPlayer ? (
    <Icon as={FaStar} boxSize={tinyBoxSize} color="yellow.500" />
  ) : (
    <Icon as={iconFor(level)} boxSize={tinyBoxSize} />
  );
}

function Player({ player }) {
  const defaultAttributes = player.extendedAttributes ? player.extendedAttributes.defaultAttributes : player.attributes;
  const bonus = player.extendedAttributes ? player.extendedAttributes.bonus : [];
  const malus = player.extendedAttributes ? player.extendedAttributes.malus : [];
  const isStarplayer = player.type.endsWith('Star');
  return (
    <Tr>
      <Td>{player.number}</Td>
      <Td>{isStarplayer ? lookupStarPlayerName(player.name) : player.name}</Td>
      <Td>{prettyPrint(player.type, '_')}</Td>
      <Td>
        <Center>
          <PlayerLevel level={player.level} starPlayer={isStarplayer} />
        </Center>
      </Td>
      <Td>
        <Skills skills={player.skills} />
      </Td>
      <Td>
        <Injuries injuries={player.casualtiesStates} />
      </Td>
      <Td>
        <Center>
          {player.suspendedNextMatch ? (
            <Image
              src="/img/injuries/recovering.png"
              alt="MNG"
              boxSize={smallBoxSize}
              fallback={<FaBandage color="orange" size={smallBoxSize} />}
            />
          ) : (
            ''
          )}
        </Center>
      </Td>
      <Td>
        <Center>
          <Attribute type={ATTR_MA} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
        </Center>
      </Td>
      <Td>
        <Center>
          <Attribute type={ATTR_ST} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
        </Center>
      </Td>
      <Td>
        <Center>
          <Attribute type={ATTR_AG} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
        </Center>
      </Td>
      <Td>
        <Center>
          <Attribute type={ATTR_PA} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
        </Center>
      </Td>
      <Td>
        <Center>
          <Attribute type={ATTR_AV} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
        </Center>
      </Td>
      <Td>
        <Center>{player.xp}</Center>
      </Td>
      <Td isNumeric>{player.value}</Td>
    </Tr>
  );
}

export default Player;
