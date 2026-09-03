import React, { useEffect, useState } from 'react';
import { Alert, AlertIcon, Box, Button, FormControl, FormLabel, Heading, Input, SimpleGrid, Stack, Text, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import Navigation from '../components/misc/Navigation';
import { useMyTeams } from '../context/MyTeamsContext';

export default function AccountPage() {
  const { user, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];
  const { teams, loading: teamsLoading, refresh: refreshTeams } = useMyTeams();
  const [connection, setConnection] = useState(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [challenge, setChallenge] = useState(null);
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const accept = (result) => {
    if (result.status === 'GUARD_REQUIRED') setChallenge(result);
    if (result.status === 'AUTHENTICATED') { setChallenge(null); setConnection({ connected: true, steamUsername: result.steamUsername, steamId: result.steamId }); refreshTeams(); }
    if (result.status === 'DISCONNECTED') { setConnection({ connected: false, steamUsername: username }); refreshTeams(); }
  };
  const run = async (action) => {
    setBusy(true); setError('');
    try { accept(await action()); } catch (reason) { setError(reason?.response?.data?.message || reason?.message || 'Authentication failed'); }
    finally { setPassword(''); setBusy(false); }
  };

  useEffect(() => {
    WarpScoresApiService.steamConnection(...auth).then((value) => { setConnection(value); setUsername(value.steamUsername || ''); }).catch((reason) => setError(reason.message));
    // Token functions are stable in Auth0; loading once is intentional.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <VStack align="stretch"><Navigation currentPage="account" />
    <Stack spacing={6} maxW="xl">
      <Box><Heading size="lg">BlaskScore account</Heading><Text>{user?.name || user?.email}</Text>
        <Text fontSize="sm" color="gray.500">Sign-in providers are managed by Auth0. Google and GitHub can be enabled there; NAF requires a future identity-provider integration.</Text></Box>
      <Box borderWidth="1px" borderRadius="md" p={5}>
        <Heading size="md" mb={3}>Blood Bowl 3 / Steam</Heading>
        {error && <Alert status="error" mb={4}><AlertIcon />{error}</Alert>}
        {connection?.connected ? <Stack>
          <Text>Connected as <strong>{connection.steamUsername}</strong></Text>
          <Heading size="sm" pt={2}>My teams</Heading>
          {teamsLoading ? <Text>Loading teams…</Text> : teams.length === 0 ? <Text>No BB3 teams found.</Text> :
            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}>{teams.map((team) =>
              <Box key={team.id} borderWidth="1px" borderRadius="md" p={3}>
                <Text fontWeight="bold">{team.name || team.id}</Text>
                <Text fontSize="sm">Race ID: {team.raceId ?? 'unknown'} · TV: {team.teamValue ?? 'unknown'}</Text>
              </Box>)}</SimpleGrid>}
          <Button alignSelf="start" onClick={() => run(async () => { await WarpScoresApiService.disconnectSteam(...auth); return { status: 'DISCONNECTED' }; })} isLoading={busy}>Disconnect session</Button>
        </Stack> : challenge ? <Stack>
          <Text>{challenge.method === 'device_confirmation' ? 'Approve the sign-in in the Steam app, then confirm below.' : `Enter the Steam Guard code${challenge.emailHint ? ` sent to ${challenge.emailHint}` : ''}.`}</Text>
          {challenge.method !== 'device_confirmation' && <FormControl><FormLabel>Steam Guard code</FormLabel><Input value={code} onChange={(e) => setCode(e.target.value)} autoComplete="one-time-code" /></FormControl>}
          <Button colorScheme="blue" isLoading={busy} onClick={() => run(() => challenge.method === 'device_confirmation' ? WarpScoresApiService.confirmSteamGuard(challenge.challengeId, ...auth) : WarpScoresApiService.submitSteamGuardCode(challenge.challengeId, code, ...auth))}>Continue</Button>
          <Button variant="ghost" onClick={() => setChallenge(null)}>Cancel</Button>
        </Stack> : <Stack>
          <Text fontSize="sm">Your Steam username is remembered. Passwords and Guard codes are sent only for this login and are never stored by BlaskScore.</Text>
          <FormControl><FormLabel>Steam username</FormLabel><Input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" /></FormControl>
          <FormControl><FormLabel>Steam password</FormLabel><Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" /></FormControl>
          <Button colorScheme="blue" isDisabled={!username || !password} isLoading={busy} onClick={() => run(() => WarpScoresApiService.startSteamAuthentication({ username, password }, ...auth))}>Connect Steam</Button>
        </Stack>}
      </Box>
    </Stack>
  </VStack>;
}
