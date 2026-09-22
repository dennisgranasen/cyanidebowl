export const LANGUAGES = [
  { code: 'sv', name: 'Svenska', intlLocale: 'sv', htmlLang: 'sv', experimental: false },
  { code: 'en', name: 'English', intlLocale: 'en', htmlLang: 'en', experimental: false },
  { code: 'es', name: 'Español', intlLocale: 'es', htmlLang: 'es', experimental: false },
  { code: 'fi', name: 'Suomi', intlLocale: 'fi', htmlLang: 'fi', experimental: false },
  { code: 'pl', name: 'Polski', intlLocale: 'pl', htmlLang: 'pl', experimental: false },
  {
    code: 'sindarin', name: 'Sindarin', intlLocale: 'en',
    htmlLang: 'art-x-sindarin', experimental: true,
  },
  {
    code: 'quenya', name: 'Quenya', intlLocale: 'en',
    htmlLang: 'art-x-quenya', experimental: true,
  },
  {
    code: 'khuzdul', name: 'Khuzdul', intlLocale: 'en',
    htmlLang: 'art-x-khuzdul', experimental: true,
  },
  {
    code: 'entish', name: 'Entiska / Entish', intlLocale: 'en',
    htmlLang: 'art-x-entish', experimental: true,
  },
  {
    code: 'nandorin', name: 'Nandorin', intlLocale: 'en',
    htmlLang: 'art-x-nandorin', experimental: true,
  },
  {
    code: 'black-speech', name: 'Svartspråket / Black Speech', intlLocale: 'en',
    htmlLang: 'art-x-black-speech', experimental: true,
  },
];

export const DEFAULT_LOCALE = 'sv';
export const languageFor = (code) => LANGUAGES.find((language) => language.code === code)
  || LANGUAGES.find((language) => language.code === DEFAULT_LOCALE);
export const isSupportedLocale = (code) => LANGUAGES.some((language) => language.code === code);
