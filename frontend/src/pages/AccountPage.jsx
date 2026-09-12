import React, { useEffect, useState } from 'react';
import { Alert, AlertIcon, Box, Button, Checkbox, FormControl, FormLabel, Heading, Input, Select, SimpleGrid, Stack, Text, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import Navigation from '../components/misc/Navigation';
import { useMyTeams } from '../context/MyTeamsContext';
import { useIntl } from 'react-intl';

export default function AccountPage() {
  const intl = useIntl();
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
    try { accept(await action()); } catch (reason) { setError(reason?.response?.data?.message || reason?.message || intl.formatMessage({ id: 'account.authFailed' })); }
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
      <Box><Heading size="lg">{intl.formatMessage({ id: 'account.heading' })}</Heading><Text>{user?.name || user?.email}</Text>
        <Text fontSize="sm" color="gray.500">{intl.formatMessage({ id: 'account.steamOptional' })}</Text></Box>
      {error && <Alert status="error"><AlertIcon />{error}</Alert>}
      <Box borderWidth="1px" borderRadius="md" p={5}>
        <Heading size="md" mb={3}>{intl.formatMessage({ id: 'account.myCoaches' })}</Heading>
        {claims.length===0?<Text color="gray.500" mb={4}>{intl.formatMessage({ id: 'account.noCoaches' })}</Text>:<Stack mb={5}>{claims.map(claim=><Box key={claim.id} borderWidth="1px" borderRadius="md" p={3}><Text fontWeight="bold">{claim.coachName}</Text><Text fontSize="sm" color="gray.500">{claim.game} · {intl.formatMessage({ id: claim.source==='STEAM_LOGIN' ? 'account.claimedFromSteam' : 'account.manuallyClaimed' })}</Text><Button mt={2} size="sm" variant="outline" onClick={()=>run(async()=>{await WarpScoresApiService.releaseCoachClaim(claim.id,...auth);await reloadClaims();return{}})}>{intl.formatMessage({ id: 'account.releaseClaim' })}</Button></Box>)}</Stack>}
        <FormControl mb={3}><FormLabel>{intl.formatMessage({ id: 'account.bbVersion' })}</FormLabel><Select maxW="12rem" value={game} onChange={async e=>{const value=e.target.value;setGame(value);setSelected([]);setCandidates(await WarpScoresApiService.coachClaimCandidates(value,...auth))}}><option value="BB1">Blood Bowl 1</option><option value="BB2">Blood Bowl 2</option><option value="BB3">Blood Bowl 3</option></Select></FormControl>
        <Text fontSize="sm" color="gray.500" mb={3}>{intl.formatMessage({ id: 'account.selectCoachNames' })}</Text>
        <Stack>{candidates.map(candidate=><Box key={candidate.coachId} borderWidth="1px" borderRadius="md" p={3}><Checkbox isChecked={selected.includes(candidate.coachId)} onChange={()=>toggle(candidate.coachId)}><Text as="span" fontWeight="bold">{candidate.coachName}</Text></Checkbox><Text fontSize="sm" color="gray.500">{intl.formatMessage({ id: 'account.teamCount' }, { count: candidate.teamCount })}{candidate.teamNames?.length?` · ${candidate.teamNames.join(', ')}`:''}</Text></Box>)}</Stack>
        <Button mt={4} colorScheme="blue" isDisabled={!selected.length} isLoading={busy} onClick={()=>run(async()=>{await WarpScoresApiService.claimCoaches(game,selected,...auth);setSelected([]);await reloadClaims();return{}})}>{intl.formatMessage({ id: 'account.claimSelected' })}</Button>
      </Box>
      <Box borderWidth="1px" borderRadius="md" p={5}>
        <Heading size="md" mb={3}>Blood Bowl 3 / Steam</Heading>
        {connection?.connected ? <Stack>
          <Text>{intl.formatMessage({ id: 'account.connectedAs' }, { name: connection.steamUsername })}</Text>
          <Text fontSize="sm" color="gray.500">{intl.formatMessage({ id: 'account.autoClaim' })}</Text>
          <Heading size="sm" pt={2}>{intl.formatMessage({ id: 'account.myTeams' })}</Heading>
          {teamsLoading ? <Text>{intl.formatMessage({ id: 'account.loadingTeams' })}</Text> : teams.length === 0 ? <Text>{intl.formatMessage({ id: 'account.noTeams' })}</Text> :
            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}>{teams.map((team) =>
              <Box key={team.id} borderWidth="1px" borderRadius="md" p={3}>
                <Text fontWeight="bold">{team.name || team.id}</Text>
                <Text fontSize="sm">{intl.formatMessage({ id: 'account.raceTv' }, { race: team.raceId ?? intl.formatMessage({ id: 'common.unknown' }), tv: team.teamValue ?? intl.formatMessage({ id: 'common.unknown' }) })}</Text>
              </Box>)}</SimpleGrid>}
          <Button alignSelf="start" onClick={() => run(async () => { await WarpScoresApiService.disconnectSteam(...auth); return { status: 'DISCONNECTED' }; })} isLoading={busy}>{intl.formatMessage({ id: 'account.disconnect' })}</Button>
        </Stack> : challenge ? <Stack>
          <Text>{challenge.method === 'device_confirmation' ? intl.formatMessage({ id: 'account.approveSteam' }) : intl.formatMessage({ id: 'account.enterGuard' }, { hint: challenge.emailHint || 'none' })}</Text>
          {challenge.method !== 'device_confirmation' && <FormControl><FormLabel>{intl.formatMessage({ id: 'account.guardCode' })}</FormLabel><Input value={code} onChange={(e) => setCode(e.target.value)} autoComplete="one-time-code" /></FormControl>}
          <Button colorScheme="blue" isLoading={busy} onClick={() => run(() => challenge.method === 'device_confirmation' ? WarpScoresApiService.confirmSteamGuard(challenge.challengeId, ...auth) : WarpScoresApiService.submitSteamGuardCode(challenge.challengeId, code, ...auth))}>{intl.formatMessage({ id: 'common.continue' })}</Button>
          <Button variant="ghost" onClick={() => setChallenge(null)}>{intl.formatMessage({ id: 'common.cancel' })}</Button>
        </Stack> : <Stack>
          <Text fontSize="sm">{intl.formatMessage({ id: 'account.credentialsPrivacy' })}</Text>
          <FormControl><FormLabel>{intl.formatMessage({ id: 'account.steamUsername' })}</FormLabel><Input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" /></FormControl>
          <FormControl><FormLabel>{intl.formatMessage({ id: 'account.steamPassword' })}</FormLabel><Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" /></FormControl>
          <Button colorScheme="blue" isDisabled={!username || !password} isLoading={busy} onClick={() => run(() => WarpScoresApiService.startSteamAuthentication({ username, password }, ...auth))}>{intl.formatMessage({ id: 'account.connectSteam' })}</Button>
        </Stack>}
      </Box>
      {userPermissions?.writeSiteAdmin&&<Box borderWidth="1px" borderRadius="md" p={5}><Heading size="md" mb={3}>{intl.formatMessage({ id: 'account.claimAdmin' })}</Heading><Text fontSize="sm" color="gray.500" mb={3}>{intl.formatMessage({ id: 'account.claimAdminHelp' })}</Text><Stack>{adminClaims.map(claim=><Box key={claim.id} borderWidth="1px" borderRadius="md" p={3}><Text fontWeight="bold">{claim.coachName} <Text as="span" fontWeight="normal">({claim.game})</Text></Text><Text fontSize="sm">{claim.userDisplayName||claim.userEmail||claim.authSubject} · {claim.source}</Text><Button mt={2} size="sm" colorScheme="red" variant="outline" onClick={()=>run(async()=>{await WarpScoresApiService.adminRemoveCoachClaim(claim.id,...auth);await reloadClaims();return{}})}>{intl.formatMessage({ id: 'account.removeClaim' })}</Button></Box>)}</Stack></Box>}
    </Stack>
  </VStack>;
}
