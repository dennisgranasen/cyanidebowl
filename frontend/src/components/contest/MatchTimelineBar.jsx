import React from 'react';
import {
  Badge,
  Box,
  Button,
  Collapse,
  HStack,
  Image,
  Text,
  Tooltip,
  VStack,
} from '@chakra-ui/react';

import imageUrls from '../../imageUrls';

const EVENT_STYLE = {
  TOUCHDOWN: { glyph: 'TD', label: 'Touchdown', size: 34 },
  COMPLETION: { glyph: '↗', label: 'Completion', size: 30 },
  INTERCEPTION: { glyph: 'INT', label: 'Interception', size: 34 },
  CASUALTY: { glyph: '☠', label: 'Casualty', size: 34 },
  INJURY: { glyph: '+', label: 'Injury', size: 30 },
  DEATH: { glyph: '†', label: 'Death', size: 34 },
  EJECTION: { glyph: '!', label: 'Ejection', size: 30 },
  APOTHECARY: { glyph: '+', label: 'Apothecary', size: 30 },
  MVP: { glyph: '★', label: 'MVP', size: 32 },
  KICKOFF: { glyph: 'KO', label: 'Kick-off', size: 32 },
  KICKOFF_DETAIL: { glyph: 'K', label: 'Kick-off event', size: 28 },
  WEATHER: { glyph: '☁', label: 'Weather', size: 30 },
  BLOCK: { glyph: 'B', label: 'Block', size: 30 },
  FOUL: { glyph: 'F', label: 'Foul', size: 30 },
  PASS: { glyph: '↗', label: 'Pass', size: 30 },
  HANDOFF: { glyph: 'H', label: 'Handoff', size: 30 },
  TURNOVER: { glyph: '!', label: 'Turnover', size: 30 },
  POSSESSION: { glyph: '●', label: 'Possession change', size: 30 },
  BALL_LOOSE: { glyph: '○', label: 'Loose ball', size: 30 },
  SPECIAL: { glyph: '★', label: 'Special event', size: 30 },
};

const eventStyle = (event) => EVENT_STYLE[event?.type] || {
  glyph: '•',
  label: event?.title || event?.type || 'Event',
  size: 28,
};

const NARRATIVE_TYPE = {
  touchdown: 'TOUCHDOWN',
  pass: 'PASS',
  interception: 'INTERCEPTION',
  handoff: 'HANDOFF',
  block: 'BLOCK',
  foul: 'FOUL',
  chainsaw_foul: 'FOUL',
  kick_off_table: 'KICKOFF',
  weather_roll: 'WEATHER',
  turn_end: 'TURNOVER',
  possession_changed: 'POSSESSION',
  ball_loose: 'BALL_LOOSE',
};

const humanizeType = (type = '') => type
  .split('_')
  .filter(Boolean)
  .map((part) => part[0]?.toUpperCase() + part.slice(1))
  .join(' ');

const narrativeDisplayEvents = (timeline) => {
  
  if (timeline?.format !== 'pybb3-narrative-timeline') return [];

  const players = new Map(
    (timeline?.match?.players || []).map((player) => [String(player.id), player]),
  );
  const resolveParticipant = (participant) => {
    if (!participant) return null;
    const player = participant.kind === 'player'
      ? players.get(String(participant.id))
      : null;
    return {
      ...participant,
      name: participant.name || player?.name,
      teamId: participant.team_id ?? player?.team_id,
    };
  };

  return (timeline.events || [])
    // The match-card timeline is an overview, not a replay action log.
    // Routine movement is far too frequent and does not add useful overview
    // information. The canonical pybb3 timeline remains unmodified and is
    // still available for AI/narrative use and detailed inspection.
    .filter((event) => event?.type?.toLowerCase() !== 'move')
    .map((event, index) => {

    const actor = resolveParticipant(event.actor);
    const target = resolveParticipant(event.target);
    const effects = Array.isArray(event.effects) ? event.effects : [];
    const firstRoll = event?.details?.roll
      || effects.map((effect) => effect?.details).find((details) => details?.dice);
    const displayType = event.type === 'turn_end' && event.outcome !== 'turnover'
      ? 'SPECIAL'
      : NARRATIVE_TYPE[event.type] || 'SPECIAL';

    return {
      id: `pybb3-${event.id ?? index}`,
      type: displayType,
      title: humanizeType(event.type),
      sequence: Number(event.id ?? index),
      eventIndex: index,
      clock: event.clock,
      half: event.half,
      drive: event.drive,
      turn: event.team_turn ?? event.turn,
      activeTeamId: event.team_id,
      teamId: actor?.teamId ?? event.team_id,
      playerId: actor?.id,
      playerName: actor?.name,
      actorPlayerName: actor?.name,
      affectedPlayerName: target?.name,
      rawEventType: event.type,
      details: {
        ...(event.details || {}),
        result: event.outcome,
        dice: firstRoll?.dice,
        effects,
      },
    };
  });
};

const teamIndex = (event) => {
  for (const value of [event?.teamIndex, event?.sourceTeamId, event?.teamId]) {
    if (typeof value === 'number' && Number.isInteger(value)) return value;
    if (typeof value === 'string' && /^\d+$/.test(value)) return Number(value);
  }
  return -1;
};

const teamName = (match, index) => match?.teams?.[index]?.name || `Team ${index + 1}`;

const teamLogoUrl = (match, index) => {
  const team = match?.teams?.[index];
  return team?.logo ? imageUrls.logo(team.logo, team?.id?.opus) : null;
};

const MATCH_WIDE_TYPES = new Set(['KICKOFF', 'WEATHER']);

const laneTeamIndex = (event) => {
  const explicit = teamIndex(event);
  if (explicit >= 0) return explicit;
  if (MATCH_WIDE_TYPES.has(event?.type)) return -1;

  // Old analyses may predate player->team enrichment. activeTeam is a safe
  // display fallback for actor-owned events, but deliberately not for
  // injury/casualty chains where activeTeam can identify the wrong side.
  if (['TOUCHDOWN', 'COMPLETION', 'INTERCEPTION', 'EJECTION', 'KICKOFF_DETAIL'].includes(event?.type)) {
    const active = Number(event?.activeTeamId);
    if (active === 0 || active === 1) return active;
  }
  return -1;
};

const timelinePosition = (event) => [
  event?.half ? `Half ${event.half}` : null,
  event?.drive ? `Drive ${event.drive}` : null,
  event?.turn != null ? `Turn ${event.turn}` : null,
].filter(Boolean).join(' · ');

const diceExpression = (details = {}) => {
  const dice = details.dice || [];
  if (!dice.length) return null;
  const raw = dice.join(' + ');
  const total = details.rawTotal ?? dice.reduce((sum, value) => sum + Number(value || 0), 0);
  if (details.modifiedTotal != null && details.modifiedTotal !== total) {
    return `${raw} = ${total} → ${details.modifiedTotal}`;
  }
  return dice.length > 1 ? `${raw} = ${total}` : raw;
};

const eventResult = (event) => {
  const details = event?.details || {};
  return details.resultName
    || details.weather
    || details.result
    || (details.resultId != null ? `Result ${details.resultId}` : null);
};

const eventPeople = (event) => {
  const details = event?.details || {};
  const actor = event.actorPlayerName || details.causingPlayerName || event.playerName;
  const affected = event.affectedPlayerName || details.affectedPlayerName || details.injuredPlayerName || details.targetPlayerName;

  let lines = [];
  switch (event?.type) {
    case 'TOUCHDOWN':
      lines = [details.scorerName || actor ? `Scorer: ${details.scorerName || actor}` : null];
      break;
    case 'COMPLETION':
      lines = [
        details.throwerName || actor ? `Thrower: ${details.throwerName || actor}` : null,
        details.receiverName || affected ? `Receiver: ${details.receiverName || affected}` : null,
      ];
      break;
    case 'INTERCEPTION':
      lines = [details.interceptorName || actor ? `Interceptor: ${details.interceptorName || actor}` : null];
      break;
    case 'CASUALTY':
      lines = [
        actor ? `Caused by: ${actor}` : null,
        affected ? `Injured: ${affected}` : null,
      ];
      break;
    case 'APOTHECARY':
      lines = [
        affected ? `Player: ${affected}` : null,
        details.originalCasualtyResult != null ? `Original casualty: ${details.originalCasualtyResult}` : null,
        details.apothecaryRerollResult != null ? `Apothecary reroll: ${details.apothecaryRerollResult}` : null,
        details.chosenCasualtyResult != null ? `Chosen casualty: ${details.chosenCasualtyResult}` : null,
      ];
      break;
    case 'INJURY':
    case 'KO':
    case 'DEATH':
      lines = [
        affected ? `Player: ${affected}` : null,
        actor && actor !== affected ? `Caused by: ${actor}` : null,
      ];
      break;
    case 'EJECTION':
      lines = [actor ? `Ejected: ${actor}` : null];
      break;
    default:
      lines = [
        event.playerName && `Player: ${event.playerName}`,
        details.playerName && `Player: ${details.playerName}`,
        actor && `Actor: ${actor}`,
        affected && `Target: ${affected}`,
      ];
  }

  if (event?.type !== 'APOTHECARY' && details.sourceActionType) {
    lines.push(`From: ${details.sourceActionType}${details.selfInflicted ? ' (self-inflicted)' : ''}`);
  }
  lines = [...new Set(lines.filter(Boolean))];
  if (!lines.length && event.playerId != null) lines.push(`Replay player ID: ${event.playerId}`);
  return lines;
};

const chronological = (left, right) =>
  Number(left?.sequence || 0) - Number(right?.sequence || 0)
  || Number(left?.eventIndex || 0) - Number(right?.eventIndex || 0);

const turnKey = (event) => {
  const half = Number(event?.half);
  const turn = Number(event?.turn);
  return (half === 1 || half === 2) && Number.isInteger(turn) && turn >= 1 && turn <= 8
    ? `${half}:${turn}`
    : null;
};

const buildTurnRanges = (events) => events.reduce((ranges, event) => {
  const key = turnKey(event);
  const sequence = Number(event?.sequence);
  if (!key || !Number.isFinite(sequence)) return ranges;
  const current = ranges.get(key);
  ranges.set(key, current
    ? { min: Math.min(current.min, sequence), max: Math.max(current.max, sequence) }
    : { min: sequence, max: sequence });
  return ranges;
}, new Map());

const logicalPosition = (event, turnRanges, minSequence, maxSequence) => {
  const half = Number(event?.half);
  const turn = Number(event?.turn);
  const sequence = Number(event?.sequence);
  const key = turnKey(event);

  if (key) {
    // Turn N occupies the interval between ticks N-1 and N. Sequence is used
    // only inside that interval, preserving both Blood Bowl turn structure and
    // the actual replay order of multiple events in the same turn.
    const segment = 100 / 16;
    const segmentIndex = (half - 1) * 8 + (turn - 1);
    const start = segmentIndex * segment;
    const range = turnRanges.get(key);
    const fraction = range && range.max > range.min && Number.isFinite(sequence)
      ? (sequence - range.min) / (range.max - range.min)
      : 0.5;
    const insetFraction = 0.16 + Math.max(0, Math.min(1, fraction)) * 0.68;
    return start + insetFraction * segment;
  }

  // Kick-off before turn 1 sits just inside the relevant half rather than on
  // top of 1H/HT. This also creates a small visual dead zone around halftime.
  if ((half === 1 || half === 2) && turn === 0) {
    return half === 1 ? 1.8 : 51.8;
  }

  if (Number.isFinite(sequence) && maxSequence > minSequence) {
    let position = 2 + ((sequence - minSequence) / (maxSequence - minSequence)) * 96;
    if (position > 48.3 && position < 51.7) {
      position = position < 50 ? 48.3 : 51.7;
    }
    return position;
  }

  return 50;
};

function TeamWatermark({ match, index }) {
  const logo = teamLogoUrl(match, index);
  const upper = index === 0;

  return <HStack
    position="absolute"
    left="50%"
    top={upper ? '8%' : 'auto'}
    bottom={upper ? 'auto' : '8%'}
    transform="translateX(-50%)"
    spacing={3}
    opacity={0.09}
    pointerEvents="none"
    userSelect="none"
    zIndex={0}
    maxW="88%"
    justify="center"
  >
    {logo && <Image
      src={logo}
      boxSize={{ base: '42px', md: '58px' }}
      objectFit="contain"
      filter="grayscale(1)"
      alt=""
    />}
    <Text
      fontSize={{ base: 'lg', md: '2xl' }}
      fontWeight="black"
      textTransform="uppercase"
      letterSpacing="wide"
      noOfLines={1}
    >
      {teamName(match, index)}
    </Text>
  </HStack>;
}

function EventTooltip({ event, match, children }) {
  const index = laneTeamIndex(event);
  const details = event.details || {};
  const roll = diceExpression(details);
  const result = eventResult(event);
  const people = eventPeople(event);

  return <Tooltip
    hasArrow
    placement={index === 1 ? 'bottom' : 'top'}
    label={<Box maxW="340px" p={1}>
      <Text fontWeight="bold">{event.title || eventStyle(event).label}</Text>
      <Text fontSize="xs">{timelinePosition(event) || `Replay step ${event.sequence}`}</Text>
      {index >= 0 && <Text fontSize="sm" mt={1}>{teamName(match, index)}</Text>}
      {people.map((line) => <Text key={line} fontSize="sm">{line}</Text>)}
      {roll && <Text fontSize="sm" mt={1}>Roll: {roll}</Text>}
      {result && <Text fontSize="sm">Result: {result}</Text>}
      {event.score && <Text fontSize="sm">Score: {event.score.home}–{event.score.away}</Text>}
      {event.sppAwarded != null && <Text fontSize="sm" fontWeight="bold">+{event.sppAwarded} SPP</Text>}
      {details.tableName && <Text fontSize="xs" mt={1}>{details.tableName} table</Text>}
    </Box>}
  >
    {children}
  </Tooltip>;
}

function TimelineMarker({ event, match, left, laneOffset = 0 }) {
  const index = laneTeamIndex(event);
  const neutral = index < 0 || MATCH_WIDE_TYPES.has(event.type);
  const style = eventStyle(event);
  const top = neutral ? 50 : index === 0 ? 24 : 76;
  const direction = neutral ? 0 : index === 0 ? 1 : -1;
  const connectorHeight = neutral ? 0 : 23;

  return <Box
    position="absolute"
    left={`${left}%`}
    top={`${top}%`}
    transform={`translate(-50%, -50%) translateX(${laneOffset * 8}px)`}
    zIndex={3}
  >
    {!neutral && <Box
      position="absolute"
      left="50%"
      top={direction > 0 ? '50%' : 'auto'}
      bottom={direction < 0 ? '50%' : 'auto'}
      transform="translateX(-50%)"
      h={`${connectorHeight}px`}
      borderLeftWidth="1px"
      borderColor="gray.400"
      zIndex={-1}
    />}
    <EventTooltip event={event} match={match}>
      <Box
        as="button"
        type="button"
        aria-label={`${style.label}: ${timelinePosition(event) || `replay step ${event.sequence}`}`}
        w={`${style.size}px`}
        h={`${style.size}px`}
        borderRadius="full"
        borderWidth="2px"
        borderColor={event.sppAwarded != null ? 'purple.400' : neutral ? 'gray.400' : 'gray.500'}
        bg={event.sppAwarded != null ? 'purple.50' : 'white'}
        color="gray.800"
        fontWeight="bold"
        fontSize={style.glyph.length > 1 ? '10px' : '18px'}
        lineHeight="1"
        display="flex"
        alignItems="center"
        justifyContent="center"
        boxShadow="sm"
        _dark={{
          bg: event.sppAwarded != null ? 'purple.900' : 'gray.700',
          color: 'white',
          borderColor: event.sppAwarded != null ? 'purple.300' : 'gray.500',
        }}
        _hover={{ transform: 'scale(1.12)', boxShadow: 'md' }}
        transition="transform 0.12s ease, box-shadow 0.12s ease"
      >
        {style.glyph}
      </Box>
    </EventTooltip>
    {event.sppAwarded != null && <Badge
      position="absolute"
      top="-9px"
      right="-12px"
      borderRadius="full"
      colorScheme="purple"
      fontSize="9px"
      px={1}
    >
      +{event.sppAwarded}
    </Badge>}
  </Box>;
}

function DetailedEvent({ event, match, children }) {
  const index = laneTeamIndex(event);
  const details = event.details || {};
  const roll = diceExpression(details);
  const result = eventResult(event);
  const people = eventPeople(event);

  return <Box
    borderLeftWidth="3px"
    borderColor={index === 0 ? 'blue.300' : index === 1 ? 'orange.300' : 'gray.300'}
    pl={3}
    py={2}
  >
    <HStack flexWrap="wrap" spacing={2}>
      <Text fontSize="xs" color="gray.500">{timelinePosition(event) || `Replay step ${event.sequence}`}</Text>
      {index >= 0 && <Badge>{teamName(match, index)}</Badge>}
      {event.sppAwarded != null && <Badge colorScheme="purple">+{event.sppAwarded} SPP</Badge>}
    </HStack>
    <Text fontWeight="semibold">{event.title || eventStyle(event).label}</Text>
    {people.map((line) => <Text key={line} fontSize="sm">{line}</Text>)}
    {roll && <Text fontSize="sm">Roll: {roll}</Text>}
    {result && <Text fontSize="sm">Result: {result}</Text>}
    {event.score && <Text fontSize="sm">Score: {event.score.home}–{event.score.away}</Text>}
    {children}
    <Text fontSize="10px" color="gray.400" mt={1}>{event.rawEventType}</Text>
  </Box>;
}

export default function MatchTimelineBar({ timeline, events = [], match }) {
  const [logOpen, setLogOpen] = React.useState(false);
  const sourceEvents = timeline?.format === 'pybb3-narrative-timeline'
    ? narrativeDisplayEvents(timeline)
    : events;
  if (!sourceEvents.length) return null;

  const ordered = [...sourceEvents].sort(chronological);
  const sequences = ordered
    .map((event) => Number(event.sequence))
    .filter((value) => Number.isFinite(value));
  const minSequence = sequences.length ? Math.min(...sequences) : 0;
  const maxSequence = sequences.length ? Math.max(...sequences) : 1;
  const turnRanges = buildTurnRanges(ordered);

  const childrenByParent = ordered.reduce((map, event) => {
    if (!event.parentEventId) return map;
    map[event.parentEventId] = [...(map[event.parentEventId] || []), event];
    return map;
  }, {});

  // One marker per important chain. Weather remains visible even when it was
  // produced by a kick-off event.
  const markers = ordered.filter((event) => !event.parentEventId || event.type === 'WEATHER');
  const occupancy = new Map();

  const markerData = markers.map((event) => {
    const left = logicalPosition(event, turnRanges, minSequence, maxSequence);
    const lane = MATCH_WIDE_TYPES.has(event.type) ? 'neutral' : laneTeamIndex(event);
    const key = `${lane}:${Math.round(left / 2)}`;
    const offset = occupancy.get(key) || 0;
    occupancy.set(key, offset + 1);
    return { event, left, laneOffset: offset };
  });

  const rootEvents = ordered.filter((event) => !event.parentEventId);
  const ticks = Array.from({ length: 17 }, (_, index) => index);

  return <Box>
    <HStack justify="space-between" mb={2}>
      <Box>
        <Text fontWeight="semibold">Match timeline</Text>
        <Text fontSize="sm" color="gray.500">Home above, away below. Hover a marker for event details.</Text>
      </Box>
      <HStack fontSize="xs" color="gray.500">
        <Text>Home ↑</Text>
        <Text>↓ Away</Text>
      </HStack>
    </HStack>

    <Box
      position="relative"
      h={{ base: '150px', md: '180px' }}
      mx={{ base: 2, md: 5 }}
      mb={2}
      overflow="visible"
    >
      <TeamWatermark match={match} index={0}/>
      <TeamWatermark match={match} index={1}/>

      <Box
        position="absolute"
        left="0"
        right="0"
        top="50%"
        borderTopWidth="2px"
        borderColor="gray.400"
      />
      <Box
        position="absolute"
        left="50%"
        top="42%"
        bottom="42%"
        borderLeftWidth="2px"
        borderColor="gray.500"
      />

      {ticks.map((tick) => {
        const left = (tick / 16) * 100;
        const isHalf = tick === 0 || tick === 8 || tick === 16;
        const label = tick === 0 ? '1H' : tick === 8 ? 'HT' : tick === 16 ? 'FT' : tick < 8 ? String(tick) : String(tick - 8);
        return <Box key={tick} position="absolute" left={`${left}%`} top="50%" transform="translate(-50%, -50%)" zIndex={1}>
          <Box h={isHalf ? '14px' : '8px'} borderLeftWidth={isHalf ? '2px' : '1px'} borderColor="gray.400"/>
          <Text
            position="absolute"
            top="10px"
            left="50%"
            transform="translateX(-50%)"
            fontSize="9px"
            color="gray.500"
            display={{ base: isHalf ? 'block' : 'none', md: 'block' }}
          >
            {label}
          </Text>
        </Box>;
      })}

      {markerData.map(({ event, left, laneOffset }) => <TimelineMarker
        key={event.id || `${event.sequence}-${event.eventIndex}-${event.type}`}
        event={event}
        match={match}
        left={left}
        laneOffset={laneOffset}
      />)}
    </Box>

    <Button size="sm" variant="ghost" onClick={() => setLogOpen((open) => !open)}>
      {logOpen ? 'Hide detailed match log' : 'Show detailed match log'}
    </Button>
    <Collapse in={logOpen} animateOpacity>
      <VStack align="stretch" spacing={1} mt={2}>
        {rootEvents.map((event) => <DetailedEvent
          key={event.id || `${event.sequence}-${event.eventIndex}-${event.type}`}
          event={event}
          match={match}
        >
          {(childrenByParent[event.id] || []).map((child) => <Box key={child.id} ml={4} mt={1}>
            <DetailedEvent event={child} match={match}/>
          </Box>)}
        </DetailedEvent>)}
      </VStack>
    </Collapse>
  </Box>;
}
