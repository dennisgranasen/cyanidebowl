import React, { useEffect } from 'react';
import { Box, VStack } from '@chakra-ui/react';
import { FaRegCircleCheck, FaRegCircleStop } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import config from '../config';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import imageUrls from '../imageUrls';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
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
