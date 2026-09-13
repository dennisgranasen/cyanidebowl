import React, { useEffect, useMemo, useState } from 'react';
import {
  Avatar, Box, Button, Checkbox, FormControl, FormLabel, Heading, HStack,
  Input, Select, SimpleGrid, Slider, SliderFilledTrack, SliderThumb,
  SliderTrack, Text, Textarea, VStack
} from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import WarpScoresApiService from '../WarpScoresApiService';
import CommunityApi from '../CommunityApi';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

const behavioralFields = [
  ['optimism', 'Optimism'],
  ['coachPatience', 'Coach patience'],
  ['playerPatience', 'Player patience'],
  ['tacticalInterest', 'Tactical interest'],
  ['matchFocus', 'Match focus'],
  ['foodDrinkInterest', 'Food/drink interest'],
  ['chantInterest', 'Chant interest'],
  ['trashTalk', 'Trash talk'],
  ['superstition', 'Superstition'],
];

function AdminCommunityFansPage() {
  const { getAccessTokenSilently, getAccessTokenWithPopup } =
    useAuth0WithUserPermissions();
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];

  const [fans, setFans] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [draft, setDraft] = useState(null);
  const [settings, setSettings] = useState(null);
  const [media, setMedia] = useState([]);
  const [filterTeam, setFilterTeam] = useState('');
  const [filterActive, setFilterActive] = useState('active');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    setError('');
    try {
      const [fanData, settingsData] = await Promise.all([
        WarpScoresApiService.adminCommunityFans(...auth),
        WarpScoresApiService.adminCommunityFanSettings(...auth),
      ]);
      setFans(fanData || []);
      setSettings(settingsData);
    } catch (e) {
      setError(e?.message || String(e));
    }
  };

  useEffect(() => { load(); }, []);

  const teams = useMemo(
    () => [...new Map(
      fans.filter((fan) => fan.teamId)
        .map((fan) => [fan.teamId, fan.teamName || fan.teamId])
    ).entries()].sort((a, b) => String(a[1]).localeCompare(String(b[1]))),
    [fans]
  );

  const visible = fans.filter((fan) => {
    if (filterTeam && fan.teamId !== filterTeam) return false;
    if (filterActive === 'active' && !fan.active) return false;
    if (filterActive === 'inactive' && fan.active) return false;
    return true;
  });

  const selectFan = async (fan) => {
    setSelectedId(fan.id);
    setDraft({ ...fan });
    setMessage('');
    try {
      setMedia(await WarpScoresApiService.adminCommunityFanMedia(fan.id, ...auth));
    } catch {
      setMedia([]);
    }
  };

  const update = (field, value) =>
    setDraft((current) => ({ ...current, [field]: value }));

  const saveFan = async () => {
    if (!draft) return;
    setBusy(true);
    setError('');
    try {
      const saved = await WarpScoresApiService.updateAdminCommunityFan(
        draft.id, draft, ...auth
      );
      setDraft(saved);
      setFans((current) =>
        current.map((fan) => fan.id === saved.id ? saved : fan)
      );
      setMessage('Profile saved.');
    } catch (e) {
      setError(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  };

  const regenerate = async () => {
    if (!draft) return;
    setBusy(true);
    try {
      const result = await WarpScoresApiService.regenerateAdminCommunityFanMedia(
        draft.id, ...auth
      );
      setMedia(result || []);
      setMessage('New profile-image and avatar jobs queued.');
    } catch (e) {
      setError(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  };

  const syncNow = async () => {
    setBusy(true);
    try {
      const result = await WarpScoresApiService.reconcileCommunityFansNow(...auth);
      setMessage(
        `Fan sync complete: ${result.changedTeams}/${result.scannedTeams} teams changed`
        + (result.failedTeams ? `; ${result.failedTeams} failed.` : '.')
      );
      await load();
    } catch (e) {
      setError(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  };

  const saveSettings = async () => {
    if (!settings) return;
    setBusy(true);
    try {
      const saved = await WarpScoresApiService.updateAdminCommunityFanSettings(
        settings, ...auth
      );
      setSettings(saved);
      setMessage('Fan settings saved.');
    } catch (e) {
      setError(e?.message || String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <VStack align="stretch" spacing={5} p={{ base: 3, md: 6 }}>
      <Navigation currentPage="admin" />
      <HStack justify="space-between">
        <Box>
          <Heading>Community members</Heading>
          <Text color="gray.400">
            Dedicated Fan profiles, personalities, media and reconciliation.
          </Text>
        </Box>
        <Button colorScheme="purple" onClick={syncNow} isLoading={busy}>
          Sync fans now
        </Button>
      </HStack>

      {error && <Text color="red.300">{error}</Text>}
      {message && <Text color="green.300">{message}</Text>}

      {settings && (
        <Box borderWidth="1px" borderRadius="md" p={4}>
          <Heading size="sm" mb={3}>Population settings</Heading>
          <SimpleGrid columns={{ base: 1, md: 3 }} spacing={4}>
            <FormControl>
              <FormLabel>Loyalty switch probability</FormLabel>
              <Input
                type="number" min="0" max="1" step="0.05"
                value={settings.loyaltySwitchProbability}
                onChange={(e) => setSettings({
                  ...settings,
                  loyaltySwitchProbability: Number(e.target.value),
                })}
              />
            </FormControl>
            <FormControl>
              <FormLabel>Automatic reconciliation</FormLabel>
              <Checkbox
                isChecked={settings.populationReconciliationEnabled}
                onChange={(e) => setSettings({
                  ...settings,
                  populationReconciliationEnabled: e.target.checked,
                })}
              >
                Enabled
              </Checkbox>
            </FormControl>
            <FormControl>
              <FormLabel>Interval (hours)</FormLabel>
              <Input
                type="number" min="1"
                value={settings.populationReconciliationIntervalHours}
                onChange={(e) => setSettings({
                  ...settings,
                  populationReconciliationIntervalHours: Number(e.target.value),
                })}
              />
            </FormControl>
          </SimpleGrid>
          <Button mt={3} size="sm" onClick={saveSettings} isLoading={busy}>
            Save settings
          </Button>
        </Box>
      )}

      <SimpleGrid columns={{ base: 1, xl: 2 }} spacing={5}>
        <Box borderWidth="1px" borderRadius="md" p={4}>
          <Heading size="sm" mb={3}>Community members</Heading>
          <HStack mb={3}>
            <Select value={filterTeam} onChange={(e) => setFilterTeam(e.target.value)}>
              <option value="">All teams</option>
              {teams.map(([id, name]) => (
                <option key={id} value={id}>{name}</option>
              ))}
            </Select>
            <Select
              value={filterActive}
              onChange={(e) => setFilterActive(e.target.value)}
            >
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="all">All</option>
            </Select>
          </HStack>
          <VStack align="stretch" maxH="70vh" overflowY="auto">
            {visible.map((fan) => (
              <Button
                key={fan.id}
                variant={fan.id === selectedId ? 'solid' : 'outline'}
                h="auto"
                py={2}
                justifyContent="flex-start"
                onClick={() => selectFan(fan)}
              >
                <Avatar
                  size="sm"
                  mr={3}
                  name={fan.displayName}
                  src={CommunityApi.assetUrl(
                    fan.avatarImageUrl || fan.profileImageUrl
                  )}
                />
                <Box textAlign="left">
                  <Text>{fan.displayName}</Text>
                  <Text fontSize="xs" color="gray.500">
                    {fan.teamName} · {fan.species} · {fan.supporterArchetype}
                  </Text>
                </Box>
              </Button>
            ))}
          </VStack>
        </Box>

        <Box borderWidth="1px" borderRadius="md" p={4}>
          {!draft ? (
            <Text color="gray.500">Select a community member.</Text>
          ) : (
            <VStack align="stretch" spacing={3}>
              <HStack>
                <Avatar
                  size="xl"
                  name={draft.displayName}
                  src={CommunityApi.assetUrl(
                    draft.avatarImageUrl || draft.profileImageUrl
                  )}
                />
                <Box>
                  <Heading size="md">{draft.displayName}</Heading>
                  <Text color="gray.400">{draft.teamName}</Text>
                </Box>
              </HStack>

              <SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}>
                {[
                  ['displayName', 'Display name'],
                  ['species', 'Species'],
                  ['supporterArchetype', 'Archetype'],
                  ['location', 'Location'],
                  ['occupation', 'Occupation'],
                  ['favoriteFood', 'Favourite food'],
                  ['favoriteDrink', 'Favourite drink'],
                  ['favoriteChant', 'Favourite chant'],
                  ['teamColors', 'Team colours'],
                  ['profileImageUrl', 'Profile image URL'],
                  ['avatarImageUrl', 'Avatar URL'],
                ].map(([field, label]) => (
                  <FormControl key={field}>
                    <FormLabel>{label}</FormLabel>
                    <Input
                      value={draft[field] || ''}
                      onChange={(e) => update(field, e.target.value)}
                    />
                  </FormControl>
                ))}
              </SimpleGrid>

              <FormControl>
                <FormLabel>Bio</FormLabel>
                <Textarea
                  value={draft.bio || ''}
                  onChange={(e) => update('bio', e.target.value)}
                />
              </FormControl>

              <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4}>
                {behavioralFields.map(([field, label]) => (
                  <FormControl key={field}>
                    <FormLabel>
                      {label}: {Number(draft[field] || 0).toFixed(2)}
                    </FormLabel>
                    <Slider
                      min={0} max={1} step={0.05}
                      value={Number(draft[field] || 0)}
                      onChange={(value) => update(field, value)}
                    >
                      <SliderTrack><SliderFilledTrack /></SliderTrack>
                      <SliderThumb />
                    </Slider>
                  </FormControl>
                ))}
              </SimpleGrid>

              <FormControl>
                <FormLabel>Profile image prompt</FormLabel>
                <Textarea
                  minH="120px"
                  value={draft.profileImagePrompt || ''}
                  onChange={(e) => update('profileImagePrompt', e.target.value)}
                />
              </FormControl>
              <FormControl>
                <FormLabel>Avatar prompt</FormLabel>
                <Textarea
                  minH="100px"
                  value={draft.avatarPrompt || ''}
                  onChange={(e) => update('avatarPrompt', e.target.value)}
                />
              </FormControl>

              <HStack>
                <Button colorScheme="blue" onClick={saveFan} isLoading={busy}>
                  Save profile
                </Button>
                <Button onClick={regenerate} isLoading={busy}>
                  Regenerate images
                </Button>
              </HStack>

              <Box>
                <Heading size="xs" mb={2}>Media jobs</Heading>
                <VStack align="stretch">
                  {media.map((job) => (
                    <Box key={job.id} borderWidth="1px" borderRadius="md" p={2}>
                      <Text fontSize="sm">
                        {job.target} · {job.status}
                        {job.model ? ` · ${job.model}` : ''}
                      </Text>
                      {job.error && (
                        <Text color="red.300" fontSize="xs">{job.error}</Text>
                      )}
                    </Box>
                  ))}
                  {!media.length && (
                    <Text color="gray.500" fontSize="sm">No media jobs.</Text>
                  )}
                </VStack>
              </Box>
            </VStack>
          )}
        </Box>
      </SimpleGrid>
    </VStack>
  );
}

export default AdminCommunityFansPage;
