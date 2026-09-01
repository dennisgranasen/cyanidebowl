import React, { useEffect, useState } from 'react';
import { Box, Button, Checkbox, FormControl, FormLabel, Heading, HStack, Input, Select, SimpleGrid, Text, VStack } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import imageUrls from '../imageUrls';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import WarpScoresApiService from '../WarpScoresApiService';

const emptySystem = { name: '', discoveryAliases: [], discoveryNotificationEnabled: false, discoveryNotificationEmail: '' };
const emptySeason = { id: '', number: '', name: '', isCollected: true };
const emptyStage = { id: '', phase: '', name: '', format: '', sequence: '' };
const emptySource = { id: '', sourceEntityId: '', sourceType: 'Competition', game: 'BB3', platform: 'PC', ruleset: '', firstIndex: '', lastIndex: '', firstId: '', lastId: '', isArchived: false };
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
  const [sources, setSources] = useState([]);
  const [system, setSystem] = useState(emptySystem);
  const [season, setSeason] = useState(emptySeason);
  const [stage, setStage] = useState(emptyStage);
  const [source, setSource] = useState(emptySource);
  const [selectedSystemId, setSelectedSystemId] = useState(null);
  const [selectedSeasonId, setSelectedSeasonId] = useState(null);
  const [selectedStageId, setSelectedStageId] = useState(null);
  const [selectedSourceId, setSelectedSourceId] = useState(null);
  const [discoveryCandidates, setDiscoveryCandidates] = useState([]);
  const [preparedCandidate, setPreparedCandidate] = useState(null);
  const [error, setError] = useState(null);
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];
  const fail = (reason) => setError(reason?.message || String(reason));
  const loadSystems = () => WarpScoresApiService.leagueSystems(...auth).then(setSystems).catch(fail);

  const selectSystem = (item) => { setSelectedSystemId(item.id); setSystem({ ...emptySystem, ...item }); setSelectedSeasonId(null); setSelectedStageId(null); setSelectedSourceId(null); setSeason(emptySeason); setStage(emptyStage); setSource(emptySource); setStages([]); setSources([]); setDiscoveryCandidates([]); setPreparedCandidate(null); WarpScoresApiService.seasons(item.id, ...auth).then(setSeasons).catch(fail); };
  const selectSeason = (item) => { setSelectedSeasonId(item.id); setSeason(item); setSelectedStageId(null); setSelectedSourceId(null); setStage(emptyStage); setSource(emptySource); setSources([]); WarpScoresApiService.stages(item.id, ...auth).then(setStages).catch(fail); };
  const sourceFromCandidate = (candidate) => ({ ...emptySource, sourceEntityId: candidate.sourceEntityId, sourceType: candidate.sourceType, game: candidate.game || emptySource.game, platform: candidate.platform || emptySource.platform });
  const selectStage = (item) => { setSelectedStageId(item.id); setStage(item); setSelectedSourceId(null); setSource(preparedCandidate ? sourceFromCandidate(preparedCandidate) : emptySource); WarpScoresApiService.stageSources(item.id, ...auth).then(setSources).catch(fail); };
  const selectSource = (item) => { setSelectedSourceId(item.id); setSource({ ...item, sourceEntityId: item.sourceEntityId?.key || '' }); };

  useEffect(() => {
    if (authenticationReady && userPermissions.writeSiteAdmin) loadSystems();
  }, [authenticationReady, userPermissions.writeSiteAdmin]);

  useEffect(() => {
    if (authenticationReady && checkPermissions && !userPermissions.writeSiteAdmin) {
      navigate('/');
    }
  }, [authenticationReady, checkPermissions, navigate, userPermissions.writeSiteAdmin]);

  const saveSystem = () => (selectedSystemId ? WarpScoresApiService.updateLeagueSystem(selectedSystemId, system, ...auth) : WarpScoresApiService.createLeagueSystem(system, ...auth)).then((item) => { loadSystems(); selectSystem(item); }).catch(fail);
  const saveSeason = () => { if (!selectedSystemId) return; const data = { ...season, number: numberOrNull(season.number) }; (selectedSeasonId ? WarpScoresApiService.updateSeason(selectedSeasonId, data, ...auth) : WarpScoresApiService.createSeason(selectedSystemId, data, ...auth)).then((item) => { WarpScoresApiService.seasons(selectedSystemId, ...auth).then(setSeasons); selectSeason(item); }).catch(fail); };
  const saveStage = () => { if (!selectedSeasonId) return; const data = { ...stage, sequence: numberOrNull(stage.sequence) }; (selectedStageId ? WarpScoresApiService.updateStage(selectedStageId, data, ...auth) : WarpScoresApiService.createStage(selectedSeasonId, data, ...auth)).then((item) => { WarpScoresApiService.stages(selectedSeasonId, ...auth).then(setStages); selectStage(item); }).catch(fail); };
  const saveSource = () => { if (!selectedStageId) return; const data = { ...source, firstIndex: numberOrNull(source.firstIndex), lastIndex: numberOrNull(source.lastIndex) }; (selectedSourceId ? WarpScoresApiService.updateStageSource(selectedSourceId, data, ...auth) : WarpScoresApiService.createStageSource(selectedStageId, data, ...auth)).then((item) => { WarpScoresApiService.stageSources(selectedStageId, ...auth).then(setSources); selectSource(item); setPreparedCandidate(null); }).catch(fail); };
  const scanForCandidates = () => WarpScoresApiService.leagueSystemDiscoveryCandidates(selectedSystemId, ...auth).then(setDiscoveryCandidates).catch(fail);
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
      <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="League systems" items={systems} selectedId={selectedSystemId} onSelect={selectSystem} label={(item) => item.name || item.id} /><Box><Heading size="sm" mb={2}>{selectedSystemId ? 'Edit league system' : 'New league system'}</Heading><TextField label="Name" value={system.name} onChange={(name) => setSystem({ ...system, name })} /><TextField label="Discovery aliases (comma separated)" value={(system.discoveryAliases || []).join(', ')} onChange={(value) => setSystem({ ...system, discoveryAliases: value.split(',').map((alias) => alias.trim()).filter(Boolean) })} /><Checkbox mt={3} isChecked={Boolean(system.discoveryNotificationEnabled)} onChange={(event) => setSystem({ ...system, discoveryNotificationEnabled: event.target.checked })}>Email new candidate notifications</Checkbox>{system.discoveryNotificationEnabled && <TextField label="Notification email" type="email" value={system.discoveryNotificationEmail} onChange={(discoveryNotificationEmail) => setSystem({ ...system, discoveryNotificationEmail })} />}<HStack mt={3}><Button onClick={() => { setSelectedSystemId(null); setSystem(emptySystem); }}>New</Button><Button colorScheme="blue" onClick={saveSystem}>Save</Button>{selectedSystemId && <Button colorScheme="red" onClick={() => remove('league system', () => WarpScoresApiService.deleteLeagueSystem(selectedSystemId, ...auth), () => { setSelectedSystemId(null); setSystem(emptySystem); setSeasons([]); setStages([]); setSources([]); })}>Delete</Button>}</HStack></Box></SimpleGrid>
      {selectedSystemId && <Box><HStack mb={2}><Heading size="sm">Potential new seasons and sources</Heading><Button size="sm" onClick={scanForCandidates}>Scan database</Button></HStack><VStack align="stretch">{discoveryCandidates.map((candidate) => <Box key={candidate.candidateId} borderWidth="1px" borderRadius="md" p={3}><HStack justify="space-between"><Box><Text fontWeight="bold">{candidate.competitionName || candidate.sourceEntityId}</Text><Text color="gray.500">{candidate.leagueName} · Season {candidate.suggestedSeasonNumber ?? '?'} · {candidate.matchCount} matches</Text></Box><Button size="sm" colorScheme="blue" onClick={() => prepareCandidate(candidate)}>Prepare</Button></HStack></Box>)}{discoveryCandidates.length === 0 && <Text color="gray.500">Scan to find match sources that are not configured yet.</Text>}</VStack></Box>}
      {selectedSystemId && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="Seasons" items={seasons} selectedId={selectedSeasonId} onSelect={selectSeason} label={(item) => item.name || item.id} /><Box><Heading size="sm" mb={2}>{selectedSeasonId ? 'Edit season' : 'New season'}</Heading><SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}><TextField label="Number" type="number" value={season.number} onChange={(number) => setSeason({ ...season, number })} /><TextField label="Name" value={season.name} onChange={(name) => setSeason({ ...season, name })} /><Checkbox isChecked={Boolean(season.isCollected)} onChange={(event) => setSeason({ ...season, isCollected: event.target.checked })}>Collect data</Checkbox></SimpleGrid><HStack mt={3}><Button onClick={() => { setSelectedSeasonId(null); setSeason(emptySeason); }}>New</Button><Button colorScheme="blue" onClick={saveSeason}>Save</Button>{selectedSeasonId && <Button colorScheme="red" onClick={() => remove('season', () => WarpScoresApiService.deleteSeason(selectedSeasonId, ...auth), () => { setSelectedSeasonId(null); setSeason(emptySeason); setStages([]); setSources([]); })}>Delete</Button>}</HStack></Box></SimpleGrid>}
      {selectedSeasonId && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="Stages" items={stages} selectedId={selectedStageId} onSelect={selectStage} label={(item) => item.name || item.id} /><Box><Heading size="sm" mb={2}>{selectedStageId ? 'Edit stage' : 'New stage'}</Heading><SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}><TextField label="ID" value={stage.id} onChange={(id) => setStage({ ...stage, id })} /><TextField label="Name" value={stage.name} onChange={(name) => setStage({ ...stage, name })} /><TextField label="Phase" value={stage.phase} onChange={(phase) => setStage({ ...stage, phase })} /><TextField label="Format" value={stage.format} onChange={(format) => setStage({ ...stage, format })} /><TextField label="Sequence" type="number" value={stage.sequence} onChange={(sequence) => setStage({ ...stage, sequence })} /></SimpleGrid><HStack mt={3}><Button onClick={() => { setSelectedStageId(null); setStage(emptyStage); }}>New</Button><Button colorScheme="blue" onClick={saveStage}>Save</Button>{selectedStageId && <Button colorScheme="red" onClick={() => remove('stage', () => WarpScoresApiService.deleteStage(selectedStageId, ...auth), () => { setSelectedStageId(null); setStage(emptyStage); setSources([]); })}>Delete</Button>}</HStack></Box></SimpleGrid>}
      {selectedStageId && <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={6}><ResourceList heading="Match sources" items={sources} selectedId={selectedSourceId} onSelect={selectSource} label={(item) => item.sourceEntityId?.key || item.id} /><Box><Heading size="sm" mb={2}>{selectedSourceId ? 'Edit match source' : 'New match source'}</Heading><SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}><TextField label="ID" value={source.id} onChange={(id) => setSource({ ...source, id })} /><TextField label="Source entity ID" value={source.sourceEntityId} onChange={(sourceEntityId) => setSource({ ...source, sourceEntityId })} /><FormControl><FormLabel>Source type</FormLabel><Select value={source.sourceType} onChange={(event) => setSource({ ...source, sourceType: event.target.value })}><option>League</option><option>Competition</option></Select></FormControl><FormControl><FormLabel>Game</FormLabel><Select value={source.game} onChange={(event) => setSource({ ...source, game: event.target.value })}><option>BB1</option><option>BB2</option><option>BB3</option></Select></FormControl><FormControl><FormLabel>Platform</FormLabel><Select value={source.platform} onChange={(event) => setSource({ ...source, platform: event.target.value })}><option>PC</option><option>XBOX</option><option>PS</option><option>TT</option><option>SWITCH</option><option>CROSS</option></Select></FormControl><TextField label="Ruleset" value={source.ruleset} onChange={(ruleset) => setSource({ ...source, ruleset })} /><TextField label="First index" type="number" value={source.firstIndex} onChange={(firstIndex) => setSource({ ...source, firstIndex })} /><TextField label="Last index" type="number" value={source.lastIndex} onChange={(lastIndex) => setSource({ ...source, lastIndex })} /><TextField label="First match ID" value={source.firstId} onChange={(firstId) => setSource({ ...source, firstId })} /><TextField label="Last match ID" value={source.lastId} onChange={(lastId) => setSource({ ...source, lastId })} /><Checkbox isChecked={Boolean(source.isArchived)} onChange={(event) => setSource({ ...source, isArchived: event.target.checked })}>Archive source</Checkbox></SimpleGrid><HStack mt={3}><Button onClick={() => { setSelectedSourceId(null); setSource(emptySource); }}>New</Button><Button colorScheme="blue" onClick={saveSource}>Save</Button>{selectedSourceId && <Button colorScheme="red" onClick={() => remove('match source', () => WarpScoresApiService.deleteStageSource(selectedSourceId, ...auth), () => { setSelectedSourceId(null); setSource(emptySource); })}>Delete</Button>}</HStack></Box></SimpleGrid>}
    </VStack>
  );
}

export default AdminPage;
