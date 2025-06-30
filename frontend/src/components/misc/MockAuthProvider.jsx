import React, { createContext, useContext } from 'react';

const Auth0Context = createContext({
  isAuthenticated: true,
  authenticationReady: true,
  isLoading: false,
  userPermissions: { writeLeagueAdmin: true, writeSiteAdmin: true, readCurrentUser: true },
  user: { name: 'Dev User', email: 'dev@example.com' },
  getAccessTokenSilently: async () => 'dev-token',
  getAccessTokenWithPopup: async () => 'dev-token',
  loginWithRedirect: () => {},
  logout: () => {},
});

export const useAuth0 = () => useContext(Auth0Context);
export const useAuth0WithUserPermissions = () => useContext(Auth0Context);

export const MockAuth0Provider = ({ children }) => (
  <Auth0Context.Provider value={{
    isAuthenticated: true,
    authenticationReady: true,
    isLoading: false,
    userPermissions: { writeLeagueAdmin: true, writeSiteAdmin: true, readCurrentUser: true },
    user: { name: 'Dev User', email: 'dev@example.com' },
    getAccessTokenSilently: async () => 'dev-token',
    getAccessTokenWithPopup: async () => 'dev-token',
    loginWithRedirect: () => {},
    logout: () => {},
  }}>
    {children}
  </Auth0Context.Provider>
);