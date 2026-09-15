import React, { useEffect, useRef, useState } from 'react';
import { Box, Button, FormControl, FormLabel, HStack, Select, Text, Textarea } from '@chakra-ui/react';
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
  const [photographers, setPhotographers] = useState([]);
  const [photographerId, setPhotographerId] = useState('');
  const [loadError, setLoadError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [queue, setQueue] = useState(null);
  const [queueError, setQueueError] = useState(false);
  useEffect(() => {
    if (!photographerId) return undefined;
    let active = true;
    let timer;
    setQueue(null);
    const refresh = async () => {
      try {
        const status = await Api.imageQueue(photographerId, getAccessTokenSilently);
        if (active) { setQueue(status); setQueueError(false); }
      } catch (_) { if (active) setQueueError(true); }
      finally { if (active) timer = setTimeout(refresh, 15000); }
    };
    refresh();
    return () => { active = false; clearTimeout(timer); };
  }, [photographerId, getAccessTokenSilently]);
  useEffect(() => {
    let active = true;
    setLoadError(false);
    Api.photographers().then(people => {
      if (!active) return;
      setPhotographers(people);
      setPhotographerId(id => people.some(p => p.id === id) ? id : people[0]?.id || '');
    }).catch(() => { if (active) setLoadError(true); });
    return () => { active = false; };
  }, [attempt]);
  const photographer = photographers.find(p => p.id === photographerId);
  const description = photographer?.descriptions?.[intl.locale.split('-')[0]] || photographer?.descriptions?.en;
  const inFlight = useRef(false);
  const available = !!photographer && contextAvailable && canGenerateArticleImage({ title, body: editor?.getText(), prompt, matchId });
  const generate = async () => {
    if (inFlight.current || disabled || !editor || !available) return;
    inFlight.current = true; setPending(true); onBusyChange?.(true);
    try {
      const image = await Api.generateArticleImage({ title, body: editor.getText(), prompt,
        associations, matchId, reporterId, photographerId }, getAccessTokenSilently);
      if (!editor.isDestroyed) editor.chain().focus().setImage({ src: Api.assetUrl(image.url),
        title: intl.formatMessage({ id: 'news.imageCredit' }, { name: image.photographerName || photographer.alias }) }).run();
      setGenerated(true);
    } catch (error) {
      const timedOut = ['IMAGE_TIMEOUT', 'ECONNABORTED', 'ETIMEDOUT'].includes(error.code) || error.response?.status === 504;
      onError(timedOut ? new Error(t('news.imageTimeout')) : error);
    }
    finally { inFlight.current = false; setPending(false); onBusyChange?.(false); }
  };
  return <Box borderWidth="1px" borderRadius="md" p={3}>
    <FormControl mb={3}>
      <FormLabel fontSize="sm">{t('news.photographer')}</FormLabel>
      <Select value={photographerId} onChange={e => setPhotographerId(e.target.value)} isDisabled={pending || disabled || !photographers.length}>
        {!photographers.length && <option value="">{t('staff.loading')}</option>}
        {photographers.map(p => <option key={p.id} value={p.id}>{p.alias}</option>)}
      </Select>
      {description && <Box mt={2} fontSize="sm">
        <Text>{description.personality}</Text>
        <Text mt={1}>{description.focus}</Text>
        <Text mt={1}><strong>{t('staff.medium')}: </strong>{description.medium}</Text>
        <Text mt={1}><strong>{t('staff.equipment')}: </strong>{description.equipment}</Text>
      </Box>}
      {loadError && <Box mt={2}><Text color="red.400">{t('news.photographersFailed')}</Text>
        <Button size="sm" mt={1} onClick={() => setAttempt(n => n + 1)}>{t('news.retryPhotographers')}</Button></Box>}
    </FormControl>
    <FormControl><FormLabel fontSize="sm">{t('news.imagePrompt')}</FormLabel>
      <Textarea size="sm" rows={2} value={prompt} onChange={e => setPrompt(e.target.value)} placeholder={t('news.imagePromptHelp')} isDisabled={pending} />
    </FormControl>
    <HStack mt={2} flexWrap="wrap">
      <Button size="sm" leftIcon={<RepeatIcon />} isLoading={pending} isDisabled={disabled || !available || !editor} onClick={generate}>
        {t(generated ? 'news.generateAnotherImage' : 'news.generateImage')}
      </Button>
      <Text fontSize="xs" opacity={0.75}>{t(matchId ? 'news.matchImageContext' : 'news.imageRegenerateHelp')}</Text>
    </HStack>
    <Box mt={3} fontSize="sm" role="status" aria-live="polite">
      <Text fontWeight="semibold">{t('news.developmentTime')}</Text>
      {queue && <>
        <Text>{intl.formatMessage({ id: 'news.imageQueueCounts' }, { ahead: queue.ahead, running: queue.running })}</Text>
        {queue.notBefore && new Date(queue.notBefore).getTime() > Date.now() && <Text color="orange.400">
          {intl.formatMessage({ id: 'news.imageQueuePaused' }, { time: intl.formatDate(queue.notBefore, { dateStyle: 'medium', timeStyle: 'short' }) })}
        </Text>}
        <Text>{queue.estimatedWaitSeconds == null ? t('news.imageQueueUnknown') :
          intl.formatMessage({ id: 'news.imageQueueEstimate' }, { minutes: Math.ceil(queue.estimatedWaitSeconds / 60) })}</Text>
      </>}
      {queueError && <Text>{t('news.imageQueueUnavailable')}</Text>}
      <Text mt={1} opacity={0.75}>{t('news.imageQueueExplanation')}</Text>
    </Box>
    {!contextAvailable && <Text mt={2} fontSize="sm">{t('news.imageNeedsReplay')}</Text>}
  </Box>;
}
