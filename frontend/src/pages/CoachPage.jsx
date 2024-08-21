import React, { useEffect, useState } from 'react';
import { Box, VStack } from '@chakra-ui/react';
import { useAuth0 } from '@auth0/auth0-react';
import { FaRegCircleCheck, FaRegCircleStop } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function PermissionIcon({ granted }) {
  const color = granted ? 'green' : 'red';
  const icon = granted ? FaRegCircleCheck : FaRegCircleStop;
  return <Icon as={icon} color={color} />;
}

function CoachPage() {
  const { user, isAuthenticated, isLoading, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0();
  const [error, setError] = useState();
  const [loading, setLoading] = useState();
  const [userPermissions, setUserPermissions] = useState({
    readCurrentUser: false,
    writeLeagueAdmin: false,
    writeSiteAdmin: false,
    writeRegisterLeague: false
  });
  const [userName, setUserName] = useState();

  useEffect(() => {
    setLoading(true);
    const fetchUserPermissions = () => {
      WarpScoresApiService.userPermissions(getAccessTokenSilently, getAccessTokenWithPopup)
          .catch((reason) => {
            setError({ type: 'error', message: reason.toLocaleString(config.locale) });
          })
          .then(setUserPermissions)
          .finally(() => setLoading(false));
    };
    const fetchUser = () => {
      WarpScoresApiService.user(getAccessTokenSilently, getAccessTokenWithPopup)
          .catch((reason) => {
            setError({ type: 'error', message: reason.toLocaleString(config.locale) });
          })
          .then(setUserName);
    };
    fetchUserPermissions();
    fetchUser();
  }, [isLoading, isAuthenticated]);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="coach" />
      </Box>
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <HeaderCard
          mainImageSrc={user?.picture}
          mainImageBorderRadius="full"
          heading="Coach-Page"
          subHeading={user && `Authenticated as ${user?.name}`}
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
