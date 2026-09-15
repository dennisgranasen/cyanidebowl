import React, { useRef, useState } from 'react';
import { Box, Button, FormControl, FormLabel, HStack, Text, Textarea } from '@chakra-ui/react';
import { RepeatIcon } from '@chakra-ui/icons';
import { useIntl } from 'react-intl';
import Api from '../../EditorialCommunityApi';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import { canGenerateArticleImage } from '../../util/articleImages';

export default function ArticleImageGenerator({ editor, title, associations = [], matchId, reporterId,
  disabled, contextAvailable = true, onError, onBusyChange }) {
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const { getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [prompt, setPrompt] = useState('');
  const [pending, setPending] = useState(false);
  const [generated, setGenerated] = useState(false);
  const inFlight = useRef(false);
  const available = contextAvailable && canGenerateArticleImage({ title, body: editor?.getText(), prompt, matchId });
  const generate = async () => {
    if (inFlight.current || disabled || !editor || !available) return;
    inFlight.current = true; setPending(true); onBusyChange?.(true);
    try {
      const image = await Api.generateArticleImage({ title, body: editor.getText(), prompt,
        associations, matchId, reporterId }, getAccessTokenSilently);
      if (!editor.isDestroyed) editor.chain().focus().setImage({ src: Api.assetUrl(image.url) }).run();
      setGenerated(true);
    } catch (error) { onError(error); }
    finally { inFlight.current = false; setPending(false); onBusyChange?.(false); }
  };
  return <Box borderWidth="1px" borderRadius="md" p={3}>
    <FormControl><FormLabel fontSize="sm">{t('news.imagePrompt')}</FormLabel>
      <Textarea size="sm" rows={2} value={prompt} onChange={e => setPrompt(e.target.value)} placeholder={t('news.imagePromptHelp')} isDisabled={pending} />
    </FormControl>
    <HStack mt={2} flexWrap="wrap">
      <Button size="sm" leftIcon={<RepeatIcon />} isLoading={pending} isDisabled={disabled || !available || !editor} onClick={generate}>
        {t(generated ? 'news.generateAnotherImage' : 'news.generateImage')}
      </Button>
      <Text fontSize="xs" opacity={0.75}>{t(matchId ? 'news.matchImageContext' : 'news.imageRegenerateHelp')}</Text>
    </HStack>
    {!contextAvailable && <Text mt={2} fontSize="sm">{t('news.imageNeedsReplay')}</Text>}
  </Box>;
}
