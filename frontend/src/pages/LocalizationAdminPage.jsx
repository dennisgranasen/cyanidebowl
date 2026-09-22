import React, { useEffect, useState } from 'react';
import {
  Alert,
  AlertIcon,
  Box,
  Button,
  FormControl,
  FormLabel,
  Heading,
  Stack,
  Text,
} from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import Navigation from '../components/misc/Navigation';
import LanguageSelect from '../components/common/LanguageSelect';
import WarpScoresApiService from '../WarpScoresApiService';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { useLocale } from '../i18n/I18nProvider';

export default function LocalizationAdminPage() {
  const intl = useIntl();
  const { defaultLocale } = useLocale();
  const {
    getAccessTokenSilently,
    getAccessTokenWithPopup,
  } = useAuth0WithUserPermissions();
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];
  const [selected, setSelected] = useState(defaultLocale);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);

  useEffect(() => setSelected(defaultLocale), [defaultLocale]);

  const save = async () => {
    setBusy(true);
    setNotice(null);
    try {
      await WarpScoresApiService.updateLocalization({ defaultLocale: selected }, ...auth);
      setNotice({
        status: 'success',
        message: intl.formatMessage({ id: 'localizationAdmin.saved' }),
      });
      window.location.reload();
    } catch (error) {
      setNotice({
        status: 'error',
        message: intl.formatMessage({ id: 'localizationAdmin.saveError' }),
      });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Stack>
      <Navigation currentPage="admin" />
      <Box maxW="xl" borderWidth="1px" borderRadius="md" p={5}>
        <Heading size="lg" mb={3}>
          {intl.formatMessage({ id: 'localizationAdmin.heading' })}
        </Heading>
        <Text mb={4}>
          {intl.formatMessage({ id: 'localizationAdmin.description' })}
        </Text>
        <FormControl>
          <FormLabel>{intl.formatMessage({ id: 'localizationAdmin.default' })}</FormLabel>
          <LanguageSelect value={selected} onChange={setSelected} />
        </FormControl>
        <Button mt={4} colorScheme="blue" isLoading={busy} onClick={save}>
          {intl.formatMessage({ id: 'localizationAdmin.save' })}
        </Button>
        {notice && <Alert status={notice.status} mt={4}><AlertIcon />{notice.message}</Alert>}
      </Box>
    </Stack>
  );
}
