import React, { useEffect, useState } from 'react';
import { Alert, AlertIcon, Box, Button, ButtonGroup, Checkbox, HStack, Image, Slider, SliderFilledTrack,
  SliderThumb, SliderTrack, Spinner, Text, VStack } from '@chakra-ui/react';
import { FiChevronLeft, FiChevronRight, FiPause, FiPlay, FiSkipBack, FiSkipForward } from 'react-icons/fi';
import WarpScoresApiService from '../../WarpScoresApiService';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';

const onPitch = (position, width, height) => position
  && position.x >= 0 && position.x < width && position.y >= 0 && position.y < height;

const positionStyle = (position, width, height) => ({
  left: `${((position.x + 0.5) / width) * 100}%`,
  top: `${((position.y + 0.5) / height) * 100}%`,
});

const rosterPlayerFor = (player, match) => match?.teams?.[Number(player.team)]?.players
  ?.find((candidate) => Number(candidate.number) === Number(player.number));

const skillNames = (player) => [
  ...(player?.skillStrings || []),
  ...(player?.skills?.acquiredSkills || []),
  ...(player?.skills?.innateSkills || []),
].map((skill) => String(skill).toLowerCase());

const ringSize = (player, match) => {
  const rosterPlayer = rosterPlayerFor(player, match);
  const skills = [...(player.traits || []), ...skillNames(rosterPlayer)].map((skill) => String(skill).toLowerCase());
  if (skills.some((skill) => skill.includes('titchy'))) return { base: '14px', md: '19px' };
  if (skills.some((skill) => skill.includes('stunty'))) return { base: '16px', md: '22px' };
  const strength = Number(player.strength ?? rosterPlayer?.extendedAttributes?.st?.value ?? rosterPlayer?.attributes?.st ?? 3);
  if (strength >= 5) return { base: '27px', md: '36px' };
  if (strength === 4) return { base: '23px', md: '30px' };
  return { base: '19px', md: '26px' };
};

const roleFor = (player, match) => {
  const rosterPlayer = rosterPlayerFor(player, match);
  const type = String(player.positionType || rosterPlayer?.type || '').toLowerCase();
  if (type.includes('star')) return 'star';
  if (type.includes('blitzer')) return 'blitzer';
  if (type.includes('thrower') || type.includes('passer')) return 'thrower';
  if (type.includes('runner') || type.includes('catcher') || type.includes('receiver')) return 'runner';
  return 'lineman';
};

const roleStyle = {
  blitzer: { color: 'red.500', label: 'B' },
  lineman: { color: 'gray.500', label: 'L' },
  thrower: { color: 'white', label: 'T', text: 'gray.900' },
  runner: { color: 'yellow.400', label: 'R', text: 'gray.900' },
  star: { color: 'yellow.500', label: 'S', text: 'gray.900' },
};

export default function ReplayPitchPlayer({ matchId, match }) {
  const [replay, setReplay] = useState(null);
  const [frameIndex, setFrameIndex] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [error, setError] = useState('');
  const [showGrid, setShowGrid] = useState(false);
  const [playbackMode, setPlaybackMode] = useState('turn');
  const [preferences, setPreferences] = useState({ replayPlayerLabel: 'number', replayPlayerVisual: 'rings' });
  const { isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];

  useEffect(() => {
    let active = true;
    setReplay(null); setFrameIndex(0); setPlaying(false); setError('');
    WarpScoresApiService.replayFrames(matchId)
      .then((result) => { if (active) setReplay(result); })
      .catch(() => { if (active) setError('Kunde inte läsa planpositionerna från replayen.'); });
    return () => { active = false; };
  }, [matchId]);

  useEffect(() => {
    if (!isAuthenticated) return;
    WarpScoresApiService.userPreferences(...auth).then((saved) => {
      setPreferences((current) => ({ ...current, ...saved }));
    }).catch(() => {});
  }, [getAccessTokenSilently, getAccessTokenWithPopup, isAuthenticated]);

  const frames = replay?.frames || [];
  const playbackFrames = playbackMode === 'action' ? frames : frames.filter((candidate) => candidate.checkpoint);
  const width = replay?.width || 26;
  const height = replay?.height || 15;
  const frame = playbackFrames[frameIndex];

  useEffect(() => {
    if (!playing || playbackFrames.length < 2) return undefined;
    const timer = window.setInterval(() => {
      setFrameIndex((current) => {
        if (current >= playbackFrames.length - 1) { setPlaying(false); return current; }
        return current + 1;
      });
    }, 750);
    return () => window.clearInterval(timer);
  }, [playing, playbackFrames.length]);

  if (error) return <Alert status="warning"><AlertIcon/>{error}</Alert>;
  if (!replay) return <HStack><Spinner size="sm"/><Text>Hämtar planpositioner…</Text></HStack>;
  if (!playbackFrames.length) return <Text color="gray.500">Replayen innehåller inga sparade planlägen.</Text>;

  const playerName = (player) => player.name || `#${player.number || player.id}`;
  const teamName = (team) => match?.teams?.[team]?.name || `Lag ${Number(team) + 1}`;
  const activePlayers = frame.players.filter((player) => onPitch(player.position, width, height));
  const sideline = frame.players.filter((player) => !onPitch(player.position, width, height));
  const bench = sideline.filter((player) => !player.injured);
  const injured = sideline.filter((player) => player.injured);
  const frameLabel = [
    frame.context?.team0Turn != null ? `${teamName(0)}: tur ${frame.context.team0Turn}` : null,
    frame.context?.team1Turn != null ? `${teamName(1)}: tur ${frame.context.team1Turn}` : null,
    frame.clock != null ? `klocka ${frame.clock}` : null,
  ].filter(Boolean).join(' · ');
  const setPreference = (key, value) => {
    const updated = { ...preferences, [key]: value };
    setPreferences(updated);
    if (isAuthenticated) WarpScoresApiService.updateUserPreferences(updated, ...auth).catch(() => setPreferences(preferences));
  };

  return <VStack align="stretch" spacing={3}>
    <HStack justify="space-between" flexWrap="wrap"><Box><Text fontWeight="semibold">Taktisk replay</Text><Text fontSize="sm" color="gray.500">{playbackMode === 'turn' ? 'Tur' : 'Action'} {frameIndex + 1} av {playbackFrames.length}{frameLabel ? ` · ${frameLabel}` : ''}</Text></Box><ButtonGroup size="xs" isAttached variant="outline"><Button isActive={playbackMode === 'turn'} onClick={() => { setPlaying(false); setFrameIndex(0); setPlaybackMode('turn'); }}>Tur</Button><Button isActive={playbackMode === 'action'} onClick={() => { setPlaying(false); setFrameIndex(0); setPlaybackMode('action'); }}>Actions</Button></ButtonGroup></HStack>
    <Box position="relative" overflow="hidden" borderWidth="2px" borderColor="green.900" bg="green.700" aspectRatio={`${width} / ${height}`} aria-label="Blood Bowl-plan med spelarpositioner">
      <svg viewBox={`0 0 ${width} ${height}`} width="100%" height="100%" aria-hidden="true">
        <rect width={width} height={height} fill="#276749"/>
        {showGrid && Array.from({ length: width * height }, (_, index) => {
          const x = index % width; const y = Math.floor(index / width);
          return <rect key={`${x}-${y}`} x={x} y={y} width="1" height="1" fill={(x + y) % 2 ? "#2f855a" : "#276749"} stroke="#c6f6d5" strokeOpacity="0.22" strokeWidth="0.035"/>;
        })}
        <rect x="0" y="0" width="1" height={height} fill="#1a202c"/><rect x={width - 1} y="0" width="1" height={height} fill="#1a202c"/>
        <path d={`M1 0V${height} M13 0V${height} M25 0V${height} M0 4H${width} M0 11H${width}`} stroke="#f7fafc" strokeOpacity="0.8" strokeWidth="0.1" strokeDasharray="0.35 0.2"/>
        <rect x="0" y="0" width={width} height={height} fill="none" stroke="#f7fafc" strokeWidth="0.14"/>
      </svg>
      {activePlayers.map((player) => {
        const role = roleStyle[roleFor(player, match)];
        const symbol = preferences.replayPlayerLabel === 'position' ? role.label : (player.number || player.id);
        const size = ringSize(player, match);
        const teamColor = Number(player.team) === 0 ? 'blue.500' : 'orange.400';
        return <Box key={`${player.team}-${player.id}`} position="absolute" transform="translate(-50%, -50%)" width={size} height={size} borderRadius="50%" display="flex" alignItems="center" justifyContent="center" bg={teamColor} borderWidth={player.hasBall ? '4px' : '3px'} borderColor={player.hasBall ? 'yellow.200' : role.color} color="white" boxShadow="0 0 0 1px rgba(0,0,0,0.85)" fontWeight="bold" fontSize={{ base: '9px', md: '11px' }} title={`${playerName(player)} · ${role.label}${player.hasBall ? ' · bollbärare' : ''}`} {...positionStyle(player.position, width, height)}>
          {preferences.replayPlayerVisual === 'avatars' && player.avatarUrl ? <Image src={player.avatarUrl} alt={playerName(player)} boxSize="calc(100% - 5px)" borderRadius="full" objectFit="cover"/> : <Box color={role.text || 'white'}>{symbol}</Box>}
        </Box>;
      })}
      {onPitch(frame.ball, width, height) && <Box position="absolute" transform="translate(-50%, -50%) rotate(-28deg)" width={{ base: '16px', md: '22px' }} height={{ base: '10px', md: '14px' }} borderRadius="50%" bg="orange.700" borderWidth="2px" borderColor="white" title="Boll" display="flex" alignItems="center" justifyContent="center" {...positionStyle(frame.ball, width, height)}><Box width="70%" borderTop="1px solid" borderColor="white"/></Box>}
    </Box>
    <Slider value={frameIndex} min={0} max={playbackFrames.length - 1} step={1} onChange={setFrameIndex} aria-label="Replay checkpoint"><SliderTrack><SliderFilledTrack/></SliderTrack><SliderThumb/></Slider>
    <HStack justify="space-between" flexWrap="wrap">
      <ButtonGroup size="sm" isAttached variant="outline">
        <Button aria-label="Första checkpoint" title="Första checkpoint" onClick={() => { setPlaying(false); setFrameIndex(0); }}><FiSkipBack/></Button><Button aria-label="Föregående checkpoint" title="Föregående checkpoint" onClick={() => { setPlaying(false); setFrameIndex((value) => Math.max(0, value - 1)); }}><FiChevronLeft/></Button><Button aria-label={playing ? 'Pausa replay' : 'Spela replay'} title={playing ? 'Pausa replay' : 'Spela replay'} onClick={() => setPlaying((value) => !value)}>{playing ? <FiPause/> : <FiPlay/>}</Button><Button aria-label="Nästa checkpoint" title="Nästa checkpoint" onClick={() => { setPlaying(false); setFrameIndex((value) => Math.min(playbackFrames.length - 1, value + 1)); }}><FiChevronRight/></Button><Button aria-label="Sista checkpoint" title="Sista checkpoint" onClick={() => { setPlaying(false); setFrameIndex(playbackFrames.length - 1); }}><FiSkipForward/></Button>
      </ButtonGroup>
      <Text fontSize="sm" color="gray.500">{activePlayers.length} spelare på planen</Text>
    </HStack>
    <HStack justify="space-between" flexWrap="wrap" spacing={3}>
      <ButtonGroup size="xs" isAttached variant="outline">
        <Button isActive={preferences.replayPlayerLabel === 'number'} onClick={() => setPreference('replayPlayerLabel', 'number')}>#</Button>
        <Button isActive={preferences.replayPlayerLabel === 'position'} onClick={() => setPreference('replayPlayerLabel', 'position')}>Position</Button>
      </ButtonGroup>
      <ButtonGroup size="xs" isAttached variant="outline">
        <Button isActive={preferences.replayPlayerVisual === 'rings'} onClick={() => setPreference('replayPlayerVisual', 'rings')}>Ringar</Button>
        <Button isActive={preferences.replayPlayerVisual === 'avatars'} onClick={() => setPreference('replayPlayerVisual', 'avatars')}>Avatarer</Button>
      </ButtonGroup>
      <Checkbox size="sm" isChecked={showGrid} onChange={(event) => setShowGrid(event.target.checked)}>Rutnät</Checkbox>
    </HStack>
    {frame.actions?.length > 0 && <Text fontSize="sm">Actions: {frame.actions.map((action) => action.type).join(' · ')}</Text>}
    {frame.events?.length > 0 && <Text fontSize="xs" color="gray.500">Händelser: {frame.events.join(' · ')}</Text>}
    <HStack align="start" spacing={4} flexWrap="wrap">
      {[0, 1].map((team) => <Box key={team} flex="1" minW="180px"><Text fontSize="sm" fontWeight="semibold">{teamName(team)} · avbytare</Text><Text fontSize="sm" color="gray.600">{bench.filter((player) => Number(player.team) === team).map(playerName).join(', ') || 'Inga'}</Text></Box>)}
    </HStack>
    {injured.length > 0 && <Box><Text fontSize="sm" fontWeight="semibold">Skadebänk</Text><Text fontSize="sm" color="red.500">{injured.map(playerName).join(', ')}</Text></Box>}
  </VStack>;
}