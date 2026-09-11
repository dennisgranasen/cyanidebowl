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
import TimelineIcon from './TimelineIcon';

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
  MATCH_START: { glyph: 'S', label: 'Match start', size: 30 },
  BLOCK: { glyph: 'B', label: 'Block', size: 30 },
  FOUL: { glyph: 'F', label: 'Foul', size: 30 },
  PASS: { glyph: '↗', label: 'Pass', size: 30 },
  COMPLETION: { glyph: '↗', label: 'Completion', size: 30 },
  CATCH: { glyph: 'C', label: 'Catch', size: 30 },
  HANDOFF: { glyph: 'H', label: 'Handoff', size: 30 },
  ANIMAL_SAVAGERY: { glyph: 'A', label: 'Animal Savagery', size: 30 },
  CHECK: { glyph: '×', label: 'Failed check', size: 30 },
  REROLL: { glyph: '⚄', label: 'Reroll', size: 30 },
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
  match_start: 'MATCH_START',
  touchdown: 'TOUCHDOWN',
  pass: 'PASS',
  completion: 'COMPLETION',
  catch: 'CATCH',
  interception: 'INTERCEPTION',
  handoff: 'HANDOFF',
  block: 'BLOCK',
  foul: 'FOUL',
  chainsaw_foul: 'FOUL',
  ejection: 'EJECTION',
  sent_off: 'EJECTION',
  casualty: 'CASUALTY',
  injury: 'INJURY',
  death: 'DEATH',
  animal_savagery: 'ANIMAL_SAVAGERY',
  kick_off_table: 'KICKOFF',
  weather_roll: 'WEATHER',
  turn_end: 'TURNOVER',
  possession_gained: 'POSSESSION',
  possession_changed: 'POSSESSION',
  ball_loose: 'BALL_LOOSE',
};

const humanizeType = (type = '') => type
  .split('_')
  .filter(Boolean)
  .map((part) => part[0]?.toUpperCase() + part.slice(1))
  .join(' ');

const normalizedNarrativeType = (event) =>
  String(event?.type || '').trim().toLowerCase();

const checkOutcome = (check) => {
  if (check?.outcome != null) return String(check.outcome).trim().toLowerCase();
  const attempts = Array.isArray(check?.attempts) ? check.attempts : [];
  const last = attempts.length ? attempts[attempts.length - 1] : null;
  return last?.outcome == null ? '' : String(last.outcome).trim().toLowerCase();
};

const isFailedCheck = (check) => checkOutcome(check) === 'failed';

const blockHasMajorConsequence = (event) => {
  const effects = Array.isArray(event?.effects) ? event.effects : [];
  const checks = Array.isArray(event?.checks) ? event.checks : [];

  if (effects.some((effect) => {
    const type = String(effect?.type || '').trim().toLowerCase();
    const outcome = String(effect?.outcome || '').trim().toLowerCase();
    if (type === 'casualty' || type === 'player_removed') return true;
    return type === 'injury' && ['ko', 'casualty', 'dead', 'death', 'badly_hurt'].includes(outcome);
  })) {
    return true;
  }

  return checks.some((check) => {
    const type = String(check?.type || '').trim().toLowerCase();
    const outcome = checkOutcome(check);
    return type === 'injury' && ['ko', 'casualty', 'dead', 'death', 'badly_hurt'].includes(outcome);
  });
};

const sameNarrativeTurn = (left, right) =>
  left?.half === right?.half
  && left?.drive === right?.drive
  && (left?.team_turn ?? left?.turn) === (right?.team_turn ?? right?.turn);

const eventCausesTurnover = (event, index, events) => {
  const eventId = String(event?.id ?? '');

  if (events.some((candidate) =>
    normalizedNarrativeType(candidate) === 'turn_end'
    && String(candidate?.outcome || '').trim().toLowerCase() === 'turnover'
    && String(candidate?.caused_by ?? candidate?.details?.caused_by ?? '') === eventId)) {
    return true;
  }

  for (let offset = index + 1; offset < Math.min(events.length, index + 6); offset += 1) {
    const candidate = events[offset];
    if (!sameNarrativeTurn(event, candidate)) break;

    const type = normalizedNarrativeType(candidate);
    if (type === 'turn_end') {
      return String(candidate?.outcome || '').trim().toLowerCase() === 'turnover';
    }

    if (!['ball_loose', 'bounce', 'scatter', 'possession_changed', 'possession_gained'].includes(type)) {
      break;
    }
  }
  return false;
};

const keepInOverviewTimeline = (event, index, events) => {
  const type = normalizedNarrativeType(event);
  const checks = Array.isArray(event?.checks) ? event.checks : [];

  if (type === 'move') return checks.some(isFailedCheck);
  if ([
    'bounce',
    'scatter',
    'kickoff_deviation',
    'stand_up',
    'face_up_stunned_players',
    'turn_end',
  ].includes(type)) {
    return false;
  }
  if (type === 'block') {
    return blockHasMajorConsequence(event) || eventCausesTurnover(event, index, events);
  }
  return true;
};

const rerollDisplayEvents = (event, index, resolveParticipant) => {
  const actor = resolveParticipant(event.actor);
  const checks = Array.isArray(event?.checks) ? event.checks : [];
  const result = [];

  checks.forEach((check, checkIndex) => {
    const attempts = Array.isArray(check?.attempts) ? check.attempts : [];
    const rerolledAttempts = attempts
      .map((attempt, attemptIndex) => ({ attempt, attemptIndex }))
      .filter(({ attempt }) => attempt?.reroll);

    const markers = rerolledAttempts.length
      ? rerolledAttempts
      : check?.reroll_used
        ? [{ attempt: null, attemptIndex: attempts.length > 1 ? attempts.length - 1 : 1 }]
        : [];

    markers.forEach(({ attempt, attemptIndex }, markerIndex) => {
      const source = attempt?.reroll ? humanizeType(String(attempt.reroll)) : 'Team';
      const checkName = humanizeType(check?.type || 'check');

      result.push({
        id: `pybb3-${event.id ?? index}-reroll-${checkIndex}-${markerIndex}`,
        type: 'REROLL',
        title: `${source} reroll · ${checkName}`,
        sequence: index,
        replayEventId: event.id,
        eventIndex: index + ((checkIndex + 1) / 100) + ((attemptIndex + 1) / 10000),
        clock: event.clock,
        half: event.half,
        drive: event.drive,
        turn: event.team_turn ?? event.turn,
        activeTeamId: event.team_id,
        teamId: actor?.teamId ?? event.team_id,
        playerId: actor?.id,
        playerName: actor?.name,
        actorPlayerName: actor?.name,
        rawEventType: 'reroll',
        checks: [check],
        details: {
          rerollSource: source.toLowerCase(),
          result: attempt?.outcome ?? check?.outcome,
        },
      });
    });
  });

  return result;
};

const checkSummary = (check) => {
  const label = humanizeType(check?.type || 'check');
  const required = check?.required != null ? ` ${check.required}+` : '';
  const attempts = Array.isArray(check?.attempts) ? check.attempts : [];
  const renderedAttempts = attempts.map((attempt) => {
    const dice = Array.isArray(attempt?.dice) ? attempt.dice.join('+') : '?';
    const outcome = attempt?.outcome != null ? ` ${humanizeType(String(attempt.outcome))}` : '';
    const reroll = attempt?.reroll ? ` (${humanizeType(String(attempt.reroll))} reroll)` : '';
    return `${dice}${outcome}${reroll}`;
  });
  return renderedAttempts.length
    ? `${label}${required}: ${renderedAttempts.join(' → ')}`
    : `${label}${required}${check?.outcome != null ? `: ${humanizeType(String(check.outcome))}` : ''}`;
};

const KICKOFF_DETAIL_TYPES = new Set([
  'blitz',
  'brilliant_coaching',
  'changing_weather',
  'cheering_fans',
  'get_the_ref',
  'high_kick',
  'officious_ref',
  'perfect_defence',
  'perfect_defense',
  'pitch_invasion',
  'quick_snap',
  'riot',
  'throw_a_rock',
]);

const overviewResultLabel = (event) => {
  const details = event?.details || {};
  return details.resultName
    || details.weather
    || details.weatherName
    || details.result
    || event?.outcome
    || null;
};

const DAMAGE_TYPES = new Set(['INJURY', 'CASUALTY', 'DEATH']);

const damageConsequence = (event) => ({
  type: event.type,
  playerName: event.affectedPlayerName || event.playerName || null,
  result: eventResult(event),
  rawEventType: event.rawEventType,
});

const collapseDamageChains = (events) => {
  const byReplayId = new Map(
    events
      .filter((event) => event.replayEventId != null)
      .map((event) => [String(event.replayEventId), event]),
  );
  const suppressed = new Set();
  const consequencesByRoot = new Map();

  const rootCause = (event) => {
    let current = event;
    const seen = new Set();
    while (current?.causedByReplayEventId != null) {
      const key = String(current.causedByReplayEventId);
      if (seen.has(key)) break;
      seen.add(key);
      const parent = byReplayId.get(key);
      if (!parent) break;
      current = parent;
    }
    return current;
  };

  events.forEach((event) => {
    if (!DAMAGE_TYPES.has(event.type) || event.causedByReplayEventId == null) return;
    const root = rootCause(event);
    if (!root || root === event || DAMAGE_TYPES.has(root.type)) return;

    suppressed.add(event.id);
    const previous = consequencesByRoot.get(root.id) || [];
    const consequence = damageConsequence(event);
    const duplicate = previous.some((item) =>
      item.type === consequence.type
      && item.playerName === consequence.playerName
      && item.result === consequence.result);
    if (!duplicate) consequencesByRoot.set(root.id, [...previous, consequence]);
  });

  return events
    .filter((event) => !suppressed.has(event.id))
    .map((event) => {
      const consequences = consequencesByRoot.get(event.id);
      if (!consequences?.length) return event;
      return {
        ...event,
        details: {
          ...(event.details || {}),
          consequences: [
            ...((event.details || {}).consequences || []),
            ...consequences,
          ],
        },
      };
    });
};

const groupOverviewEvents = (events) => {
  const skipped = new Set();
  const grouped = [];
  let initialWeatherGrouped = false;

  for (let index = 0; index < events.length; index += 1) {
    if (skipped.has(index)) continue;
    const event = events[index];
    const rawType = normalizedNarrativeType({ type: event.rawEventType });

    if (rawType === 'match_start' && !initialWeatherGrouped) {
      const weatherIndex = events.findIndex((candidate, candidateIndex) =>
        candidateIndex > index
        && !skipped.has(candidateIndex)
        && normalizedNarrativeType({ type: candidate.rawEventType }) === 'weather_roll'
        && Number(candidate.turn ?? 0) === 0);

      if (weatherIndex > index) {
        const weather = events[weatherIndex];
        const weatherLabel = overviewResultLabel(weather) || 'Weather';
        skipped.add(weatherIndex);
        initialWeatherGrouped = true;
        grouped.push({
          ...event,
          type: 'WEATHER',
          rawEventType: 'match_start_weather',
          title: `Match start · ${humanizeType(String(weatherLabel))}`,
          details: {
            ...(event.details || {}),
            weather: weatherLabel,
            groupedEvents: [event.rawEventType, weather.rawEventType],
          },
        });
        continue;
      }
    }

    if (rawType === 'kick_off_table') {
      let detailIndex = -1;
      for (let candidateIndex = index + 1;
        candidateIndex < Math.min(events.length, index + 4);
        candidateIndex += 1) {
        if (skipped.has(candidateIndex)) continue;
        const candidateType = normalizedNarrativeType({
          type: events[candidateIndex].rawEventType,
        });
        if (KICKOFF_DETAIL_TYPES.has(candidateType)) {
          detailIndex = candidateIndex;
          break;
        }
        if (events[candidateIndex].type !== 'REROLL') break;
      }

      if (detailIndex > index) {
        const detail = events[detailIndex];
        const detailType = normalizedNarrativeType({ type: detail.rawEventType });
        skipped.add(detailIndex);
        grouped.push({
          ...event,
          title: `Kick-off · ${humanizeType(detailType)}`,
          details: {
            ...(event.details || {}),
            kickoffEventType: detailType,
            kickoffEventResult: overviewResultLabel(detail),
            groupedEvents: [event.rawEventType, detail.rawEventType],
          },
        });
        continue;
      }
    }

    grouped.push(event);
  }

  return grouped;
};

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

  const projected = (timeline.events || []).flatMap((event, index, events) => {
    const rerolls = rerollDisplayEvents(event, index, resolveParticipant);
    if (!keepInOverviewTimeline(event, index, events)) return rerolls;

    const actor = resolveParticipant(event.actor);
    const target = resolveParticipant(event.target);
    const effects = (Array.isArray(event.effects) ? event.effects : []).map((effect) => ({
      ...effect,
      subject: resolveParticipant(effect?.subject),
    }));
    const checks = Array.isArray(event.checks) ? event.checks : [];
    const failedCheck = checks.find(isFailedCheck);
    const rawType = normalizedNarrativeType(event);
    const damageSubject = effects
      .filter((effect) => ['injury', 'casualty', 'player_removed'].includes(
        String(effect?.type || '').trim().toLowerCase(),
      ))
      .map((effect) => effect.subject)
      .find((subject) => subject?.kind === 'player');
    const affected = target
      || damageSubject
      || (['injury', 'casualty', 'death'].includes(rawType) && actor?.kind === 'player'
        ? actor
        : null);
    const actorPlayer = actor?.kind === 'player' ? actor : null;
    const turnoverCaused = (rawType === 'move' && Boolean(failedCheck))
      || eventCausesTurnover(event, index, events);
    const displayType = rawType === 'move' && failedCheck
      ? 'CHECK'
      : NARRATIVE_TYPE[rawType] || 'SPECIAL';

    const projectedEvent = {
      id: `pybb3-${event.id ?? index}`,
      type: displayType,
      title: rawType === 'move' && failedCheck
        ? `Failed ${humanizeType(failedCheck.type)}`
        : humanizeType(event.type),
      sequence: index,
      replayEventId: event.id,
      causedByReplayEventId: event.caused_by ?? event.details?.caused_by ?? null,
      eventIndex: index,
      clock: event.clock,
      half: event.half,
      drive: event.drive,
      turn: event.team_turn ?? event.turn,
      activeTeamId: event.team_id,
      teamId: affected?.teamId ?? actorPlayer?.teamId ?? event.team_id,
      playerId: actorPlayer?.id ?? affected?.id,
      playerName: actorPlayer?.name ?? affected?.name,
      actorPlayerName: actorPlayer?.name,
      affectedPlayerName: affected?.name,
      rawEventType: event.type,
      turnoverCaused,
      checks,
      details: {
        ...(event.details || {}),
        result: event.outcome,
        effects,
        causeUnknown: ['injury', 'casualty', 'death'].includes(rawType)
          && event.caused_by == null
          && event.details?.caused_by == null
          && !event.details?.sourceActionType,
      },
    };

    return [projectedEvent, ...rerolls];
  });

  return groupOverviewEvents(collapseDamageChains(projected));
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

const displayHalf = (event) => {
  const explicit = Number(event?.half);
  if (explicit === 1 || explicit === 2) return explicit;
  const turn = Number(event?.turn);
  if (Number.isInteger(turn) && turn >= 1 && turn <= 16) return turn <= 8 ? 1 : 2;
  return null;
};

const globalTurn = (event) => {
  const turn = Number(event?.turn);
  if (!Number.isInteger(turn) || turn < 1 || turn > 16) return null;

  // pybb3 team_turn is local to the half (1-8). The half is authoritative.
  const half = displayHalf(event);
  if ((half === 1 || half === 2) && turn <= 8) return turn + ((half - 1) * 8);
  return turn;
};

const timelinePosition = (event) => [
  displayHalf(event) ? `${displayHalf(event)}H` : null,
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

const INJURY_OUTCOMES = {
  0: 'Stunned',
  1: 'Reserve',
  2: 'KO',
  3: 'Badly Hurt',
  4: 'Casualty',
};

const CASUALTY_OUTCOMES = {
  0: 'No Casualty',
  1: 'Badly Hurt',
  2: 'Seriously Hurt',
  3: 'Serious Injury',
  4: 'Lasting Injury',
  5: 'Smashed Knee',
  6: 'Head Injury',
  7: 'Broken Arm',
  8: 'Neck Injury',
  9: 'Dislocated Shoulder',
  10: 'Dead',
};

const damageOutcomeLabel = (event) => {
  const details = event?.details || {};
  const rawType = String(event?.rawEventType || event?.type || '').trim().toLowerCase();
  const value = details.result ?? details.resultId;
  const numeric = Number(value);

  if (rawType === 'injury' || event?.type === 'INJURY') {
    const injury = Number.isInteger(numeric) && INJURY_OUTCOMES[numeric] != null
      ? INJURY_OUTCOMES[numeric]
      : typeof value === 'string' && value
        ? humanizeType(value)
        : null;
    const casualty = (details.effects || []).find((effect) =>
      String(effect?.type || '').trim().toLowerCase() === 'casualty');
    if (injury === 'Casualty' && casualty?.outcome != null) {
      const casualtyNumeric = Number(casualty.outcome);
      const casualtyLabel = Number.isInteger(casualtyNumeric)
        && CASUALTY_OUTCOMES[casualtyNumeric] != null
        ? CASUALTY_OUTCOMES[casualtyNumeric]
        : humanizeType(String(casualty.outcome));
      return `Casualty · ${casualtyLabel}`;
    }
    return injury;
  }

  if (rawType === 'casualty' || event?.type === 'CASUALTY') {
    if (Number.isInteger(numeric) && CASUALTY_OUTCOMES[numeric] != null) {
      return CASUALTY_OUTCOMES[numeric];
    }
    return typeof value === 'string' && value ? humanizeType(value) : null;
  }

  if (rawType === 'death' || event?.type === 'DEATH') return 'Dead';
  return null;
};

const eventResult = (event) => {
  const details = event?.details || {};
  return damageOutcomeLabel(event)
    || details.resultName
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
  (details.consequences || []).forEach((consequence) => {
    const result = consequence.result ? ` · ${humanizeType(String(consequence.result))}` : '';
    if (consequence.playerName) {
      lines.push(`${humanizeType(consequence.type)}: ${consequence.playerName}${result}`);
    }
  });
  if (details.causeUnknown && ['INJURY', 'CASUALTY', 'DEATH'].includes(event?.type)) {
    lines.push('Cause: Unknown');
  }
  lines = [...new Set(lines.filter(Boolean))];
  if (!lines.length && event.playerId != null) lines.push(`Replay player ID: ${event.playerId}`);
  return lines;
};

const chronological = (left, right) =>
  Number(left?.sequence || 0) - Number(right?.sequence || 0)
  || Number(left?.eventIndex || 0) - Number(right?.eventIndex || 0);

const turnKey = (event) => {
  const turn = globalTurn(event);
  return turn != null ? `turn:${turn}` : null;
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
  const half = displayHalf(event);
  const turn = globalTurn(event);
  const sequence = Number(event?.sequence);
  const key = turnKey(event);

  if (key) {
    // Turns 1–16 occupy the sixteen timeline segments. pybb3 exposes the
    // second half as global turns 9–16.
    const segment = 100 / 16;
    const segmentIndex = turn - 1;
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
  const checks = Array.isArray(event.checks) ? event.checks : [];
  const roll = checks.length ? null : diceExpression(details);
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
      {checks.map((check, checkIndex) => (
        <Text key={`${check.type || 'check'}-${checkIndex}`} fontSize="sm" mt={checkIndex === 0 ? 1 : 0}>
          {checkSummary(check)}
        </Text>
      ))}
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
        borderColor={event.turnoverCaused
          ? 'red.500'
          : event.sppAwarded != null
            ? 'purple.400'
            : neutral ? 'gray.400' : 'gray.500'}
        bg={event.sppAwarded != null ? 'purple.50' : 'white'}
        color="gray.800"
        fontWeight="bold"
        fontSize="18px"
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
        <TimelineIcon event={event} size={Math.max(18, style.size - 10)}/>
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
  const checks = Array.isArray(event.checks) ? event.checks : [];
  const roll = checks.length ? null : diceExpression(details);
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
    {checks.map((check, checkIndex) => (
      <Text key={`${check.type || 'check'}-${checkIndex}`} fontSize="sm">
        {checkSummary(check)}
      </Text>
    ))}
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
