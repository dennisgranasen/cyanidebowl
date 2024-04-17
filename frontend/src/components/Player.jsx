import React from 'react';
import { Tag, Td, Text, Tr } from '@chakra-ui/react';
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

function romanize(num) {
  const lookup = { X: 10, IX: 9, V: 5, IV: 4, I: 1 };
  let roman = '';
  let i;
  for (i in lookup) {
    while (num >= lookup[i]) {
      roman += i;
      num -= lookup[i];
    }
  }
  return roman;
}

function Player({ player }) {
  const defaultAttributes = player.extendedAttributes ? player.extendedAttributes.defaultAttributes : player.attributes;
  const bonus = player.extendedAttributes ? player.extendedAttributes.bonus : [];
  const malus = player.extendedAttributes ? player.extendedAttributes.malus : [];
  return (
    <Tr>
      <Td>{player.number}</Td>
      <Td>{player.name}</Td>
      <Td>{prettyPrint(player.type, '_')}</Td>
      <Td>{player.level > 0 && <Tag size="sm" borderRadius="full">{`${romanize(player.level)}`}</Tag>}</Td>
      <Td>
        <Skills skills={player.skills} />
      </Td>
      <Td>
        <Injuries injuries={player.casualtiesStates} />
      </Td>
      <Td>{player.suspendedNextMatch ? <FaBandage color="orange" size="24px" /> : ''}</Td>
      <Td>
        <Attribute type={ATTR_MA} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
      </Td>
      <Td>
        <Attribute type={ATTR_ST} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
      </Td>
      <Td>
        <Attribute type={ATTR_AG} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
      </Td>
      <Td>
        <Attribute type={ATTR_PA} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
      </Td>
      <Td>
        <Attribute type={ATTR_AV} defaultAttributes={defaultAttributes} bonus={bonus} malus={malus} />
      </Td>
      <Td>{player.value}</Td>
      <Td>{player.xp}</Td>
    </Tr>
  );
}

export default Player;

/*
   "attributes_ex": {
        "default": {
          "ma": new
          NumberInt(
          "4"
          ),
          "st": new
          NumberInt(
          "5"
          ),
          "ag": new
          NumberInt(
          "5"
          ),
          "av": new
          NumberInt(
          "10"
          )
        },
        "bonus": [],
        "malus": []
      },
 */
