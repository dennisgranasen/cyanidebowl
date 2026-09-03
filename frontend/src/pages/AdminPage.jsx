import React, { useEffect, useState } from 'react';
import { Box, Button, Checkbox, FormControl, FormLabel, Heading, HStack, IconButton, Input, Select, SimpleGrid, Text, Tooltip, VStack } from '@chakra-ui/react';
import { SearchIcon } from '@chakra-ui/icons';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import imageUrls from '../imageUrls';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import WarpScoresApiService from '../WarpScoresApiService';
import formatter from '../util/formatter';

const emptySystem = { name: '', primary: false, discoveryAliases: [], discoveryNotificationEnabled: false, discoveryNotificationEmail: '' };
const emptySeason = { id: '', number: '', name: '', isCollected: true };
const emptyPhase = { id: '', name: '', type: 'OTHER', sequence: '' };
const emptyStage = { id: '', name: '', type: 'OTHER', format: '', step: '', displayOrder: '' };
const emptySource = { id: '', sourceEntityId: '', sourceType: 'Competition', game: 'BB3', platform: 'PC', ruleset: '', firstIndex: '', lastIndex: '', firstId: '', lastId: '', includedMatchIds: [], excludedMatchIds: [], isArchived: false };
const numberOrNull = (value) => (value === '' ? null : Number(value));

function TextField({ label, value, onChange, type = 'text' }) {
  return <FormControl><FormLabel>{label}</FormLabel><Input type={type} value={value ?? ''} onChange={(event) => onChange(event.target.value)} /></FormControl>;
}

function ResourceList({ heading, items, selectedId, onSelect, label }) {
  return <Box><Heading size="sm" mb={2}>{heading}</Heading><VStack align="stretch" maxH="12rem" overflowY="auto">{items.map((item) => <Button key={item.id} size="sm" justifyContent="flex-start" variant={item.id === selectedId ? 'solid' : 'outline'} onClick={() => onSelect(item)}>{label(item)}</Button>)}{items.length === 0 && <Text color="gray.500">No entries</Text>}</VStack></Box>;
}

function AdminPage() {
  const { authenticationReady, checkPermissions, userPermissions, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const navigate = useNavigate();
  const [systems, setSystems] = useState([]);
  const [seasons, setSeasons] = useState([]);
  const [stages, setStages] = useState([]);
  const [phases, setPhases] = useState([]);
  const [registeredSources, setRegisteredSources] = useState([]);
  const [sourceInspections, setSourceInspections] = useState({});
  const [inspectedRegisteredSourceId, setInspectedRegisteredSourceId] = useState(null);
  const [registeredSourceMatches, setRegisteredSourceMatches] = useState([]);
  const [registeredSourceInspectLoading, setRegisteredSourceInspectLoading] = useState(false);
  const [sources, setSources] = useState([]);
  const [system, setSystem] = useState(emptySystem);
  const [season, setSeason] = useState(emptySeason);
  const [stage, setStage] = useState(emptyStage);
  const [phase, setPhase] = useState(emptyPhase);
  const [source, setSource] = useState(emptySource);
  const [selectedSystemId, setSelectedSystemId] = useState(null);
  const [selectedSeasonId, setSelectedSeasonId] = useState(null);
  const [selectedStageId, setSelectedStageId] = useState(null);
  const [selectedPhaseId, setSelectedPhaseId] = useState(null);
  const [selectedSourceId, setSelectedSourceId] = useState(null);
  const [discoveryCandidates, setDiscoveryCandidates] = useState([]);
  const [cyanideSearch, setCyanideSearch] = useState({ term: '', opus: '3', exact: true });
  const [cyanideSearchResults, setCyanideSearchResults] = useState({ leagueDetails: [], competitionDetails: [] });
  const [cyanideSearchLoading, setCyanideSearchLoading] = useState(false);
  const [cyanideInspections, setCyanideInspections] = useState({});
  const [openCyanideInspections, setOpenCyanideInspections] = useState({});
  const [cyanideInspectionLoading, setCyanideInspectionLoading] = useState({});
  const [preparedCandidate, setPreparedCandidate] = useState(null);
  const [sourceMatches, setSourceMatches] = useState([]);
  const [sourceMatchesLoading, setSourceMatchesLoading] = useState(false);
  const [error, setError] = useState(null);
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];
  const fail = (reason) => setError(reason?.message || String(reason));
  const loadSystems = () => WarpScoresApiService.leagueSystems(...auth).then(setSystems).catch(fail);

  const clearSourceMatches = () => { setSourceMatches([]); setSourceMatchesLoading(false); };
  const selectSystem = (item) => { setSelectedSystemId(item.id); setSystem({ ...emptySystem, ...item }); setSelectedSeasonId(null); setSelectedStageId(null); setSelectedSourceId(null); setSeason(emptySeason); setStage(emptyStage); setSource(emptySource); setStages([]); setSources([]); setDiscoveryCandidates([]); setPreparedCandidate(null); clearSourceMatches(); WarpScoresApiService.seasons(item.id, ...auth).then(setSeasons).catch(fail); };
  const loadSourceInspections = (seasonId) => WarpScoresApiService.registeredSourceInspections(seasonId, ...auth)
    .then((items) => setSourceInspections(Object.fromEntries(items.map((item) => [item.registeredSourceId, item])))).catch(fail);
  const loadRegisteredSources = (seasonId) => {
    setRegisteredSources([]);
    setSourceInspections({});
    return WarpScoresApiService.registeredSources(seasonId, ...auth)
      .then((items) => {
        setRegisteredSources(items);
        if (items.length > 0) loadSourceInspections(seasonId);
      })
      .catch(fail);
  };
  const selectSeason = (item) => { setSelectedSeasonId(item.id); setSeason(item); setSelectedPhaseId(null); setSelectedStageId(null); setSelectedSourceId(null); setInspectedRegisteredSourceId(null); setRegisteredSourceMatches([]); setPhase(emptyPhase); setStage(emptyStage); setSource(emptySource); setPhases([]); setStages([]); setSources([]); clearSourceMatches(); WarpScoresApiService.phases(item.id, ...auth).then(setPhases).catch(fail); loadRegisteredSources(item.id); };
  const selectPhase = (item) => { setSelectedPhaseId(item.id); setPhase(item); setSelectedStageId(null); setStage(emptyStage); setSources([]); WarpScoresApiService.phaseStages(item.id, ...auth).then(setStages).catch(fail); };
  const sourceFromCandidate = (candidate) => ({ ...emptySource, sourceEntityId: candidate.sourceEntityId, sourceType: candidate.sourceType, game: candidate.game || emptySource.game, platform: candidate.platform || emptySource.platform });
  const selectStage = (item) => { setSelectedStageId(item.id); setStage(item); setSelectedSourceId(null); setSource(preparedCandidate ? sourceFromCandidate(preparedCandidate) : emptySource); clearSourceMatches(); WarpScoresApiService.stageSources(item.id, ...auth).then(setSources).catch(fail); };
  const selectSource = (item) => {
    setSelectedSourceId(item.id);
    setSource({ ...item, sourceEntityId: item.sourceEntityId?.key || '' });
    setSourceMatchesLoading(true);
    WarpScoresApiService.stageMatches(item.stageId || selectedStageId)
      .then((matches) => setSourceMatches(matches.filter((match) => match.stageSourceId === item.id)))
      .catch(fail)
      .finally(() => setSourceMatchesLoading(false));
  };

  useEffect(() => {
    if (authenticationReady && userPermissions.writeSiteAdmin) loadSystems();
  }, [authenticationReady, userPermissions.writeSiteAdmin]);

  useEffect(() => {
    if (authenticationReady && checkPermissions && !userPermissions.writeSiteAdmin) {
      navigate('/');
    }
  }, [authenticationReady, checkPermissions, navigate, userPermissions.writeSiteAdmin]);

  const saveSystem = () => (selectedSystemId ? WarpScoresApiService.updateLeagueSystem(selectedSystemId, system, ...auth) : WarpScoresApiService.createLeagueSystem(system, ...auth)).then((item) => { loadSystems(); selectSystem(item); }).catch(fail);
  const saveSeason = () => { if (!selectedSystemId) return; const data = { ...season, number: numberOrNull(season.number) }; if (!selectedSeasonId) delete data.id; (selectedSeasonId ? WarpScoresApiService.updateSeason(selectedSeasonId, data, ...auth) : WarpScoresApiService.createSeason(selectedSystemId, data, ...auth)).then((item) => { WarpScoresApiService.seasons(selectedSystemId, ...auth).then(setSeasons); selectSeason(item); }).catch(fail); };
  const savePhase = () => { if (!selectedSeasonId) return; const data = { ...phase, sequence: numberOrNull(phase.sequence) }; (selectedPhaseId ? WarpScoresApiService.updatePhase(selectedPhaseId, data, ...auth) : WarpScoresApiService.createPhase(selectedSeasonId, data, ...auth)).then((item) => { WarpScoresApiService.phases(selectedSeasonId, ...auth).then(setPhases); selectPhase(item); }).catch(fail); };
  const saveStage = () => { if (!selectedPhaseId) return; const data = { ...stage, step: numberOrNull(stage.step), displayOrder: numberOrNull(stage.displayOrder) }; (selectedStageId ? WarpScoresApiService.updateStage(selectedStageId, data, ...auth) : WarpScoresApiService.createPhaseStage(selectedPhaseId, data, ...auth)).then((item) => { WarpScoresApiService.phaseStages(selectedPhaseId, ...auth).then(setStages); selectStage(item); }).catch(fail); };
  const saveSource = () => { if (!selectedStageId) return; const data = { ...source, firstIndex: numberOrNull(source.firstIndex), lastIndex: numberOrNull(source.lastIndex) }; const action = selectedSourceId && source.registeredSourceId ? WarpScoresApiService.updateMatchSelection(selectedSourceId, data, ...auth) : selectedSourceId ? WarpScoresApiService.updateStageSource(selectedSourceId, data, ...auth) : WarpScoresApiService.createStageSource(selectedStageId, data, ...auth); action.then((item) => { WarpScoresApiService.stageSources(selectedStageId, ...auth).then(setSources); selectSource(item); setPreparedCandidate(null); }).catch(fail); };
  const scanForCandidates = () => WarpScoresApiService.leagueSystemDiscoveryCandidates(selectedSystemId, ...auth).then(setDiscoveryCandidates).catch(fail);
  const suggestedSeason = (...names) => {
    for (const name of names) {
      const matches = String(name || '').match(/(?:^|\D)(\d{1,3})(?!\d)/g);
      if (matches?.length) {
        const number = Number(matches[matches.length - 1].match(/\d+/)[0]);
        if (Number.isInteger(number)) return number;
      }
    }
    return null;
  };
  const candidateFromCompetition = (competition) => ({
    candidateId: `Competition:${competition.id?.key}`,
    sourceEntityId: competition.id?.key,
    sourceType: 'Competition',
    leagueName: competition.leagueName,
    competitionName: competition.name,
    suggestedSeasonNumber: suggestedSeason(competition.name, competition.leagueName),
    game: `BB${competition.id?.opus || Number(cyanideSearch.opus)}`,
    platform: emptySource.platform,
  });
  const registerCandidate = (competition) => {
    if (!selectedSeasonId) return;
    const candidate = candidateFromCompetition(competition);
    WarpScoresApiService.registerSource(selectedSeasonId, {
      sourceEntityId: candidate.sourceEntityId, sourceType: candidate.sourceType,
      game: candidate.game, platform: candidate.platform, collectionEnabled: true,
    }, ...auth).then((item) => { setRegisteredSources([...registeredSources, item]); loadSourceInspections(selectedSeasonId); }).catch(fail);
  };
  const registerDiscoveryCandidate = (candidate) => {
    if (!selectedSeasonId) return;
    WarpScoresApiService.registerSource(selectedSeasonId, {
      sourceEntityId: candidate.sourceEntityId, sourceType: candidate.sourceType,
      game: candidate.game, platform: candidate.platform || emptySource.platform,
      collectionEnabled: true,
    }, ...auth).then((item) => { setRegisteredSources([...registeredSources, item]); loadSourceInspections(selectedSeasonId); }).catch(fail);
  };
  const attachSource = (registeredSource) => {
    if (!selectedStageId) return;
    WarpScoresApiService.createMatchSelection(selectedStageId, { registeredSourceId: registeredSource.id }, ...auth)
      .then((item) => setSources([...sources, item])).catch(fail);
  };
  const inspectRegisteredSource = (registeredSource) => {
    if (inspectedRegisteredSourceId === registeredSource.id) {
      setInspectedRegisteredSourceId(null); setRegisteredSourceMatches([]); return;
    }
    setInspectedRegisteredSourceId(registeredSource.id); setRegisteredSourceInspectLoading(true);
    WarpScoresApiService.inspectRegisteredSource(registeredSource.id, 10, ...auth)
      .then(setRegisteredSourceMatches).catch(fail).finally(() => setRegisteredSourceInspectLoading(false));
  };
  const searchCyanide = async () => {
    if (!cyanideSearch.term.trim()) return;
    setCyanideSearchLoading(true);
    setCyanideInspections({});
    setOpenCyanideInspections({});
    setError(null);
    try {
      const term = cyanideSearch.term.trim();
      const isId = /^\d+$/.test(term) || /^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(term);
      const result = await WarpScoresApiService.lookup({
        ...(isId ? { league_id: term } : { league_name: term }),
        opus: Number(cyanideSearch.opus),
        exact: cyanideSearch.exact ? 1 : 0,
        instruction: 'HAS_CONTESTS',
        fallback: 0,
        includeDetails: true,
      }, ...auth);
      setCyanideSearchResults({
        leagueDetails: result?.leagueDetails || result?.fullLeagues || [],
        competitionDetails: result?.competitionDetails || result?.fullCompetitions || [],
      });
    } catch (reason) {
      fail(reason);
      setCyanideSearchResults({ leagueDetails: [], competitionDetails: [] });
    } finally {
      setCyanideSearchLoading(false);
    }
  };
  const inspectCyanideCompetition = (competition) => {
    const competitionId = competition.id?.key;
    if (!competitionId) return;
    setOpenCyanideInspections((current) => ({ ...current, [competitionId]: !current[competitionId] }));
    if (cyanideInspections[competitionId]) return;
    setCyanideInspectionLoading((current) => ({ ...current, [competitionId]: true }));
    WarpScoresApiService.inspectCyanideCompetition(competitionId, 5, ...auth)
      .then((inspection) => setCyanideInspections((current) => ({ ...current, [competitionId]: inspection })))
      .catch(fail).finally(() => setCyanideInspectionLoading((current) => ({ ...current, [competitionId]: false })));
  };
  const prepareCandidate = (candidate) => {
    setPreparedCandidate(candidate);
    setSource(sourceFromCandidate(candidate));
    const existingSeason = seasons.find((item) => item.number === candidate.suggestedSeasonNumber);
    if (existingSeason) {
      selectSeason(existingSeason);
    } else {
      setSelectedSeasonId(null);
      setSelectedStageId(null);
      setSeason({ ...emptySeason, number: candidate.suggestedSeasonNumber ?? '', name: candidate.suggestedSeasonNumber ? `Season ${candidate.suggestedSeasonNumber}` : '' });
      setStages([]);
      setSources([]);
    }
  };
  const remove = (label, action, clear) => { if (window.confirm(`Delete ${label} and all of its children?`)) action().then(() => { clear(); loadSystems(); }).catch(fail); };

  return (
    <VStack align="stretch" spacing={5}>
      <Navigation currentPage="admin" />
      <HeaderCard mainImageSrc={imageUrls.blaskscoreLogoPng('medium')} heading="League Systems" subHeading="Manage seasons, stages, and match sources" />
      {error && <Text color="red.400">{error}</Text>}
      <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="League systems" items={systems} selectedId={selectedSystemId} onSelect={selectSystem} label={(item) => `${item.primary ? '★ ' : ''}${item.name || item.id}`} /><Box><Heading size="sm" mb={2}>{selectedSystemId ? 'Edit league system' : 'New league system'}</Heading><TextField label="Name" value={system.name} onChange={(name) => setSystem({ ...system, name })} /><Checkbox mt={3} isChecked={Boolean(system.primary)} onChange={(event) => setSystem({ ...system, primary: event.target.checked })}>Primary league system on home page</Checkbox><TextField label="Discovery aliases (comma separated)" value={(system.discoveryAliases || []).join(', ')} onChange={(value) => setSystem({ ...system, discoveryAliases: value.split(',').map((alias) => alias.trim()).filter(Boolean) })} /><Checkbox mt={3} isChecked={Boolean(system.discoveryNotificationEnabled)} onChange={(event) => setSystem({ ...system, discoveryNotificationEnabled: event.target.checked })}>Email new candidate notifications</Checkbox>{system.discoveryNotificationEnabled && <TextField label="Notification email" type="email" value={system.discoveryNotificationEmail} onChange={(discoveryNotificationEmail) => setSystem({ ...system, discoveryNotificationEmail })} />}<HStack mt={3}><Button onClick={() => { setSelectedSystemId(null); setSystem(emptySystem); }}>New</Button><Button colorScheme="blue" onClick={saveSystem}>Save</Button>{selectedSystemId && <Button colorScheme="red" onClick={() => remove('league system', () => WarpScoresApiService.deleteLeagueSystem(selectedSystemId, ...auth), () => { setSelectedSystemId(null); setSystem(emptySystem); setSeasons([]); setStages([]); setSources([]); })}>Delete</Button>}</HStack></Box></SimpleGrid>
      {selectedSystemId && <FormControl maxW="30rem"><FormLabel>Working season</FormLabel><Select placeholder={seasons.length ? 'Choose the season to configure' : 'No seasons yet — create one below'} value={selectedSeasonId || ''} isDisabled={seasons.length === 0} onChange={(event) => { const selected = seasons.find((item) => item.id === event.target.value); if (selected) selectSeason(selected); }}>{seasons.map((item) => <option key={item.id} value={item.id}>{item.name || `Season ${item.number}`}</option>)}</Select><Text color="gray.500" fontSize="sm" mt={1}>Selecting a season reveals its phases, stages, watched sources, summaries, and Inspect actions.</Text></FormControl>}
      <Box borderWidth="1px" borderRadius="md" p={4}>
        <Heading size="sm" mb={1}>Search Cyanide</Heading>
        <Text color="gray.500" mb={3}>Search league names or IDs. Search results do not fetch matches.</Text>
        <SimpleGrid columns={{ base: 1, md: 4 }} spacing={3} alignItems="end">
          <TextField label="League name or ID" value={cyanideSearch.term} onChange={(term) => setCyanideSearch({ ...cyanideSearch, term })} />
          <FormControl><FormLabel>Blood Bowl version</FormLabel><Select value={cyanideSearch.opus} onChange={(event) => setCyanideSearch({ ...cyanideSearch, opus: event.target.value })}><option value="1">Blood Bowl 1</option><option value="2">Blood Bowl 2</option><option value="3">Blood Bowl 3</option></Select></FormControl>
          <Checkbox pb={2} isChecked={cyanideSearch.exact} onChange={(event) => setCyanideSearch({ ...cyanideSearch, exact: event.target.checked })}>Exact name</Checkbox>
          <Button colorScheme="blue" isLoading={cyanideSearchLoading} onClick={searchCyanide}>Search</Button>
        </SimpleGrid>
        {(cyanideSearchResults.leagueDetails.length > 0 || cyanideSearchResults.competitionDetails.length > 0) && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={5} mt={4}>
          <Box><Heading size="xs" mb={2}>Leagues</Heading><VStack align="stretch">{cyanideSearchResults.leagueDetails.map((item) => <Box key={item.id?.key || item.name} borderWidth="1px" borderRadius="md" p={3}><Text fontWeight="bold">{item.name}</Text><Text color="gray.500" fontSize="sm">{item.id?.key || item.id?.value}</Text></Box>)}</VStack></Box>
          <Box><Heading size="xs" mb={2}>Competitions</Heading><VStack align="stretch">{cyanideSearchResults.competitionDetails.map((item) => { const competitionId = item.id?.key; const registered = registeredSources.some((entry) => entry.sourceEntityId?.key === competitionId); const isInspected = Boolean(openCyanideInspections[competitionId]); const inspection = cyanideInspections[competitionId]; const isLoadingInspection = Boolean(cyanideInspectionLoading[competitionId]); return <Box key={competitionId || item.name} borderWidth="1px" borderRadius="md" p={3}><HStack justify="space-between" align="start"><Box><Text fontWeight="bold">{item.name}</Text><Text color="gray.500" fontSize="sm">{item.leagueName} · {item.status || 'Unknown status'} · {item.format || 'Unknown format'}</Text><Text mt={1} fontSize="sm">{item.teamsCount ?? '?'}{item.teamsMax != null ? `/${item.teamsMax}` : ''} teams · {item.playedMatches ?? '?'}{item.totalMatches != null ? `/${item.totalMatches}` : ''} matches · round {item.currentRound ?? '?'}{item.totalRounds != null ? `/${item.totalRounds}` : ''}</Text>{item.liveMatches > 0 && <Text color="orange.400" fontSize="sm">{item.liveMatches} live</Text>}<Text color="gray.500" fontSize="xs">{competitionId}</Text></Box><VStack align="end"><Button size="sm" colorScheme="blue" isDisabled={!selectedSeasonId || registered || !competitionId} onClick={() => registerCandidate(item)}>{registered ? 'Registered' : selectedSeasonId ? 'Add to season' : 'Choose working season'}</Button><Tooltip label={isInspected ? 'Close inspection' : 'Inspect latest matches'}><IconButton size="sm" variant={isInspected ? 'solid' : 'outline'} colorScheme={isInspected ? 'blue' : undefined} aria-label={isInspected ? `Close inspection for ${item.name}` : `Inspect ${item.name}`} icon={<SearchIcon />} isDisabled={!competitionId} isLoading={isLoadingInspection} onClick={() => inspectCyanideCompetition(item)} /></Tooltip></VStack></HStack>{isInspected && <Box mt={3} pt={3} borderTopWidth="1px">{isLoadingInspection && !inspection ? <Text color="gray.500">Reading contest metadata from Cyanide…</Text> : !inspection?.contests?.length ? <Text color="gray.500">No played contests found.</Text> : <><Text fontSize="sm" mb={2}>Latest played: {formatter.formatAsDate(inspection.latestMatch, 'unknown')}</Text><VStack align="stretch">{inspection.contests.map((contest) => <Box key={contest.contestId || contest.matchId} borderWidth="1px" borderRadius="md" p={2}><Text fontWeight="bold">{(contest.teams || []).map((team) => `${team.name || '-'} ${team.score ?? '-'}`).join(' – ')}</Text><Text fontSize="sm" color="gray.500">{(contest.teams || []).map((team) => [team.coach, team.race].filter(Boolean).join(' · ')).join(' / ')}</Text><Text fontSize="xs" color="gray.500">Round {contest.round ?? '?'} · {contest.status || 'Unknown status'} · {formatter.formatAsDate(contest.matchDate, 'No date')}</Text></Box>)}</VStack></>}</Box>}</Box>; })}</VStack></Box>
        </SimpleGrid>}
      </Box>
      {selectedSystemId && <Box><HStack mb={2}><Heading size="sm">Potential new seasons and sources</Heading><Button size="sm" onClick={scanForCandidates}>Scan database</Button></HStack><VStack align="stretch">{discoveryCandidates.map((candidate) => { const registered = registeredSources.some((entry) => entry.sourceEntityId?.key === candidate.sourceEntityId); return <Box key={candidate.candidateId} borderWidth="1px" borderRadius="md" p={3}><HStack justify="space-between"><Box><Text fontWeight="bold">{candidate.competitionName || candidate.sourceEntityId}</Text><Text color="gray.500">{candidate.leagueName} · Suggested season {candidate.suggestedSeasonNumber ?? '?'} · {candidate.matchCount} known matches</Text></Box><Button size="sm" colorScheme="blue" isDisabled={!selectedSeasonId || registered} onClick={() => registerDiscoveryCandidate(candidate)}>{registered ? 'Registered' : selectedSeasonId ? 'Add to season' : 'Choose working season'}</Button></HStack></Box>; })}{discoveryCandidates.length === 0 && <Text color="gray.500">Scan to find match sources that are not configured yet. Scanning does not fetch matches.</Text>}</VStack></Box>}
      {selectedSystemId && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="Seasons" items={seasons} selectedId={selectedSeasonId} onSelect={selectSeason} label={(item) => item.name || item.id} /><Box><Heading size="sm" mb={2}>{selectedSeasonId ? 'Edit season' : 'New season'}</Heading><SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}><TextField label="Number" type="number" value={season.number} onChange={(number) => setSeason({ ...season, number })} /><TextField label="Name" value={season.name} onChange={(name) => setSeason({ ...season, name })} /><Checkbox isChecked={Boolean(season.isCollected)} onChange={(event) => setSeason({ ...season, isCollected: event.target.checked })}>Collect data</Checkbox></SimpleGrid><HStack mt={3}><Button onClick={() => { setSelectedSeasonId(null); setSeason(emptySeason); }}>New</Button><Button colorScheme="blue" onClick={saveSeason}>Save</Button>{selectedSeasonId && <Button colorScheme="red" onClick={() => remove('season', () => WarpScoresApiService.deleteSeason(selectedSeasonId, ...auth), () => { setSelectedSeasonId(null); setSeason(emptySeason); setStages([]); setSources([]); })}>Delete</Button>}</HStack></Box></SimpleGrid>}
      {selectedSeasonId && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="Phases" items={phases} selectedId={selectedPhaseId} onSelect={selectPhase} label={(item) => item.name || item.id} /><Box><Heading size="sm" mb={2}>{selectedPhaseId ? 'Edit phase' : 'New phase'}</Heading><SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}><TextField label="Name" value={phase.name} onChange={(name) => setPhase({ ...phase, name })} /><FormControl><FormLabel>Type</FormLabel><Select value={phase.type} onChange={(event) => setPhase({ ...phase, type: event.target.value })}>{['OFF_SEASON','PRESEASON','FRIENDLIES','QUALIFICATION','GROUP_STAGE','PLAYOFFS','OTHER'].map((type) => <option key={type}>{type}</option>)}</Select></FormControl><TextField label="Sequence" type="number" value={phase.sequence} onChange={(sequence) => setPhase({ ...phase, sequence })} /></SimpleGrid><HStack mt={3}><Button onClick={() => { setSelectedPhaseId(null); setPhase(emptyPhase); setStages([]); }}>New</Button><Button colorScheme="blue" onClick={savePhase}>Save</Button>{selectedPhaseId && <Button colorScheme="red" onClick={() => remove('phase', () => WarpScoresApiService.deletePhase(selectedPhaseId, ...auth), () => { setSelectedPhaseId(null); setPhase(emptyPhase); setStages([]); })}>Delete</Button>}</HStack></Box></SimpleGrid>}
      {selectedPhaseId && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="Stages" items={stages} selectedId={selectedStageId} onSelect={selectStage} label={(item) => item.name || item.id} /><Box><Heading size="sm" mb={2}>{selectedStageId ? 'Edit stage' : 'New stage'}</Heading><SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}><TextField label="Name" value={stage.name} onChange={(name) => setStage({ ...stage, name })} /><FormControl><FormLabel>Type</FormLabel><Select value={stage.type} onChange={(event) => setStage({ ...stage, type: event.target.value })}><option>GROUP</option><option>ROUND</option><option>OTHER</option></Select></FormControl><TextField label="Format" value={stage.format} onChange={(format) => setStage({ ...stage, format })} /><TextField label="Step (same means parallel)" type="number" value={stage.step} onChange={(step) => setStage({ ...stage, step })} /><TextField label="Display order" type="number" value={stage.displayOrder} onChange={(displayOrder) => setStage({ ...stage, displayOrder })} /></SimpleGrid><HStack mt={3}><Button onClick={() => { setSelectedStageId(null); setStage(emptyStage); }}>New</Button><Button colorScheme="blue" onClick={saveStage}>Save</Button>{selectedStageId && <Button colorScheme="red" onClick={() => remove('stage', () => WarpScoresApiService.deleteStage(selectedStageId, ...auth), () => { setSelectedStageId(null); setStage(emptyStage); setSources([]); })}>Delete</Button>}</HStack></Box></SimpleGrid>}
      {selectedSeasonId && <Box><Heading size="sm" mb={2}>Registered and watched sources</Heading><VStack align="stretch">{registeredSources.map((item) => { const inspection = sourceInspections[item.id]; const isInspected = inspectedRegisteredSourceId === item.id; return <Box key={item.id} borderWidth="1px" borderRadius="md" p={3}><HStack justify="space-between" align="start"><Box><Text fontWeight="bold">{item.sourceEntityId?.key}</Text><Text color="gray.500" fontSize="sm">{item.game} / {item.platform} · {item.collectionEnabled ? 'Watched' : 'Not watched'}</Text><Text color="gray.500" fontSize="sm">{inspection ? `${inspection.matchCount} matches · ${inspection.teamCount} teams · latest ${formatter.formatAsDate(inspection.latestMatch, 'unknown')}` : 'Reading local match summary…'}</Text></Box><HStack><Button size="sm" variant="outline" onClick={() => inspectRegisteredSource(item)}>{isInspected ? 'Close' : 'Inspect'}</Button>{selectedStageId && <Button size="sm" onClick={() => attachSource(item)}>Use in selected stage</Button>}</HStack></HStack>{isInspected && <Box mt={3} pt={3} borderTopWidth="1px">{registeredSourceInspectLoading ? <Text color="gray.500">Loading latest matches…</Text> : registeredSourceMatches.length === 0 ? <Text color="gray.500">No locally stored matches.</Text> : <VStack align="stretch" maxH="24rem" overflowY="auto">{registeredSourceMatches.map((match) => <Box key={match.matchId || match.id?.key} borderWidth="1px" borderRadius="md" p={2}><Text fontWeight="bold">{(match.teams || []).map((team) => `${team.name || '-'} ${team.score ?? '-'}`).join(' – ')}</Text><Text fontSize="sm" color="gray.500">{(match.teams || []).map((team, index) => [team.race, match.coaches?.[index]?.name || team.coachName].filter(Boolean).join(' · ')).join(' / ')}</Text><Text fontSize="xs" color="gray.500">{formatter.formatAsDate(match.finished || match.started, 'No date')} · {match.matchId || match.id?.key}</Text></Box>)}</VStack>}</Box>}</Box>; })}</VStack></Box>}
      {selectedStageId && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="Match selections" items={sources} selectedId={selectedSourceId} onSelect={selectSource} label={(item) => item.sourceEntityId?.key || item.id} /><Box><Heading size="sm" mb={2}>{selectedSourceId ? 'Edit match selection' : 'Legacy inline source'}</Heading><SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}><TextField label="Source entity ID" value={source.sourceEntityId} onChange={(sourceEntityId) => setSource({ ...source, sourceEntityId })} /><TextField label="First index" type="number" value={source.firstIndex} onChange={(firstIndex) => setSource({ ...source, firstIndex })} /><TextField label="Last index" type="number" value={source.lastIndex} onChange={(lastIndex) => setSource({ ...source, lastIndex })} /><TextField label="First match ID" value={source.firstId} onChange={(firstId) => setSource({ ...source, firstId })} /><TextField label="Last match ID" value={source.lastId} onChange={(lastId) => setSource({ ...source, lastId })} /><TextField label="Always include match IDs (comma separated)" value={(source.includedMatchIds || []).join(', ')} onChange={(value) => setSource({ ...source, includedMatchIds: value.split(',').map((id) => id.trim()).filter(Boolean) })} /><TextField label="Always exclude match IDs (comma separated)" value={(source.excludedMatchIds || []).join(', ')} onChange={(value) => setSource({ ...source, excludedMatchIds: value.split(',').map((id) => id.trim()).filter(Boolean) })} /><Checkbox isChecked={Boolean(source.isArchived)} onChange={(event) => setSource({ ...source, isArchived: event.target.checked })}>Archive source</Checkbox></SimpleGrid><HStack mt={3}><Button colorScheme="blue" isDisabled={!selectedSourceId} onClick={saveSource}>Save selection</Button>{selectedSourceId && <Button colorScheme="red" onClick={() => remove('match selection', () => WarpScoresApiService.deleteStageSource(selectedSourceId, ...auth), () => { setSelectedSourceId(null); setSource(emptySource); })}>Delete</Button>}</HStack></Box></SimpleGrid>}
      {selectedSourceId && <Box><Heading size="sm" mb={2}>Database matches for selected source</Heading>{sourceMatchesLoading ? <Text color="gray.500">Loading matches…</Text> : sourceMatches.length === 0 ? <Text color="gray.500">No matches found for this source.</Text> : <VStack align="stretch" maxH="24rem" overflowY="auto">{[...sourceMatches].sort((first, second) => new Date(second.finishedAt || second.startedAt || 0) - new Date(first.finishedAt || first.startedAt || 0)).map((match) => <Box key={match.sourceMatchKey} borderWidth="1px" borderRadius="md" p={3}><HStack justify="space-between" align="start"><Box><Text fontWeight="bold">{(match.teams || []).map((team) => `${team.name || '-'} ${team.score ?? '-'}`).join(' – ') || match.sourceMatchKey}</Text><Text color="gray.500" fontSize="sm">{match.finishedAt || match.startedAt || 'No date'} · {match.status || 'Unknown status'}</Text></Box><Text fontSize="sm">{match.game} / {match.platform}</Text></HStack><Text mt={1} fontSize="xs" color="gray.500">{match.sourceMatchKey}</Text></Box>)}</VStack>}</Box>}
    </VStack>
  );
}

export default AdminPage;
