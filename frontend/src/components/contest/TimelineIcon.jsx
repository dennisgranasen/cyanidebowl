import React from 'react';
import { Box } from '@chakra-ui/react';
import {
  MdAcUnit,
  MdCampaign,
  MdCasino,
  MdCloud,
  MdDirectionsRun,
  MdFlashOn,
  MdFrontHand,
  MdGroups,
  MdHealing,
  MdPanTool,
  MdPanToolAlt,
  MdPsychology,
  MdSend,
  MdShield,
  MdSportsFootball,
  MdStar,
  MdStars,
  MdSwapHoriz,
  MdWarning,
  MdWbSunny,
  MdUmbrella,
} from 'react-icons/md';
import { FaCow, FaShoePrints, FaSkull } from 'react-icons/fa6';

const normalized = (value) => String(value || '').trim().toLowerCase();

function KickIcon({ size }) {
  return <Box position="relative" w={`${size}px`} h={`${size}px`}>
    <Box
      as={FaShoePrints}
      position="absolute"
      left="0"
      bottom="0"
      boxSize={`${Math.round(size * 0.72)}px`}
      transform="rotate(-28deg)"
    />
    <Box
      as={MdSportsFootball}
      position="absolute"
      right="-1px"
      top="-1px"
      boxSize={`${Math.round(size * 0.48)}px`}
      transform="rotate(-18deg)"
    />
  </Box>;
}

const weatherIcon = (event) => {
  const details = event?.details || {};
  const text = [
    details.weather,
    details.weatherName,
    details.resultName,
    details.result,
    event?.title,
  ].filter(Boolean).join(' ').toLowerCase();

  if (text.includes('blizzard') || text.includes('snow')) return MdAcUnit;
  if (text.includes('rain')) return MdUmbrella;
  if (text.includes('sun') || text.includes('heat')) return MdWbSunny;
  return MdCloud;
};

const kickoffIcon = (event) => {
  const detail = normalized(event?.details?.kickoffEventType);
  if (detail === 'blitz') return MdFlashOn;
  if (detail === 'brilliant_coaching') return MdPsychology;
  if (detail === 'cheering_fans') return MdCampaign;
  if (detail === 'perfect_defence' || detail === 'perfect_defense') return MdShield;
  if (detail === 'pitch_invasion') return MdGroups;
  if (detail === 'quick_snap') return MdDirectionsRun;
  if (detail === 'riot' || detail === 'throw_a_rock' || detail === 'officious_ref') return MdWarning;
  if (detail === 'changing_weather') return weatherIcon(event);
  return KickIcon;
};

function PowIcon({ size }) {
  return <Box
    as="span"
    fontSize={`${Math.max(9, Math.round(size * 0.42))}px`}
    fontWeight="black"
    letterSpacing="-0.08em"
    transform="rotate(-8deg)"
    lineHeight="1"
  >
    POW!
  </Box>;
}

function RedCardIcon({ size }) {
  return <Box
    as="span"
    display="inline-block"
    w={`${Math.max(9, Math.round(size * 0.52))}px`}
    h={`${Math.max(14, Math.round(size * 0.78))}px`}
    bg="red.500"
    borderRadius="1px"
    transform="rotate(7deg)"
    boxShadow="inset 0 0 0 1px rgba(0,0,0,0.18)"
  />;
}

export default function TimelineIcon({ event, size = 20 }) {
  const type = normalized(event?.type);
  const rawType = normalized(event?.rawEventType);
  const sourceAction = normalized(
    event?.details?.sourceActionType || event?.details?.declared_action,
  );

  if (type === 'weather') {
    const Icon = weatherIcon(event);
    return <Icon size={size}/>;
  }
  if (type === 'kickoff') {
    const Icon = kickoffIcon(event);
    return <Icon size={size}/>;
  }
  if (type === 'touchdown') return <MdSportsFootball size={size}/>;
  if (type === 'block') return <PowIcon size={size}/>;
  if (type === 'foul') return <FaShoePrints size={size}/>;
  if (type === 'ejection') return <RedCardIcon size={size}/>;
  if (type === 'reroll') return <MdCasino size={size}/>;
  if (type === 'animal_savagery' || rawType === 'animal_savagery') return <FaCow size={size}/>;
  if (type === 'possession') return <MdPanToolAlt size={size}/>;
  if (type === 'completion' || type === 'pass') return <MdSend size={size}/>;
  if (type === 'handoff') return <MdSwapHoriz size={size}/>;
  if (type === 'catch') {
    const Icon = sourceAction.includes('handoff') ? MdSwapHoriz : MdPanTool;
    return <Icon size={size}/>;
  }
  if (type === 'interception') return <MdFrontHand size={size}/>;
  if (type === 'casualty' || type === 'death') return <FaSkull size={size}/>;
  if (type === 'injury') return <MdHealing size={size}/>;
  if (type === 'check') return <MdWarning size={size}/>;
  if (type === 'ball_loose') return <MdSportsFootball size={size}/>;
  if (type === 'match_start') return <MdStar size={size}/>;
  if (type === 'special') return <MdStars size={size}/>;
  return <MdStar size={size}/>;
}
