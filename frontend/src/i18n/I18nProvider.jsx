import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { IntlProvider } from 'react-intl';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import messages from './messages';
import { DEFAULT_LOCALE, LANGUAGES, isSupportedLocale, languageFor } from './languages';

const STORAGE_KEY = 'blaskscore.uiLocale';

const LocaleContext = createContext({
  locale: DEFAULT_LOCALE,
  userLocale: null,
  defaultLocale: DEFAULT_LOCALE,
  languages: LANGUAGES,
  authenticated: false,
  setLocale: async () => {},
});

export const useLocale = () => useContext(LocaleContext);

function storedLocale() {
  try {
    const locale = window.localStorage.getItem(STORAGE_KEY);
    return isSupportedLocale(locale) ? locale : null;
  } catch (error) {
    return null;
  }
}

function writeStoredLocale(locale) {
  try {
    if (locale) window.localStorage.setItem(STORAGE_KEY, locale);
    else window.localStorage.removeItem(STORAGE_KEY);
  } catch (error) {
    // Storage can be unavailable in privacy-restricted browsers.
  }
}

export default function I18nProvider({ children }) {
  const {
    authenticationReady,
    isAuthenticated,
    getAccessTokenSilently,
    getAccessTokenWithPopup,
  } = useAuth0WithUserPermissions();
  const [defaultLocale, setDefaultLocale] = useState(DEFAULT_LOCALE);
  const [userLocale, setUserLocale] = useState(null);
  const [anonymousLocale, setAnonymousLocale] = useState(storedLocale);
  const auth = useMemo(
    () => [getAccessTokenSilently, getAccessTokenWithPopup],
    [getAccessTokenSilently, getAccessTokenWithPopup]
  );

  useEffect(() => {
    WarpScoresApiService.localization()
      .then((settings) => {
        if (isSupportedLocale(settings?.defaultLocale)) {
          setDefaultLocale(settings.defaultLocale);
        }
      })
      .catch(() => setDefaultLocale(DEFAULT_LOCALE));
  }, []);

  useEffect(() => {
    if (!authenticationReady) return;
    if (!isAuthenticated) {
      setUserLocale(null);
      return;
    }
    WarpScoresApiService.userPreferences(...auth)
      .then((preferences) => {
        setUserLocale(isSupportedLocale(preferences?.locale) ? preferences.locale : null);
      })
      .catch(() => setUserLocale(null));
  }, [auth, authenticationReady, isAuthenticated]);

  const locale = (
    isAuthenticated
      ? (userLocale || defaultLocale)
      : (anonymousLocale || defaultLocale)
  );
  const safeLocale = isSupportedLocale(locale) ? locale : DEFAULT_LOCALE;
  const language = languageFor(safeLocale);

  useEffect(() => {
    document.documentElement.lang = language.htmlLang;
  }, [language.htmlLang]);

  const setLocale = useCallback(async (nextLocale) => {
    const normalized = nextLocale && isSupportedLocale(nextLocale) ? nextLocale : null;
    if (isAuthenticated) {
      const saved = await WarpScoresApiService.updateUserPreferences({ locale: normalized }, ...auth);
      setUserLocale(isSupportedLocale(saved?.locale) ? saved.locale : null);
      return;
    }
    writeStoredLocale(normalized);
    setAnonymousLocale(normalized);
  }, [auth, isAuthenticated]);

  const value = useMemo(() => ({
    locale: safeLocale,
    userLocale,
    defaultLocale,
    languages: LANGUAGES,
    authenticated: isAuthenticated,
    setLocale,
  }), [safeLocale, userLocale, defaultLocale, isAuthenticated, setLocale]);

  const localeMessages = {
    ...messages.en,
    ...(messages[safeLocale] || {}),
  };

  return (
    <LocaleContext.Provider value={value}>
      <IntlProvider
        locale={language.intlLocale}
        defaultLocale="en"
        messages={localeMessages}
      >
        {children}
      </IntlProvider>
    </LocaleContext.Provider>
  );
}
