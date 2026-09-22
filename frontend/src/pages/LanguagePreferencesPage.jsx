import React, { useState } from 'react';
import {
  Alert,
  AlertIcon,
  Box,
  Heading,
  Stack,
  Text,
} from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import Navigation from '../components/misc/Navigation';
import LanguageSelect from '../components/common/LanguageSelect';
import { useLocale } from '../i18n/I18nProvider';

export default function LanguagePreferencesPage() {
  const intl = useIntl();
  const {
    locale,
    userLocale,
    authenticated,
    setLocale,
    languages,
  } = useLocale();
  const [notice, setNotice] = useState(null);

  const change = async (nextLocale) => {
    setNotice(null);
    try {
      await setLocale(nextLocale);
      setNotice({ status: 'success', message: intl.formatMessage({ id: 'language.saved' }) });
    } catch (error) {
      setNotice({ status: 'error', message: intl.formatMessage({ id: 'language.saveError' }) });
    }
  };

  return (
    <Stack>
      <Navigation currentPage="language" />
      <Box maxW="xl" borderWidth="1px" borderRadius="md" p={5}>
        <Heading size="lg" mb={3}>{intl.formatMessage({ id: 'language.heading' })}</Heading>
        <Text mb={4}>{intl.formatMessage({ id: 'language.description' })}</Text>
        <LanguageSelect
          value={authenticated ? userLocale : locale}
          onChange={change}
          allowFollowDefault={authenticated}
        />
        {languages.some((language) => language.experimental) && (
          <Text mt={3} fontSize="sm" color="gray.500">
            {intl.formatMessage({ id: 'language.experimentalHelp' })}
          </Text>
        )}
        {notice && (
          <Alert status={notice.status} mt={4}>
            <AlertIcon />
            {notice.message}
          </Alert>
        )}
      </Box>
    </Stack>
  );
}
