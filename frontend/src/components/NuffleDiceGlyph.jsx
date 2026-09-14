import React from 'react';
import { Box } from '@chakra-ui/react';

export const NUFFLE_BLOCK_DICE = Object.freeze({
  push: 'j',
  attackerDown: 'k',
  defenderDown: 'l',
  tackle: 'm',
  bothDown: 'n',
});

export const NUFFLE_WEATHER = Object.freeze({
  swelteringHeat: 'o',
  // Nuffle Dice has no Very Sunny glyph. Upper-case O is the inverted
  // Sweltering Heat face and deliberately distinguishes the two conditions.
  verySunny: 'O',
  perfectConditions: 'p',
  foggy: 'q',
  blizzard: 'r',
  thunderstorm: 's',
  pouringRain: 't',
});

const normalize = (value) => String(value ?? '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .trim()
  .toLowerCase()
  .replace(/[^a-z0-9]+/g, '_')
  .replace(/^_+|_+$/g, '');

const BLOCK_FACE_ALIASES = Object.freeze({
  push: 'push',
  pushed: 'push',
  attacker_down: 'attackerDown',
  attackerdown: 'attackerDown',
  skull: 'attackerDown',
  defender_down: 'defenderDown',
  defenderdown: 'defenderDown',
  pow: 'defenderDown',
  tackle: 'tackle',
  defender_stumbles: 'tackle',
  defenderstumbles: 'tackle',
  stumble: 'tackle',
  both_down: 'bothDown',
  bothdown: 'bothDown',
});

const WEATHER_ALIASES = Object.freeze({
  sweltering_heat: 'swelteringHeat',
  sweltering: 'swelteringHeat',
  very_sunny: 'verySunny',
  perfect_conditions: 'perfectConditions',
  // Accept old/foreign payload wording but never display it back to users.
  perfect_weather: 'perfectConditions',
  foggy: 'foggy',
  fog: 'foggy',
  blizzard: 'blizzard',
  snow: 'blizzard',
  thunderstorm: 'thunderstorm',
  thunder_storm: 'thunderstorm',
  aska: 'thunderstorm',
  pouring_rain: 'pouringRain',
  rain: 'pouringRain',
});

const WEATHER_NAMES = Object.freeze({
  swelteringHeat: 'Sweltering Heat',
  verySunny: 'Very Sunny',
  perfectConditions: 'Perfect conditions',
  foggy: 'Foggy',
  blizzard: 'Blizzard',
  thunderstorm: 'Thunderstorm',
  pouringRain: 'Pouring Rain',
});

const weatherKey = (value) => {
  const key = normalize(value);
  if (/^\d+$/.test(key)) {
    const roll = Number(key);
    if (roll === 2) return 'swelteringHeat';
    if (roll === 3) return 'verySunny';
    if (roll >= 4 && roll <= 10) return 'perfectConditions';
    if (roll === 11) return 'pouringRain';
    if (roll === 12) return 'blizzard';
  }
  if (key === '4_10') return 'perfectConditions';
  return WEATHER_ALIASES[key] || null;
};

export const blockDieGlyph = (value) => {
  const face = BLOCK_FACE_ALIASES[normalize(value)];
  return face ? NUFFLE_BLOCK_DICE[face] : null;
};

export const weatherGlyph = (value) => {
  const weather = weatherKey(value);
  return weather ? NUFFLE_WEATHER[weather] : null;
};

export const canonicalWeatherName = (value) => {
  const weather = weatherKey(value);
  return weather ? WEATHER_NAMES[weather] : value;
};

export const ratingGlyphs = (value) => {
  const rating = Number(value);
  if (!Number.isInteger(rating) || rating < -3 || rating > 3) return null;
  if (rating < 0) return Array(Math.abs(rating)).fill(NUFFLE_BLOCK_DICE.attackerDown).join(' ');
  if (rating === 0) return NUFFLE_BLOCK_DICE.bothDown;
  return Array(rating).fill(NUFFLE_BLOCK_DICE.defenderDown).join(' ');
};

export default function NuffleDiceGlyph({ glyph, label, fontSize = '1.35em', ...props }) {
  if (!glyph) return null;
  return <Box
    as="span"
    fontFamily="nuffleDice"
    fontSize={fontSize}
    lineHeight="1"
    aria-label={label}
    title={label}
    {...props}
  >
    {glyph}
  </Box>;
}
