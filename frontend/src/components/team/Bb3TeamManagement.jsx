import React, { useEffect, useState } from 'react';
import {
  Alert,
  AlertIcon,
  Box,
  Button,
  FormControl,
  FormLabel,
  Heading,
  HStack,
  Input,
  Select,
  Spinner,
  Stack,
  Table,
  TableContainer,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
  Text,
  Textarea,
} from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import WarpScoresApiService from '../../WarpScoresApiService';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import { useMyTeams } from '../../context/MyTeamsContext';
import { getStarPlayerDisplayName } from '../../util/starplayerUtil';

const serializePitchMap = (pitchMap) => JSON.stringify(pitchMap || {}, null, 2);
const describeIds = (values) => (values || []).map((value) => `#${value}`).join(', ') || '-';
const formatRuleName = (name) => name
  ?.replace(/[_-]+/g, ' ')
  .replace(/\b\w/g, (letter) => letter.toUpperCase());
const describeSkills = (player) => (player.skill_ids || []).map((skillId) => {
  const name = player.skill_names?.[String(skillId)];
  return name ? formatRuleName(name) : `#${skillId}`;
}).join(', ') || '-';

function Bb3TeamManagement({ teamId }) {
  const intl = useIntl();
  const auth = useAuth0WithUserPermissions();
  const { isMyTeam } = useMyTeams();
  const [roster, setRoster] = useState(null);
  const [formations, setFormations] = useState([]);
  const [selectedFormationId, setSelectedFormationId] = useState('');
  const [formationName, setFormationName] = useState('');
  const [formationType, setFormationType] = useState(0);
  const [pitchMapText, setPitchMapText] = useState('{}');
  const [selectedPlayerId, setSelectedPlayerId] = useState('');
  const [improvements, setImprovements] = useState(null);
  const [selectedSkillId, setSelectedSkillId] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [characteristicRoll, setCharacteristicRoll] = useState(null);
  const [selectedCharacteristicId, setSelectedCharacteristicId] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const callArgs = [auth.getAccessTokenSilently, auth.getAccessTokenWithPopup];
  const players = roster?.players || [];
  const selectedPlayer = players.find((player) => player.player_id === selectedPlayerId);
  const categories = improvements?.skill_categories || [];
  const chosenSkills = categories.flatMap((category) => category.skills || [])
    .filter((skill) => skill.available && skill.choosable);
  const randomCategories = categories.filter((category) => category.random_available && category.random_choosable);
  const selectedSkill = chosenSkills.find((skill) => String(skill.skill_id) === selectedSkillId);
  const selectedRandomCategory = randomCategories.find((category) => String(category.category) === selectedCategory);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [rosterResult, formationResult] = await Promise.all([
        WarpScoresApiService.bb3TeamRoster(teamId, ...callArgs),
        WarpScoresApiService.bb3TeamFormations(teamId, ...callArgs),
      ]);
      setRoster(rosterResult);
      const items = formationResult.items || [];
      setFormations(items);
      if (!selectedFormationId && items.length) {
        setSelectedFormationId(items[0].formationId || '');
        setFormationName(items[0].name || '');
        setFormationType(items[0].formationType ?? 0);
        setPitchMapText(serializePitchMap(items[0].pitchMap));
      }
      if (!selectedPlayerId && rosterResult.players?.length) {
        setSelectedPlayerId(rosterResult.players[0].player_id || '');
      }
    } catch (reason) {
      setError(reason.response?.data?.message || reason.message || String(reason));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isMyTeam(teamId)) loadData();
    else setLoading(false);
  }, [teamId, isMyTeam]);

  useEffect(() => {
    setImprovements(null);
    setCharacteristicRoll(null);
    if (!selectedPlayerId) return;
    WarpScoresApiService.bb3PlayerImprovements(teamId, selectedPlayerId, ...callArgs)
      .then(setImprovements)
      .catch((reason) => setError(reason.response?.data?.message || reason.message || String(reason)));
  }, [teamId, selectedPlayerId]);

  const runAction = async (action, successMessage, refreshRoster = false) => {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await action();
      setNotice(successMessage);
      if (refreshRoster) await loadData();
      if (selectedPlayerId) {
        setImprovements(await WarpScoresApiService.bb3PlayerImprovements(teamId, selectedPlayerId, ...callArgs));
      }
    } catch (reason) {
      setError(reason.response?.data?.message || reason.message || String(reason));
    } finally {
      setBusy(false);
    }
  };

  const selectFormation = (formationId) => {
    setSelectedFormationId(formationId);
    const formation = formations.find((item) => item.formationId === formationId);
    setFormationName(formation?.name || '');
    setFormationType(formation?.formationType ?? 0);
    setPitchMapText(serializePitchMap(formation?.pitchMap));
  };

  const saveFormation = async () => {
    let pitchMap;
    try {
      pitchMap = JSON.parse(pitchMapText);
      if (!pitchMap || Array.isArray(pitchMap) || typeof pitchMap !== 'object') throw new Error();
    } catch (_error) {
      setError(intl.formatMessage({ id: 'team.bb3.invalidFormation' }));
      return;
    }
    await runAction(async () => {
      const result = await WarpScoresApiService.saveBb3Formation(teamId, {
        ...(selectedFormationId ? { formationId: selectedFormationId } : {}),
        name: formationName,
        formationType: Number(formationType),
        pitchMap,
      }, ...callArgs);
      const saved = { formationId: result.formationId, name: formationName, formationType: Number(formationType), pitchMap };
      setFormations((current) => [saved, ...current.filter((item) => item.formationId !== result.formationId)]);
      setSelectedFormationId(result.formationId);
    }, intl.formatMessage({ id: 'team.bb3.formationSaved' }));
  };

  const removeFormation = () => runAction(async () => {
    await WarpScoresApiService.deleteBb3Formation(teamId, selectedFormationId, ...callArgs);
    const remaining = formations.filter((item) => item.formationId !== selectedFormationId);
    setFormations(remaining);
    setSelectedFormationId(remaining[0]?.formationId || '');
    setFormationName(remaining[0]?.name || '');
    setFormationType(remaining[0]?.formationType ?? 0);
    setPitchMapText(serializePitchMap(remaining[0]?.pitchMap));
  }, intl.formatMessage({ id: 'team.bb3.formationRemoved' }));

  const improve = (skill) => runAction(async () => {
    if (!skill) return;
    await WarpScoresApiService.advanceBb3PlayerSkill(teamId, selectedPlayerId, {
      kind: 'chosen', skillId: Number(skill.skill_id),
    }, ...callArgs);
  }, intl.formatMessage({ id: 'team.bb3.advancementApplied' }), true);

  const improveRandom = () => runAction(async () => {
    await WarpScoresApiService.advanceBb3PlayerSkill(teamId, selectedPlayerId, {
      kind: 'random', category: Number(selectedCategory),
    }, ...callArgs);
  }, intl.formatMessage({ id: 'team.bb3.advancementApplied' }), true);

  const rollCharacteristic = () => runAction(async () => {
    const result = await WarpScoresApiService.rollBb3PlayerCharacteristic(teamId, selectedPlayerId, ...callArgs);
    setCharacteristicRoll(result);
    setSelectedCharacteristicId(String(result.characteristics?.find((item) => item.available)?.characteristic_id || ''));
  }, intl.formatMessage({ id: 'team.bb3.characteristicRolled' }));

  const chooseCharacteristic = () => runAction(async () => {
    await WarpScoresApiService.chooseBb3PlayerCharacteristic(teamId, selectedPlayerId, {
      rollId: characteristicRoll.rollId,
      characteristicId: Number(selectedCharacteristicId),
    }, ...callArgs);
    setCharacteristicRoll(null);
  }, intl.formatMessage({ id: 'team.bb3.advancementApplied' }), true);

  if (!isMyTeam(teamId)) return null;
  if (loading) return <Spinner />;

  return (
    <Stack spacing={5} width="full">
      <Box borderBottom="1px solid" borderColor="warpScoresBorderColor" pb={2}>
        <Heading size="md">{intl.formatMessage({ id: 'team.bb3.management' })}</Heading>
      </Box>
      {error && <Alert status="error"><AlertIcon />{error}</Alert>}
      {notice && <Alert status="success"><AlertIcon />{notice}</Alert>}

      <Box>
        <Heading size="sm" mb={3}>{intl.formatMessage({ id: 'team.bb3.currentRoster' })}</Heading>
        {players.length ? (
          <TableContainer>
            <Table size="sm" variant="striped">
              <Thead><Tr>
                <Th>#</Th><Th>{intl.formatMessage({ id: 'common.name' })}</Th>
                <Th>{intl.formatMessage({ id: 'common.level' })}</Th><Th>SPP</Th>
                <Th>{intl.formatMessage({ id: 'common.skills' })}</Th>
                <Th>{intl.formatMessage({ id: 'team.bb3.characteristics' })}</Th>
                <Th>{intl.formatMessage({ id: 'common.injuries' })}</Th>
              </Tr></Thead>
              <Tbody>{players.map((player) => (
                <Tr key={player.player_id}>
                  <Td>{player.number}</Td><Td>{getStarPlayerDisplayName(player.name)}</Td><Td>{player.level}</Td><Td>{player.spp}</Td>
                  <Td>{describeSkills(player)}</Td>
                  <Td>{(player.characteristics || []).map((item) => `#${item.characteristic_id}: ${item.value}`).join(', ') || '-'}</Td>
                  <Td>{describeIds(player.casualty_ids)}{player.miss_next_game ? ' · MNG' : ''}{player.dead ? ' · Dead' : ''}</Td>
                </Tr>
              ))}</Tbody>
            </Table>
          </TableContainer>
        ) : <Text>{intl.formatMessage({ id: 'team.bb3.noPlayers' })}</Text>}
      </Box>

      <Box>
        <Heading size="sm" mb={3}>{intl.formatMessage({ id: 'team.bb3.formations' })}</Heading>
        <Stack spacing={3} maxWidth="720px">
          <FormControl>
            <FormLabel>{intl.formatMessage({ id: 'team.bb3.selectFormation' })}</FormLabel>
            <Select value={selectedFormationId} onChange={(event) => selectFormation(event.target.value)}>
              <option value="">{intl.formatMessage({ id: 'team.bb3.newFormation' })}</option>
              {formations.map((item) => <option key={item.formationId} value={item.formationId}>{item.name}</option>)}
            </Select>
          </FormControl>
          <FormControl>
            <FormLabel>{intl.formatMessage({ id: 'common.name' })}</FormLabel>
            <Input value={formationName} maxLength={40} onChange={(event) => setFormationName(event.target.value)} />
          </FormControl>
          <FormControl>
            <FormLabel>{intl.formatMessage({ id: 'team.bb3.formationType' })}</FormLabel>
            <Select value={formationType} onChange={(event) => setFormationType(Number(event.target.value))}>
              <option value={0}>{intl.formatMessage({ id: 'team.bb3.defensive' })}</option>
              <option value={1}>{intl.formatMessage({ id: 'team.bb3.offensive' })}</option>
            </Select>
          </FormControl>
          <FormControl>
            <FormLabel>{intl.formatMessage({ id: 'team.bb3.pitchMap' })}</FormLabel>
            <Textarea value={pitchMapText} onChange={(event) => setPitchMapText(event.target.value)} rows={8} fontFamily="mono" />
          </FormControl>
          <HStack>
            <Button colorScheme="teal" onClick={saveFormation} isLoading={busy} isDisabled={!formationName.trim()}>
              {intl.formatMessage({ id: 'common.save' })}
            </Button>
            <Button variant="outline" colorScheme="red" onClick={removeFormation} isLoading={busy} isDisabled={!selectedFormationId}>
              {intl.formatMessage({ id: 'common.delete' })}
            </Button>
          </HStack>
        </Stack>
      </Box>

      <Box>
        <Heading size="sm" mb={3}>{intl.formatMessage({ id: 'team.bb3.playerAdvancement' })}</Heading>
        {players.length ? (
          <Stack spacing={3} maxWidth="720px">
            <FormControl>
              <FormLabel>{intl.formatMessage({ id: 'common.player' })}</FormLabel>
              <Select value={selectedPlayerId} onChange={(event) => setSelectedPlayerId(event.target.value)}>
                {players.map((player) => <option key={player.player_id} value={player.player_id}>{player.number}. {player.name} ({player.spp} SPP)</option>)}
              </Select>
            </FormControl>
            {selectedPlayer && <Text fontSize="sm">{intl.formatMessage({ id: 'team.bb3.sppSpent' }, { spent: improvements?.spent_spp ?? 0 })}</Text>}
            <FormControl>
              <FormLabel>{intl.formatMessage({ id: 'team.bb3.chosenSkill' })}</FormLabel>
              <Select value={selectedSkillId} onChange={(event) => setSelectedSkillId(event.target.value)}>
                <option value="">{intl.formatMessage({ id: 'team.bb3.chooseSkill' })}</option>
                {chosenSkills.map((skill) => <option key={skill.skill_id} value={skill.skill_id}>
                  {skill.name ? formatRuleName(skill.name) : `#${skill.skill_id}`} · {skill.cost} SPP
                </option>)}
              </Select>
              <Button mt={2} onClick={() => improve(chosenSkills.find((skill) => String(skill.skill_id) === selectedSkillId))}
                isLoading={busy} isDisabled={!selectedSkill || !selectedPlayer?.can_be_updated || selectedPlayer.spp < selectedSkill.cost}>
                {intl.formatMessage({ id: 'team.bb3.addSkill' })}
              </Button>
            </FormControl>
            <FormControl>
              <FormLabel>{intl.formatMessage({ id: 'team.bb3.randomSkill' })}</FormLabel>
              <Select value={selectedCategory} onChange={(event) => setSelectedCategory(event.target.value)}>
                <option value="">{intl.formatMessage({ id: 'team.bb3.chooseCategory' })}</option>
                {randomCategories.map((category) => <option key={category.category} value={category.category}>#{category.category} · {category.cost_random} SPP</option>)}
              </Select>
              <Button mt={2} onClick={improveRandom} isLoading={busy}
                isDisabled={!selectedRandomCategory || !selectedPlayer?.can_be_updated || selectedPlayer.spp < selectedRandomCategory.cost_random}>
                {intl.formatMessage({ id: 'team.bb3.addRandomSkill' })}
              </Button>
            </FormControl>
            <Button alignSelf="flex-start" onClick={rollCharacteristic} isLoading={busy}
              isDisabled={!improvements?.characteristic_available || !improvements?.characteristic_choosable
                || !selectedPlayer?.can_be_updated || selectedPlayer.spp < (improvements?.characteristic_cost ?? Infinity)}>
              {intl.formatMessage({ id: 'team.bb3.rollCharacteristic' })}
            </Button>
            {characteristicRoll && <HStack align="end">
              <FormControl>
                <FormLabel>{intl.formatMessage({ id: 'team.bb3.characteristicResult' }, { roll: characteristicRoll.roll })}</FormLabel>
                <Select value={selectedCharacteristicId} onChange={(event) => setSelectedCharacteristicId(event.target.value)}>
                  {characteristicRoll.characteristics.filter((item) => item.available).map((item) => (
                    <option key={item.characteristic_id} value={item.characteristic_id}>#{item.characteristic_id}</option>
                  ))}
                </Select>
              </FormControl>
              <Button onClick={chooseCharacteristic} isLoading={busy} isDisabled={!selectedCharacteristicId}>
                {intl.formatMessage({ id: 'team.bb3.acceptCharacteristic' })}
              </Button>
            </HStack>}
          </Stack>
        ) : <Text>{intl.formatMessage({ id: 'team.bb3.noPlayers' })}</Text>}
      </Box>
    </Stack>
  );
}

export default Bb3TeamManagement;