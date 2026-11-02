import React, { useEffect } from 'react';
import { Box, Button, Heading, SimpleGrid, Stack, Text, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import { FaRegCircleCheck, FaRegCircleStop } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import config from '../config';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import imageUrls from '../imageUrls';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { useMyTeams } from '../context/MyTeamsContext';
import { useIntl } from 'react-intl';

const { isProduction } = config;

function PermissionIcon({ granted }) {
  const color = granted ? 'green' : 'red';
  const icon = granted ? FaRegCircleCheck : FaRegCircleStop;
  return <Icon as={icon} color={color} />;
}

function CoachPage() {
  const intl = useIntl();
  const { user, authenticationReady, checkPermissions, userPermissions } = useAuth0WithUserPermissions();
  const { teams, loading: teamsLoading, teamsError, refresh } = useMyTeams();
  const navigate = useNavigate();

  useEffect(() => {
    if (authenticationReady && checkPermissions && !userPermissions.readCurrentUser) {
      navigate('/');
    }
  }, [authenticationReady, checkPermissions, userPermissions]);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="coach" />
      </Box>
      <LoadingOrErrorWrapper loading={!authenticationReady}>
        <HeaderCard
          mainImageSrc={
            isProduction ? authenticationReady && user && user.picture : imageUrls.blaskscoreLogoPng('medium')
          }
          mainImageBorderRadius="full"
          heading={intl.formatMessage({ id: 'coachPage.title' })}
          subHeading={
            isProduction
              ? authenticationReady && user && intl.formatMessage({ id: 'coachPage.authenticatedAs' }, { name: user.name })
              : intl.formatMessage({ id: 'coachPage.devUser' })
          }
        />
        <Box borderWidth="1px" borderRadius="md" p={5}>
          <Heading size="md" mb={3}>{intl.formatMessage({ id: 'account.myTeams' })}</Heading>
          {teamsLoading ? <Text>{intl.formatMessage({ id: 'account.loadingTeams' })}</Text> : teamsError ? <Stack align="start">
            <Text color="red.500">{intl.formatMessage({ id: 'account.teamsLoadFailed' })}</Text>
            <Text fontSize="sm" color="gray.500">{teamsError}</Text>
            <Button size="sm" variant="outline" onClick={refresh} isLoading={teamsLoading}>{intl.formatMessage({ id: 'account.retryTeams' })}</Button>
          </Stack> : teams.length === 0 ? <Text>{intl.formatMessage({ id: 'account.noTeams' })}</Text> :
            <SimpleGrid columns={{ base: 1, md: 2 }} spacing={3}>{teams.map((team) => {
              const teamId = String(team.id);
              const teamPageId = teamId.startsWith('3_') ? teamId : `3_${teamId}`;
              return <Box
                as={RouteLink}
                to={`/team/${encodeURIComponent(teamPageId)}`}
                key={team.id}
                borderWidth="1px"
                borderRadius="md"
                p={3}
                _hover={{ borderColor: 'teal.400', textDecoration: 'none' }}
                _focusVisible={{ outline: '2px solid', outlineColor: 'teal.400' }}
              >
                <Text fontWeight="bold">{team.name || team.id}</Text>
                <Text fontSize="sm">{intl.formatMessage({ id: 'account.raceTv' }, { race: team.raceId ?? intl.formatMessage({ id: 'common.unknown' }), tv: team.teamValue ?? intl.formatMessage({ id: 'common.unknown' }) })}</Text>
              </Box>;
            })}</SimpleGrid>}
        </Box>
        <Box>
          <PermissionIcon granted={userPermissions?.readCurrentUser} /> {intl.formatMessage({ id: 'coachPage.readPermission' })}
        </Box>
        <Box>
          <PermissionIcon granted={userPermissions?.writeRegisterLeague} /> {intl.formatMessage({ id: 'coachPage.registerLeaguePermission' })}
        </Box>
        <Box>
          <PermissionIcon granted={userPermissions?.writeLeagueAdmin} /> {intl.formatMessage({ id: 'coachPage.leagueAdminPermission' })}
        </Box>
        <Box>
          <PermissionIcon granted={userPermissions?.writeSiteAdmin} /> {intl.formatMessage({ id: 'coachPage.siteAdminPermission' })}
        </Box>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CoachPage;
