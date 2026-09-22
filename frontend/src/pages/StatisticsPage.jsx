import React,{useEffect,useState} from 'react';
import { Button,Checkbox,Heading,HStack,Select,Spinner,Stack,Tab,TabList,TabPanel,TabPanels,Tabs,Text } from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import {CategoryTabs,TeamTable,VersusTable} from '../components/statistics/StatisticsTables';
import {useIntl} from 'react-intl';
export default function StatisticsPage(){const intl=useIntl(),auth=useAuth0WithUserPermissions(),[systems,setSystems]=useState([]),[systemId,setSystemId]=useState(''),[overview,setOverview]=useState(null),[seasonId,setSeasonId]=useState(''),[season,setSeason]=useState(null),[marathon,setMarathon]=useState(null),[edition,setEdition]=useState('ALL'),[merge,setMerge]=useState(false),[page,setPage]=useState(0),[myTeamsOnly,setMyTeamsOnly]=useState(false),[myPlayersOnly,setMyPlayersOnly]=useState(false),[personal,setPersonal]=useState(null),[activeTab,setActiveTab]=useState(0),[loading,setLoading]=useState(true),[error,setError]=useState('');
useEffect(()=>{WarpScoresApiService.publicLeagueSystems().then(s=>{setSystems(s);setSystemId((s.find(x=>x.primary)||s[0])?.id||'')}).catch(e=>setError(e.message))},[]);
useEffect(() => {
  if (!systemId) return undefined;
  let active = true;
  setLoading(true); setError(''); setOverview(null); setSeason(null); setMarathon(null);
  WarpScoresApiService.leagueSystemOverview(systemId).then(o => {
    if (!active) return;
    setOverview(o);
    setSeasonId([...(o.seasons || [])].sort((a,b) => (b.sequence ?? b.number ?? 0) - (a.sequence ?? a.number ?? 0))[0]?.id || '');
  }).catch(e => { if (active) setError(e.message); }).finally(() => { if (active) setLoading(false); });
  return () => { active = false; };
}, [systemId]);
useEffect(() => {
  if (!systemId || activeTab !== 1) return undefined;
  let active = true;
  setLoading(true); setError(''); setMarathon(null);
  WarpScoresApiService.marathonStatistics(systemId,{edition,mergeTeamsByName:merge,page})
    .then(result => { if (active) setMarathon(result); })
    .catch(e => { if (active) setError(e.message); })
    .finally(() => { if (active) setLoading(false); });
  return () => { active = false; };
}, [systemId,edition,merge,page,activeTab]);
useEffect(() => {
  if (!systemId || !seasonId || activeTab !== 0) return undefined;
  let active = true;
  setSeason(null);
  WarpScoresApiService.seasonStatistics(systemId,seasonId).then(result => { if (active) setSeason(result); })
    .catch(e => { if (active) setError(e.message); });
  return () => { active = false; };
}, [systemId,seasonId,activeTab]);
useEffect(() => {
  let active = true;
  setPersonal(null);
  if (systemId && activeTab === 2 && auth.authenticationReady && auth.isAuthenticated)
    WarpScoresApiService.personalStatistics(systemId,auth.getAccessTokenSilently,auth.getAccessTokenWithPopup)
      .then(result => { if (active) setPersonal(result); }).catch(() => { if (active) setPersonal(null); });
  return () => { active = false; };
}, [systemId,activeTab,auth.authenticationReady,auth.isAuthenticated,auth.getAccessTokenSilently,auth.getAccessTokenWithPopup]);
const mineToggle=(type)=><Checkbox mb={3} isChecked={type==='team'?myTeamsOnly:myPlayersOnly} onChange={e=>type==='team'?setMyTeamsOnly(e.target.checked):setMyPlayersOnly(e.target.checked)}>{intl.formatMessage({id:type==='team'?'statistics.myTeamsOnly':'statistics.myPlayersOnly'})}</Checkbox>;
return <Stack><Navigation currentPage="statistics"/><Heading>{intl.formatMessage({id:'statistics.title'})}</Heading><HStack wrap="wrap"><Select maxW="20rem" value={systemId} onChange={e=>{setSystemId(e.target.value);setSeasonId('');setPage(0);setActiveTab(0)}}>{systems.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</Select>{loading&&<Spinner size="sm"/>}</HStack>{error&&<Text color="red.500">{error}</Text>}<Tabs variant="soft-rounded" isLazy index={activeTab} onChange={setActiveTab}><TabList><Tab>{intl.formatMessage({id:'statistics.season'})}</Tab><Tab>{intl.formatMessage({id:'statistics.marathon'})}</Tab>{auth.authenticationReady&&auth.isAuthenticated&&<Tab>{intl.formatMessage({id:'statistics.coachVersus'})}</Tab>}</TabList><TabPanels><TabPanel px={0}><Select mb={4} maxW="20rem" value={seasonId} onChange={e=>setSeasonId(e.target.value)}>{overview?.seasons?.map(s=><option key={s.id} value={s.id}>{s.name||intl.formatMessage({id:'statistics.seasonNumber'},{number:s.number})}</option>)}</Select><Tabs isFitted variant="enclosed"><TabList><Tab>{intl.formatMessage({id:'common.players'})}</Tab><Tab>{intl.formatMessage({id:'common.teams'})}</Tab></TabList><TabPanels><TabPanel px={0}>{mineToggle('player')}<CategoryTabs type="player" categories={season?.players} mineOnly={myPlayersOnly}/></TabPanel><TabPanel px={0}>{mineToggle('team')}<CategoryTabs type="team" categories={season?.teams} mineOnly={myTeamsOnly}/></TabPanel></TabPanels></Tabs></TabPanel><TabPanel px={0}><Stack direction={{base:'column',md:'row'}} mb={4}><Select maxW="14rem" value={edition} onChange={e=>{setEdition(e.target.value);setPage(0)}}><option value="ALL">{intl.formatMessage({id:'statistics.allEditions'})}</option>{marathon?.availableEditions?.map(e=><option key={e}>{e}</option>)}</Select><Checkbox isChecked={merge} onChange={e=>{setMerge(e.target.checked);setPage(0)}}>{intl.formatMessage({id:'statistics.mergeTeams'})}</Checkbox></Stack>{edition==='ALL'&&<Text fontSize="sm" color="gray.500" mb={3}>{intl.formatMessage({id:'statistics.comparableOnly'})}</Text>}<Tabs isFitted variant="enclosed"><TabList><Tab>{intl.formatMessage({id:'common.teams'})}</Tab><Tab>{intl.formatMessage({id:'common.players'})}</Tab></TabList><TabPanels><TabPanel px={0}>{mineToggle('team')}<TeamTable entries={marathon?.teams?.content||[]} mineOnly={myTeamsOnly}/><HStack justify="space-between" mt={3}><Button isDisabled={!page} onClick={()=>setPage(p=>p-1)}>{intl.formatMessage({id:'common.previous'})}</Button><Text>{intl.formatMessage({id:'statistics.page'},{page:page+1,pages:marathon?.teams?.totalPages||1})}</Text><Button isDisabled={page+1>=(marathon?.teams?.totalPages||1)} onClick={()=>setPage(p=>p+1)}>{intl.formatMessage({id:'common.next'})}</Button></HStack></TabPanel><TabPanel px={0}>{mineToggle('player')}<CategoryTabs type="player" categories={marathon?.players} mineOnly={myPlayersOnly}/></TabPanel></TabPanels></Tabs></TabPanel>{auth.authenticationReady&&auth.isAuthenticated&&<TabPanel px={0}>{!personal?.coaches?.length?<Text>{intl.formatMessage({id:'statistics.noClaimedCoaches'})}</Text>:<VersusTable rows={personal?.versus}/>}</TabPanel>}</TabPanels></Tabs></Stack>}
