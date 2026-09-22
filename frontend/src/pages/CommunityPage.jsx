import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Avatar, Badge, Box, Button, Card, CardBody, Heading, Image, Text, SimpleGrid, FormControl, FormLabel, Select
} from '@chakra-ui/react';
import { Link as RouteLink, useSearchParams } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import CommunityApi from '../CommunityApi';
import { useIntl } from 'react-intl';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { claimedCoachIds, MY_COACHES } from '../util/claimedCoachIds';

const PAGE_SIZE = 24;
const DEFAULT_FILTERS = {
  team: '', coach: '', season: '', race: '', species: '', status: 'active', sort: 'name', direction: 'asc',
};
const EMPTY_FACETS = { teams: [], coaches: [], races: [], species: [] };

function CommunityPage() {
  const { isAuthenticated, authenticationReady, user, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const [coachClaims, setCoachClaims] = useState({ subject: null, ids: [] });
  const myCoachIds = isAuthenticated && coachClaims.subject === user?.sub ? coachClaims.ids : [];
  const myCoachKey = myCoachIds.join(',');
  const [searchParams, setSearchParams] = useSearchParams();
  const systemId = searchParams.get('leagueSystem');
  const [systems, setSystems] = useState([]);
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const [seasons, setSeasons] = useState([]);
  const [facets, setFacets] = useState(EMPTY_FACETS);
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [fans, setFans] = useState(null);
  const [pageInfo, setPageInfo] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  const requestVersion = useRef(0);
  const loadMoreRef = useRef(null);

  useEffect(() => {
    let active = true;
    setCoachClaims({ subject: null, ids: [] });
    if (authenticationReady && isAuthenticated) {
      WarpScoresApiService.coachClaims(getAccessTokenSilently, getAccessTokenWithPopup)
        .then(claims => { if (active) setCoachClaims({ subject: user?.sub, ids: claimedCoachIds(claims) }); })
        .catch(() => { if (active) setCoachClaims({ subject: null, ids: [] }); });
    }
    return () => { active = false; };
  }, [authenticationReady, isAuthenticated, user?.sub, getAccessTokenSilently, getAccessTokenWithPopup]);

  useEffect(() => {
    if (filters.coach === MY_COACHES && !myCoachKey) {
      setFilters(current => ({ ...current, coach: '' }));
    }
  }, [myCoachKey, filters.coach]);

  useEffect(() => {
    let active = true;
    const loadSystems = async () => {
      const available = await WarpScoresApiService.publicLeagueSystems();
      if (!active) return;
      setSystems(available);
      const selected = available.find(s => s.id === systemId)
        || (!systemId && (available.find(s => s.primary) || available[0]));
      if (!selected) {
        setFans([]);
        return;
      }
      if (!systemId) {
        setSearchParams({ leagueSystem: selected.id }, { replace: true });
      }
    };
    loadSystems().catch(e => { if (active) setError(e); });
    return () => { active = false; };
  }, [systemId, setSearchParams]);

  useEffect(() => {
    if (!systemId) return undefined;
    let active = true;
    const version = ++requestVersion.current;
    setFans(null);
    setPageInfo(null);
    setError(null);
    setLoadingMore(false);

    CommunityApi.directory(systemId, {
      ...filters,
      coach: filters.coach === MY_COACHES ? (myCoachKey || MY_COACHES) : filters.coach,
      locale: intl.locale,
      page: 0,
      size: PAGE_SIZE,
    }).then(data => {
      if (!active || version !== requestVersion.current) return;
      setFans(data.members || []);
      setSeasons(data.seasons || []);
      setFacets(data.facets || EMPTY_FACETS);
      setPageInfo(data);
    }).catch(e => {
      if (active && version === requestVersion.current) setError(e);
    });

    return () => { active = false; };
  }, [systemId, filters, intl.locale, myCoachKey]);

  const loadMore = useCallback(async () => {
    if (!systemId || !pageInfo?.hasMore || loadingMore) return;
    const version = requestVersion.current;
    setLoadingMore(true);
    try {
      const data = await CommunityApi.directory(systemId, {
        ...filters,
        coach: filters.coach === MY_COACHES ? (myCoachKey || MY_COACHES) : filters.coach,
        locale: intl.locale,
        page: pageInfo.page + 1,
        size: PAGE_SIZE,
      });
      if (version !== requestVersion.current) return;
      setFans(current => [...(current || []), ...(data.members || [])]);
      setPageInfo(data);
      setSeasons(data.seasons || []);
      setFacets(data.facets || EMPTY_FACETS);
    } catch (e) {
      if (version === requestVersion.current) setError(e);
    } finally {
      if (version === requestVersion.current) setLoadingMore(false);
    }
  }, [systemId, pageInfo, loadingMore, filters, intl.locale, myCoachKey]);

  useEffect(() => {
    const node = loadMoreRef.current;
    if (!node || !pageInfo?.hasMore || loadingMore) return undefined;
    const observer = new IntersectionObserver(entries => {
      if (entries[0]?.isIntersecting) loadMore();
    }, { rootMargin: '300px' });
    observer.observe(node);
    return () => observer.disconnect();
  }, [loadMore, pageInfo?.hasMore, loadingMore]);

  const select = (key, label, values, all = true) => <FormControl><FormLabel>{label}</FormLabel>
    <Select value={filters[key]} onChange={e => setFilters(f => ({ ...f, [key]: e.target.value }))}>
      {all && <option value="">{t('community.all')}</option>}
      {values.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
    </Select></FormControl>;

  const changeLeagueSystem = id => {
    requestVersion.current += 1;
    setFilters(DEFAULT_FILTERS);
    setSearchParams({ leagueSystem: id });
  };

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation
        currentPage="community"
        leagueSystems={systems}
        selectedLeagueSystemId={systemId}
        onSelectLeagueSystem={changeLeagueSystem}
      />
      <Heading mt={6}>Community</Heading>
      <Text mt={2} fontWeight="bold">{systems.find(s => s.id === systemId)?.name}</Text>
      <Text mt={2} color="gray.400">
        Supporters and other community members around the Blood Bowl world.
      </Text>
      <SimpleGrid mt={5} columns={{ base: 1, md: 3, xl: 4 }} spacing={3}>
        {select('team', t('community.team'), facets.teams.map(team => [team.id, team.name]))}
        {select('coach', t('community.coach'), [...(myCoachIds.length ? [[MY_COACHES, t('community.myCoaches')]] : []), ...(facets.coaches || []).map(coach => [coach.id, coach.name])])}
        {select('season', t('community.season'), seasons.map(s => [s.id, `${s.name || s.number || s.id} · ${s.leagueSystemId}`]))}
        {select('race', t('community.race'), facets.races.map(r => [r, r]))}
        {select('species', t('community.species'), facets.species.map(s => [s, s]))}
        {select('status', t('community.status'), [['active', t('community.active')], ['inactive', t('community.inactive')]])}
        {select('sort', t('community.sort'), ['name', 'joined', 'comments', 'team', 'season', 'race', 'status'].map(key => [key, t(`community.${key}`)]), false)}
        {select('direction', t('community.direction'), [['asc', t('community.ascending')], ['desc', t('community.descending')]], false)}
      </SimpleGrid>
      <Text mt={2} fontSize="sm">{t('community.seasonHelp')}</Text>
      <Button mt={3} size="sm" onClick={() => setFilters(DEFAULT_FILTERS)}>{t('community.reset')}</Button>
      {pageInfo && (
        <Text mt={3}>
          {intl.formatMessage(
            { id: 'community.results' },
            { count: pageInfo.filteredTotal, total: pageInfo.total }
          )}
        </Text>
      )}

      {error && <Text mt={6} color="red.300">{error.message || String(error)}</Text>}
      {!fans && !error && <Text mt={8}>Loading community...</Text>}

      <Box
        mt={6}
        display="grid"
        gridTemplateColumns="repeat(auto-fill, minmax(min(100%, 210px), 1fr))"
        gap={4}
      >
        {(fans || []).map((fan) => (
          <Card key={fan.id} overflow="hidden">
            <Box
              as={RouteLink}
              to={`/community/${encodeURIComponent(fan.id)}?leagueSystem=${encodeURIComponent(systemId)}`}
              display="block"
              h="100%"
              _hover={{ textDecoration: 'none' }}
            >
              {fan.profileImageUrl && (
                <Image
                  src={CommunityApi.assetUrl(fan.profileImageUrl)}
                  alt=""
                  h="150px"
                  w="100%"
                  objectFit="cover"
                  loading="lazy"
                  decoding="async"
                />
              )}
              <CardBody textAlign="center">
                <Avatar
                  mt={fan.profileImageUrl ? -12 : 0}
                  mb={3}
                  size="xl"
                  name={fan.displayName}
                  src={CommunityApi.assetUrl(fan.avatarImageUrl || fan.profileImageUrl)}
                  loading="lazy"
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

      {pageInfo?.hasMore && (
        <Box ref={loadMoreRef} py={6} textAlign="center">
          <Button onClick={loadMore} isLoading={loadingMore}>
            {intl.formatMessage({ id: 'community.loadMore', defaultMessage: 'Load more' })}
          </Button>
        </Box>
      )}
    </Box>
  );
}

export default CommunityPage;
