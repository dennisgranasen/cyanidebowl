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

const { isProduction } = config;

function PermissionIcon({ granted }) {
  const color = granted ? 'green' : 'red';
  const icon = granted ? FaRegCircleCheck : FaRegCircleStop;
  return <Icon as={icon} color={color} />;
}

function CoachPage() {
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
            isProduction ? authenticationReady && user && user.picture : imageUrls.warpscoresLogoPng('medium')
          }
          mainImageBorderRadius="full"
          heading="Coach-Page"
          subHeading={
            isProduction
              ? authenticationReady && user && `Authenticated as ${user.name}`
              : 'No real User in dev environment.'
          }
        />
        <Box>
          <PermissionIcon granted={userPermissions?.readCurrentUser} /> User read permissions
        </Box>
        <Box>
          <PermissionIcon granted={userPermissions?.writeRegisterLeague} /> Permission to register leagues
        </Box>
        <Box>
          <PermissionIcon granted={userPermissions?.writeLeagueAdmin} /> League admin permissions
        </Box>
        <Box>
          <PermissionIcon granted={userPermissions?.writeSiteAdmin} /> Site admin permissions
        </Box>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CoachPage;
