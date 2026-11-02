import React, { useEffect, useState } from 'react';
import { Alert, Box, Button, Heading, HStack, Spinner, Table, Tbody, Td, Th, Thead, Tr, VStack } from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

export default function AdminCoachPage() {
  const { authenticationReady, userPermissions, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];

  const load = () => WarpScoresApiService.adminCoachClaims(...auth)
    .then(setClaims)
    .catch((reason) => setError(reason?.message || String(reason)))
    .finally(() => setLoading(false));

  useEffect(() => {
    if (authenticationReady && userPermissions.writeSiteAdmin) load();
  }, [authenticationReady, userPermissions.writeSiteAdmin]);

  const remove = async (id) => {
    try {
      await WarpScoresApiService.adminRemoveCoachClaim(id, ...auth);
      setClaims((items) => items.filter((item) => item.id !== id));
    } catch (reason) { setError(reason?.message || String(reason)); }
  };

  return <VStack align="stretch" spacing={5} p={{ base: 3, md: 6 }}>
    <Navigation currentPage="admin" parentPage="admin" />
    <Box><Heading>Coaches</Heading><Box color="gray.500" mt={2}>Manage coach identities claimed by BlaskScore accounts across Blood Bowl 2 and Blood Bowl 3.</Box></Box>
    {error && <Alert status="error">{error}</Alert>}
    {loading ? <Spinner alignSelf="start" /> : <Table size="sm"><Thead><Tr><Th>Game</Th><Th>Coach</Th><Th>Coach ID</Th><Th>BlaskScore account</Th><Th /></Tr></Thead><Tbody>
      {claims.map((claim) => <Tr key={claim.id}><Td>{claim.game}</Td><Td>{claim.coachName}</Td><Td>{claim.coachId}</Td><Td>{claim.userDisplayName || claim.userEmail || claim.authSubject}</Td><Td><Button size="xs" colorScheme="red" onClick={() => remove(claim.id)}>Remove claim</Button></Td></Tr>)}
      {!claims.length && <Tr><Td colSpan={5}>No claimed coaches found.</Td></Tr>}
    </Tbody></Table>}
  </VStack>;
}
