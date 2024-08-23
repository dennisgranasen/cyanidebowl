import {useAuth0} from "@auth0/auth0-react";
import {useEffect, useState} from "react";
import WarpScoresApiService from "../WarpScoresApiService";
import logger from "../util/Logger";
import config from "../config";

const {isProduction} = config;

export function useAuth0WithUserPermissions() {
    const {user, isAuthenticated, isLoading, loginWithPopup, logout, getAccessTokenSilently, getAccessTokenWithPopup} =
        useAuth0();
    const [userPermissions, setUserPermissions] = useState({
        readCurrentUser: false,
        writeLeagueAdmin: false,
        writeSiteAdmin: false,
        writeRegisterLeague: false
    });
    const [permissionsLoading, setPermissionsLoading] = useState(true);
    const [authenticationReady, setAuthenticationReady] = useState(false);
    const [checkPermissions, setCheckPermissions] = useState(false);

    useEffect(() => {
        const fetchUserPermissions = () => {
            setPermissionsLoading(true);
            WarpScoresApiService.userPermissions(getAccessTokenSilently, getAccessTokenWithPopup, !isLoading && isAuthenticated)
                .catch((reason) => {
                    logger.error("Unable to get permissions from backend.", reason);
                })
                .finally(() => setPermissionsLoading(false))
                .then(setUserPermissions);
        };
        if (checkPermissions) {
            fetchUserPermissions();
        }
    }, [checkPermissions])

    useEffect(() => {
        setCheckPermissions(!isProduction || !isLoading);
    }, [isLoading]);

    useEffect(() => {
        setAuthenticationReady(!isLoading && !permissionsLoading);
    }, [userPermissions]);

    return {
        user,
        authenticationReady,
        checkPermissions,
        userPermissions,
        isAuthenticated,
        loginWithPopup,
        logout,
        getAccessTokenSilently,
        getAccessTokenWithPopup
    }
}
