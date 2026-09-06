import React, { useEffect, useState } from 'react';
import { Alert, AlertIcon, Box, Button, Checkbox, FormControl, FormLabel, Heading, Input, Select, SimpleGrid, Stack, Text, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import Navigation from '../components/misc/Navigation';
import { useMyTeams } from '../context/MyTeamsContext';

export default function AccountPage() {
  const { user, userPermissions, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];
  const { teams, claims, loading: teamsLoading, refresh } = useMyTeams();
  const [connection, setConnection] = useState(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [challenge, setChallenge] = useState(null);
  const [code, setCode] = useState('');
  const [game, setGame] = useState('BB3');
  const [candidates, setCandidates] = useState([]);
  const [selected, setSelected] = useState([]);
  const [adminClaims, setAdminClaims] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const reloadClaims = async (selectedGame = game) => { await refresh(); setCandidates(await WarpScoresApiService.coachClaimCandidates(selectedGame, ...auth)); if(userPermissions?.writeSiteAdmin)setAdminClaims(await WarpScoresApiService.adminCoachClaims(...auth)); };

  const accept = (result) => {
    if (result.status === 'GUARD_REQUIRED') setChallenge(result);
    if (result.status === 'AUTHENTICATED') { setChallenge(null); setConnection({ connected: true, steamUsername: result.steamUsername, steamId: result.steamId }); reloadClaims(); }
    if (result.status === 'DISCONNECTED') { setConnection({ connected: false, steamUsername: username }); refresh(); }
  };
  const run = async (action) => {
    setBusy(true); setError('');
    try { accept(await action()); } catch (reason) { setError(reason?.response?.data?.message || reason?.message || 'Authentication failed'); }
    finally { setPassword(''); setBusy(false); }
  };

  useEffect(() => {
    WarpScoresApiService.steamConnection(...auth).then((value) => { setConnection(value); setUsername(value.steamUsername || ''); }).catch((reason) => setError(reason.message));
    WarpScoresApiService.coachClaimCandidates(game, ...auth).then(setCandidates).catch((reason)=>setError(reason.message));
    if(userPermissions?.writeSiteAdmin)WarpScoresApiService.adminCoachClaims(...auth).then(setAdminClaims).catch((reason)=>setError(reason.message));
    // Token functions are stable in Auth0; loading once is intentional.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userPermissions?.writeSiteAdmin]);

  const toggle = id => setSelected(values => values.includes(id) ? values.filter(value => value !== id) : [...values, id]);

  return <VStack align="stretch"><Navigation currentPage="account" />
    <Stack spacing={6} maxW="3xl">
      <Box><Heading size="lg">BlaskScore account</Heading><Text>{user?.name || user?.email}</Text>
        <Text fontSize="sm" color="gray.500">Steam is optional. Coach claims determine which historical teams and players are yours.</Text></Box>
      {error && <Alert status="error"><AlertIcon />{error}</Alert>}
      <Box borderWidth="1px" borderRadius="md" p={5}>
        <Heading size="md" mb={3}>My coaches</Heading>
        {claims.length===0?<Text color="gray.500" mb={4}>No coaches claimed yet.</Text>:<Stack mb={5}>{claims.map(claim=><Box key={claim.id} borderWidth="1px" borderRadius="md" p={3}><Text fontWeight="bold">{claim.coachName}</Text><Text fontSize="sm" color="gray.500">{claim.game} · {claim.source==='STEAM_LOGIN'?'claimed from Steam':'manually claimed'}</Text><Button mt={2} size="sm" variant="outline" onClick={()=>run(async()=>{await WarpScoresApiService.releaseCoachClaim(claim.id,...auth);await reloadClaims();return{}})}>Release claim</Button></Box>)}</Stack>}
        <FormControl mb={3}><FormLabel>Blood Bowl version</FormLabel><Select maxW="12rem" value={game} onChange={async e=>{const value=e.target.value;setGame(value);setSelected([]);setCandidates(await WarpScoresApiService.coachClaimCandidates(value,...auth))}}><option value="BB1">Blood Bowl 1</option><option value="BB2">Blood Bowl 2</option><option value="BB3">Blood Bowl 3</option></Select></FormControl>
        <Text fontSize="sm" color="gray.500" mb={3}>Select your coach names. Internal coach IDs are deliberately hidden.</Text>
        <Stack>{candidates.map(candidate=><Box key={candidate.coachId} borderWidth="1px" borderRadius="md" p={3}><Checkbox isChecked={selected.includes(candidate.coachId)} onChange={()=>toggle(candidate.coachId)}><Text as="span" fontWeight="bold">{candidate.coachName}</Text></Checkbox><Text fontSize="sm" color="gray.500">{candidate.teamCount} team{candidate.teamCount===1?'':'s'}{candidate.teamNames?.length?` · ${candidate.teamNames.join(', ')}`:''}</Text></Box>)}</Stack>
        <Button mt={4} colorScheme="blue" isDisabled={!selected.length} isLoading={busy} onClick={()=>run(async()=>{await WarpScoresApiService.claimCoaches(game,selected,...auth);setSelected([]);await reloadClaims();return{}})}>Claim selected coaches</Button>
      </Box>
      <Box borderWidth="1px" borderRadius="md" p={5}>
        <Heading size="md" mb={3}>Blood Bowl 3 / Steam</Heading>
        {connection?.connected ? <Stack>
          <Text>Connected as <strong>{connection.steamUsername}</strong></Text>
          <Text fontSize="sm" color="gray.500">The BB3 coach exposed by this Steam session is claimed automatically if unclaimed.</Text>
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
      {userPermissions?.writeSiteAdmin&&<Box borderWidth="1px" borderRadius="md" p={5}><Heading size="md" mb={3}>Coach claims administration</Heading><Text fontSize="sm" color="gray.500" mb={3}>Remove a bad claim; the correct user can then claim that coach.</Text><Stack>{adminClaims.map(claim=><Box key={claim.id} borderWidth="1px" borderRadius="md" p={3}><Text fontWeight="bold">{claim.coachName} <Text as="span" fontWeight="normal">({claim.game})</Text></Text><Text fontSize="sm">{claim.userDisplayName||claim.userEmail||claim.authSubject} · {claim.source}</Text><Button mt={2} size="sm" colorScheme="red" variant="outline" onClick={()=>run(async()=>{await WarpScoresApiService.adminRemoveCoachClaim(claim.id,...auth);await reloadClaims();return{}})}>Remove claim</Button></Box>)}</Stack></Box>}
    </Stack>
  </VStack>;
}
