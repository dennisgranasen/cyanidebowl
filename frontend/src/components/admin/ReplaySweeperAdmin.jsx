import React,{useEffect,useState} from 'react';
import {Alert,AlertIcon,Badge,Box,Button,Checkbox,FormControl,FormLabel,Heading,HStack,Input,NumberInput,NumberInputField,Stack,Text,VStack} from '@chakra-ui/react';
import WarpScoresApiService from '../../WarpScoresApiService';

export default function ReplaySweeperAdmin({auth}){
  const[status,setStatus]=useState(null),[logs,setLogs]=useState([]),[password,setPassword]=useState(''),[challenge,setChallenge]=useState(null),[code,setCode]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const load=()=>Promise.all([WarpScoresApiService.replaySweeperStatus(...auth),WarpScoresApiService.replaySweeperLogs(...auth)]).then(([s,l])=>{setStatus(s);setLogs(l)}).catch(e=>setError(e.message));
  useEffect(()=>{load()},[]); // eslint-disable-line react-hooks/exhaustive-deps
  const run=async action=>{setBusy(true);setError('');try{return await action()}catch(e){setError(e?.response?.data?.message||e.message)}finally{setPassword('');setBusy(false)}};
  const accept=result=>{if(result.status==='GUARD_REQUIRED')setChallenge(result);if(result.status==='AUTHENTICATED'){setChallenge(null);load()}};
  if(!status)return <Box borderWidth="1px" borderRadius="md" p={4}><Text>Loading replay service…</Text></Box>;
  const needsAuth=!status.credentialConfigured||!status.credentialValid;
  return <Box borderWidth="1px" borderRadius="md" p={4}><Heading size="md" mb={3}>BB3 replay sweeper</Heading>
    {needsAuth&&<Alert status="warning" mb={4}><AlertIcon/>{status.credentialConfigured?'The Steam ticket has expired and must be renewed.':'No replay service Steam account has been authenticated.'}</Alert>}
    {error&&<Alert status="error" mb={4}><AlertIcon/>{error}</Alert>}
    <Stack direction={{base:'column',lg:'row'}} align="end" spacing={3}>
      <Checkbox pb={2} isChecked={status.enabled} onChange={e=>setStatus({...status,enabled:e.target.checked})}>Enabled</Checkbox>
      <FormControl><FormLabel>Steam username</FormLabel><Input value={status.steamUsername||''} onChange={e=>setStatus({...status,steamUsername:e.target.value})}/></FormControl>
      <FormControl><FormLabel>Cron</FormLabel><Input value={status.cron} onChange={e=>setStatus({...status,cron:e.target.value})}/></FormControl>
      <FormControl><FormLabel>Time zone</FormLabel><Input value={status.zoneId} onChange={e=>setStatus({...status,zoneId:e.target.value})}/></FormControl>
      <FormControl maxW="8rem"><FormLabel>Batch size</FormLabel><NumberInput min={1} max={50} value={status.batchSize} onChange={(_,v)=>setStatus({...status,batchSize:v})}><NumberInputField/></NumberInput></FormControl>
      <Button onClick={()=>run(()=>WarpScoresApiService.updateReplaySweeper(status,...auth).then(setStatus))}>Save</Button>
      <Button isLoading={busy} onClick={()=>run(()=>WarpScoresApiService.runReplaySweeper(...auth).then(s=>{setStatus(s);load()}))}>Run now</Button>
    </Stack>
    <Text color="gray.500" fontSize="sm" mt={2}>Default: 05:00 Europe/Stockholm. Last run: {status.lastCompletedAt||'never'} · downloaded: {status.lastDownloaded||0}</Text>
    {challenge?<Stack mt={4} maxW="md"><Text>{challenge.method==='device_confirmation'?'Approve in the Steam app, then continue.':'Enter the Steam Guard code.'}</Text>{challenge.method!=='device_confirmation'&&<Input value={code} onChange={e=>setCode(e.target.value)}/>}<Button colorScheme="blue" isLoading={busy} onClick={()=>run(()=>challenge.method==='device_confirmation'?WarpScoresApiService.confirmReplaySweeperGuard(challenge.challengeId,...auth).then(accept):WarpScoresApiService.replaySweeperGuardCode(challenge.challengeId,code,...auth).then(accept))}>Continue</Button></Stack>:
      needsAuth&&<Stack mt={4} maxW="md"><FormControl><FormLabel>Steam password</FormLabel><Input type="password" value={password} onChange={e=>setPassword(e.target.value)}/></FormControl><Button colorScheme="blue" isDisabled={!status.steamUsername||!password} isLoading={busy} onClick={()=>run(()=>WarpScoresApiService.authenticateReplaySweeper({username:status.steamUsername,password},...auth).then(accept))}>Authenticate replay account</Button></Stack>}
    <Heading size="sm" mt={5} mb={2}>Administrator log</Heading><VStack align="stretch" maxH="16rem" overflowY="auto">{logs.map(entry=><HStack key={entry.id} borderBottomWidth="1px" py={1}><Badge colorScheme={entry.level==='ERROR'?'red':entry.level==='WARN'?'orange':'blue'}>{entry.level}</Badge><Text fontSize="sm" flex="1">{entry.message}</Text><Text color="gray.500" fontSize="xs">{entry.createdAt}</Text></HStack>)}{!logs.length&&<Text color="gray.500">No replay runs logged yet.</Text>}</VStack>
  </Box>;
}
