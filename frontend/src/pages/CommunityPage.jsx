import React, { useEffect, useMemo, useState } from 'react';
import {
  Avatar, Badge, Box, Button, Card, CardBody, Heading, Text, SimpleGrid, FormControl, FormLabel, Select
} from '@chakra-ui/react';
import { Link as RouteLink, useSearchParams } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import CommunityApi from '../CommunityApi';
import { useIntl } from 'react-intl';
import { selectMembers } from '../util/communityDirectory';
import WarpScoresApiService from '../WarpScoresApiService';

function CommunityPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const systemId = searchParams.get('leagueSystem');
  const [systems, setSystems] = useState([]);
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const [seasons, setSeasons] = useState([]);
  const defaults = { team: '', season: '', race: '', species: '', status: 'active', sort: 'name', direction: 'asc' };
  const [filters, setFilters] = useState(defaults);
  const [fans, setFans] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    setFans(null); setError(null); setSeasons([]); setFilters(defaults);
    const load = async () => {
      const available = await WarpScoresApiService.publicLeagueSystems();
      if (!active) return;
      setSystems(available);
      const selected = available.find(s => s.id === systemId) || (!systemId && (available.find(s => s.primary) || available[0]));
      if (!selected) { setFans([]); return; }
      if (!systemId) { setSearchParams({ leagueSystem: selected.id }, { replace: true }); return; }
      const data = await CommunityApi.directory(selected.id);
      if (!active) return;
      setFans(data.members.map(m => ({ ...m.profile, commentCount: m.commentCount, seasonIds: m.seasonIds,
        seasonNames: m.seasonIds.map(id => data.seasons.find(s => s.id === id)?.name || id).sort() })));
      setSeasons(data.seasons);
    };
    load().catch(e => { if (active) setError(e); });
    return () => { active = false; };
  }, [systemId]);
  const visible = useMemo(() => selectMembers(fans || [], filters, intl.locale), [fans, filters, intl.locale]);
  const options = key => [...new Set((fans || []).map(p => p[key]).filter(Boolean))].sort((a, b) => a.localeCompare(b, intl.locale));
  const select = (key, label, values, all = true) => <FormControl><FormLabel>{label}</FormLabel>
    <Select value={filters[key]} onChange={e => setFilters(f => ({ ...f, [key]: e.target.value }))}>
      {all && <option value="">{t('community.all')}</option>}
      {values.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
    </Select></FormControl>;

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="community" leagueSystems={systems} selectedLeagueSystemId={systemId} onSelectLeagueSystem={id => setSearchParams({ leagueSystem: id })} />
      <Heading mt={6}>Community</Heading>
      <Text mt={2} fontWeight="bold">{systems.find(s => s.id === systemId)?.name}</Text>
      <Text mt={2} color="gray.400">
        Supporters and other community members around the Blood Bowl world.
      </Text>
      <SimpleGrid mt={5} columns={{ base: 1, md: 3, xl: 4 }} spacing={3}>
        {select('team', t('community.team'), options('teamId').map(id => [id, fans.find(p => p.teamId === id)?.teamName || id]))}
        {select('season', t('community.season'), seasons.map(s => [s.id, `${s.name || s.number || s.id} · ${s.leagueSystemId}`]))}
        {select('race', t('community.race'), options('teamRace').map(r => [r, r]))}
        {select('species', t('community.species'), options('species').map(r => [r, r]))}
        {select('status', t('community.status'), [['active', t('community.active')], ['inactive', t('community.inactive')]])}
        {select('sort', t('community.sort'), ['name', 'joined', 'comments', 'team', 'season', 'race', 'status'].map(key => [key, t(`community.${key}`)]), false)}
        {select('direction', t('community.direction'), [['asc', t('community.ascending')], ['desc', t('community.descending')]], false)}
      </SimpleGrid>
      <Text mt={2} fontSize="sm">{t('community.seasonHelp')}</Text>
      <Button mt={3} size="sm" onClick={() => setFilters(defaults)}>{t('community.reset')}</Button>
      {fans && <Text mt={3}>{intl.formatMessage({ id: 'community.results' }, { count: visible.length, total: fans.length })}</Text>}

      {error && <Text mt={6} color="red.300">{error.message || String(error)}</Text>}
      {!fans && !error && <Text mt={8}>Loading community...</Text>}

      <Box
        mt={6}
        display="grid"
        gridTemplateColumns="repeat(auto-fill, minmax(min(100%, 210px), 1fr))"
        gap={4}
      >
        {visible.map((fan) => (
          <Card key={fan.id} overflow="hidden">
            <Box
              as={RouteLink}
              to={`/community/${encodeURIComponent(fan.id)}?leagueSystem=${encodeURIComponent(systemId)}`}
              display="block"
              h="100%"
              _hover={{ textDecoration: 'none' }}
            >
              {fan.profileImageUrl && (
                <Box
                  h="150px"
                  backgroundImage={`url(${CommunityApi.assetUrl(fan.profileImageUrl)})`}
                  backgroundSize="cover"
                  backgroundPosition="center"
                />
              )}
              <CardBody textAlign="center">
                <Avatar
                  mt={fan.profileImageUrl ? -12 : 0}
                  mb={3}
                  size="xl"
                  name={fan.displayName}
                  src={CommunityApi.assetUrl(fan.avatarImageUrl || fan.profileImageUrl)}
                  borderWidth={fan.profileImageUrl ? '4px' : 0}
                  borderColor="gray.700"
                />
                <Heading size="sm">{fan.displayName}</Heading>
                <Box mt={2}>
                  {fan.species && <Badge mr={1}>{fan.species}</Badge>}
                  {fan.supporterArchetype && <Badge colorScheme="purple">{fan.supporterArchetype}</Badge>}
                </Box>
                <Text mt={2} color="gray.400" fontSize="sm">{fan.teamName}</Text>
                <Text fontSize="sm">{t(fan.active ? 'community.active' : 'community.inactive')} · {intl.formatMessage({ id: 'community.commentCount' }, { count: fan.commentCount })}</Text>
                <Text fontSize="xs">{t('community.joined')}: {fan.createdAt ? intl.formatDate(fan.createdAt) : '—'}</Text>
                {fan.bio && <Text mt={2} color="gray.500" fontSize="xs" noOfLines={3}>{fan.bio}</Text>}
              </CardBody>
            </Box>
          </Card>
        ))}
      </Box>
    </Box>
  );
}

export default CommunityPage;
