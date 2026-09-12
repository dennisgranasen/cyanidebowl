import React from 'react';
import { Select } from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import { languageFor } from '../../i18n/languages';
import { useLocale } from '../../i18n/I18nProvider';

export default function LanguageSelect({ value, onChange, allowFollowDefault = false, ...props }) {
  const intl = useIntl();
  const { defaultLocale, languages } = useLocale();
  const defaultLanguage = languageFor(defaultLocale);

  return (
    <Select
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value || null)}
      {...props}
    >
      {allowFollowDefault && (
        <option value="">
          {intl.formatMessage(
            { id: 'language.followDefault' },
            { language: defaultLanguage.name }
          )}
        </option>
      )}
      {languages.map((language) => (
        <option key={language.code} value={language.code}>
          {language.name}
          {language.experimental
            ? ` (${intl.formatMessage({ id: 'language.experimental' })})`
            : ''}
        </option>
      ))}
    </Select>
  );
}
