import { useAuth0 } from '@auth0/auth0-react';
import { useEffect, useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';
import logger from '../util/logger';
import config from '../config';

const { isProduction } = config;
const noPermissions = {
  readCurrentUser: false,
  writeLeagueAdmin: false,
  writeSiteAdmin: false,
  writeRegisterLeague: false,
};

let useAuth0WithUserPermissions;

if (isProduction)  {
  useAuth0WithUserPermissions = function useAuth0WithUserPermissions() {
    const { user, isAuthenticated, isLoading, loginWithPopup, logout, getAccessTokenSilently, getAccessTokenWithPopup } =
      useAuth0();
      const [userPermissions, setUserPermissions] = useState(noPermissions);
      const [permissionsLoading, setPermissionsLoading] = useState(isLoading);
      const [permissionsError, setPermissionsError] = useState(null);

    useEffect(() => {
        let active = true;

        if (isLoading) {
          setPermissionsLoading(true);
          return () => {
            active = false;
          };
      }

        if (!isAuthenticated) {
          setUserPermissions(noPermissions);
          setPermissionsError(null);
          setPermissionsLoading(false);
          return () => {
            active = false;
          };
        }

        setPermissionsLoading(true);
        setPermissionsError(null);
        WarpScoresApiService.userPermissions(getAccessTokenSilently, getAccessTokenWithPopup, true)
          .then((permissions) => {
            if (active) {
              setUserPermissions(permissions || noPermissions);
            }
          })
          .catch((reason) => {
            logger.error('Unable to get permissions from backend.', reason);
            if (active) {
              setUserPermissions(noPermissions);
              setPermissionsError(reason);
            }
          })
          .finally(() => {
            if (active) {
              setPermissionsLoading(false);
            }
          });

        return () => {
          active = false;
        };
      }, [getAccessTokenSilently, getAccessTokenWithPopup, isAuthenticated, isLoading]);

      const checkPermissions = !isLoading;
      const loadUserPermissions = checkPermissions && isAuthenticated;
      const authenticationReady = checkPermissions && !permissionsLoading;

    return {
      user,
      authenticationReady,
      checkPermissions,
      loadUserPermissions,
      userPermissions,
      permissionsError,
      isAuthenticated,
      loginWithPopup,
      logout,
      getAccessTokenSilently,
      getAccessTokenWithPopup,
    };
  };
} else {
  useAuth0WithUserPermissions = require('../components/misc/MockAuthProvider').useAuth0WithUserPermissions;
}

export default useAuth0WithUserPermissions;
