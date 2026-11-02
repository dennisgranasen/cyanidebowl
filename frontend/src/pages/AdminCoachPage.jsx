import React, { useEffect, useState } from 'react';
import { Alert, Box, Button, Heading, HStack, Input, Spinner, Table, Tbody, Td, Th, Thead, Tr, VStack } from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

export default function AdminCoachPage() {
  const { authenticationReady, userPermissions, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const [claims, setClaims] = useState([]);
  const [drafts, setDrafts] = useState({});
  const [saving, setSaving] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];

  const load = () => WarpScoresApiService.adminCoaches(...auth)
    .then((items) => {
      setClaims(items);
      setDrafts(Object.fromEntries(items.map((item) => [item.id?.key || item.id?.mongoKey || item.id?.value, {
        bb2CoachId: item.bb2CoachId || '', bb3CoachId: item.bb3CoachId || '',
      }])));
    })
    .catch((reason) => setError(reason?.message || String(reason)))
    .finally(() => setLoading(false));

  useEffect(() => {
    if (authenticationReady && userPermissions.writeSiteAdmin) load();
  }, [authenticationReady, userPermissions.writeSiteAdmin]);

  const save = async (coach) => {
    const id = coach.id?.key || coach.id?.mongoKey || coach.id?.value;
    if (!id) return;
    setSaving((current) => ({ ...current, [id]: true }));
    try {
      const updated = await WarpScoresApiService.updateAdminCoachMappings(id, drafts[id], ...auth);
      setClaims((items) => items.map((item) => (item.id?.key || item.id?.mongoKey || item.id?.value) === id ? updated : item));
    } catch (reason) { setError(reason?.message || String(reason)); }
    finally { setSaving((current) => ({ ...current, [id]: false })); }
  };

  return <VStack align="stretch" spacing={5} p={{ base: 3, md: 6 }}>
    <Navigation currentPage="admin" parentPage="admin" />
    <Box><Heading>Coaches</Heading><Box color="gray.500" mt={2}>Connect each BlaskScore coach profile to its Blood Bowl 2 and Blood Bowl 3 coach IDs.</Box></Box>
    {error && <Alert status="error">{error}</Alert>}
    {loading ? <Spinner alignSelf="start" /> : <Table size="sm"><Thead><Tr><Th>BlaskScore coach</Th><Th>Profile ID</Th><Th>BB2 coach ID</Th><Th>BB3 coach ID</Th><Th /></Tr></Thead><Tbody>
      {claims.map((coach) => { const id = coach.id?.key || coach.id?.mongoKey || coach.id?.value; const draft = drafts[id] || {}; return <Tr key={id}><Td>{coach.name || 'Unnamed coach'}</Td><Td>{coach.id?.key || coach.id?.value || id}</Td><Td><Input size="sm" value={draft.bb2CoachId || ''} onChange={(event) => setDrafts((current) => ({ ...current, [id]: { ...draft, bb2CoachId: event.target.value } }))} /></Td><Td><Input size="sm" value={draft.bb3CoachId || ''} onChange={(event) => setDrafts((current) => ({ ...current, [id]: { ...draft, bb3CoachId: event.target.value } }))} /></Td><Td><Button size="sm" colorScheme="blue" isLoading={saving[id]} onClick={() => save(coach)}>Save</Button></Td></Tr>; })}
      {!claims.length && <Tr><Td colSpan={5}>No coach profiles found.</Td></Tr>}
    </Tbody></Table>}
  </VStack>;
}
