import React, { useEffect, useState } from 'react';
import { Box, Button, Checkbox, Heading, HStack, Select, SimpleGrid, Text, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../../WarpScoresApiService';
import { useIntl } from 'react-intl';

export default function SiteUserAdmin({ auth }) {
  const intl = useIntl();
  const [users, setUsers] = useState([]);
  const [systems, setSystems] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [draft, setDraft] = useState(null);
  const [error, setError] = useState('');

  const load = () => Promise.all([
    WarpScoresApiService.adminUsers(...auth),
    WarpScoresApiService.leagueSystems(...auth),
  ]).then(([loadedUsers, loadedSystems]) => {
    setUsers(loadedUsers);
    setSystems(loadedSystems);
    if (!selectedId && loadedUsers.length) {
      setSelectedId(String(loadedUsers[0].id));
      setDraft(loadedUsers[0]);
    }
  }).catch(reason => setError(reason?.message || String(reason)));

  useEffect(() => { load(); }, []);

  const selectUser = (id) => {
    setSelectedId(id);
    setDraft(users.find(user => String(user.id) === id) || null);
  };
  const set = (key, value) => setDraft(current => ({ ...current, [key]: value }));
  const toggleSystem = (id, checked) => {
    const current = new Set(draft?.adminForLeagueSystems || []);
    if (checked) current.add(id); else current.delete(id);
    set('adminForLeagueSystems', [...current]);
  };
  const save = () => {
    if (!draft) return;
    WarpScoresApiService.updateAdminUserPermissions(draft.id, {
      siteAdmin: Boolean(draft.siteAdmin),
      leagueAdmin: Boolean(draft.leagueAdmin),
      registerLeague: Boolean(draft.registerLeague),
      adminForLeagueSystems: draft.adminForLeagueSystems || [],
    }, ...auth).then(saved => {
      setUsers(current => current.map(user => user.id === saved.id ? saved : user));
      setDraft(saved);
    }).catch(reason => setError(reason?.message || String(reason)));
  };

  return <Box borderWidth="1px" borderRadius="md" p={4}>
    <Heading size="sm" mb={3}>{intl.formatMessage({ id: 'adminUsers.heading' })}</Heading>
    {error && <Text color="red.400" mb={2}>{error}</Text>}
    <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={4}>
      <Box>
        <Select value={selectedId} onChange={event => selectUser(event.target.value)}>
          {users.map(user => <option key={user.id} value={user.id}>{user.username || user.email || user.id}</option>)}
        </Select>
      </Box>
      {draft && <VStack align="stretch">
        <Text fontWeight="semibold">{draft.username || draft.email}</Text>
        <Text fontSize="sm" color="gray.500">{draft.email || intl.formatMessage({ id: 'adminUsers.noEmail' })} · {draft.provider || intl.formatMessage({ id: 'adminUsers.unknownProvider' })}</Text>
        <Checkbox isChecked={Boolean(draft.siteAdmin)} onChange={e => set('siteAdmin', e.target.checked)}>{intl.formatMessage({ id: 'adminUsers.siteAdmin' })}</Checkbox>
        <Checkbox isChecked={Boolean(draft.leagueAdmin)} onChange={e => set('leagueAdmin', e.target.checked)}>{intl.formatMessage({ id: 'adminUsers.allSystemsAdmin' })}</Checkbox>
        <Checkbox isChecked={Boolean(draft.registerLeague)} onChange={e => set('registerLeague', e.target.checked)}>{intl.formatMessage({ id: 'adminUsers.mayRegister' })}</Checkbox>
        <Box>
          <Text fontSize="sm" fontWeight="semibold" mb={1}>{intl.formatMessage({ id: 'adminUsers.systemAdmin' })}</Text>
          <VStack align="stretch">
            {systems.map(system => <Checkbox key={system.id}
              isDisabled={Boolean(draft.siteAdmin || draft.leagueAdmin)}
              isChecked={(draft.adminForLeagueSystems || []).includes(system.id)}
              onChange={e => toggleSystem(system.id, e.target.checked)}>{system.name || system.id}</Checkbox>)}
          </VStack>
        </Box>
        <HStack><Button colorScheme="blue" onClick={save}>{intl.formatMessage({ id: 'adminUsers.save' })}</Button></HStack>
      </VStack>}
    </SimpleGrid>
  </Box>;
}
