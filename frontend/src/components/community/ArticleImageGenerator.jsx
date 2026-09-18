import React, { useEffect, useRef, useState } from 'react';
import { Box, Button, FormControl, FormLabel, HStack, Select, Text, Textarea } from '@chakra-ui/react';
import { RepeatIcon } from '@chakra-ui/icons';
import { useIntl } from 'react-intl';
import ArticleSubjectPicker from './ArticleSubjectPicker';
import Api from '../../EditorialCommunityApi';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import { canGenerateArticleImage } from '../../util/articleImages';

export default function ArticleImageGenerator({ editor, title, associations = [], matchId, reporterId,
  disabled, contextAvailable = true, onError, onBusyChange }) {
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const { getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [imageLinks, setImageLinks] = useState(null);
  const selectedLinks = imageLinks ?? associations;
  const [prompt, setPrompt] = useState('');
  const [pending, setPending] = useState(false);
  const [generated, setGenerated] = useState(false);
  const [referenceNotice, setReferenceNotice] = useState(false);
  const [photographers, setPhotographers] = useState([]);
  const [photographerId, setPhotographerId] = useState('');
  const [loadError, setLoadError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [queue, setQueue] = useState(null);
  const [queueError, setQueueError] = useState(false);
  const [providerStatus, setProviderStatus] = useState(null);
  const [providerStatusError, setProviderStatusError] = useState(false);
  const [referenceRejected, setReferenceRejected] = useState(false);
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
  const subjectKey = JSON.stringify(selectedLinks.map(({ type, id }) => ({ type, id })));
  useEffect(() => {
    setReferenceRejected(false);
  }, [subjectKey, prompt]);

  useEffect(() => {
    let active = true;
    let timer;
    const refresh = async () => {
      try {
        const status = await Api.imageStatus(JSON.parse(subjectKey), getAccessTokenSilently);
        if (active) { setProviderStatus(status); setProviderStatusError(false); }
      } catch (_) {
        if (active) setProviderStatusError(true);
      } finally {
        if (active) timer = setTimeout(refresh, 15000);
      }
    };
    refresh();
    return () => { active = false; clearTimeout(timer); };
  }, [subjectKey, getAccessTokenSilently]);

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
  const providerBlocked = providerStatus?.blocked === true;
  const available = !!photographer && contextAvailable && !providerBlocked
    && (selectedLinks.length > 0 || canGenerateArticleImage({ title, body: editor?.getText(), prompt, matchId }));
  const generate = async (ignoreReferences = false) => {
    if (inFlight.current || disabled || !editor || !available) return;
    inFlight.current = true; setPending(true); onBusyChange?.(true);
    try {
      const image = await Api.generateArticleImage({ title, body: editor.getText(), prompt,
        associations: selectedLinks.map(({ type, id }) => ({ type, id })),
        matchId, reporterId, photographerId, ignoreReferences }, getAccessTokenSilently);
      if (!editor.isDestroyed) editor.chain().focus().setImage({ src: Api.assetUrl(image.url), editorialImageId: image.imageId,
        title: intl.formatMessage({ id: 'news.imageCredit' }, { name: image.photographerName || photographer.alias }) }).run();
      setGenerated(true);
      setReferenceRejected(false);
      setReferenceNotice(image.referenceImagesAvailable === 'true' && image.referenceImagesUsed !== 'true');
    } catch (error) {
      const providerCode = error.response?.data?.code;
      if (providerCode === 'IMAGE_SAFETY_REJECTED') {
        setReferenceRejected(true);
        setReferenceNotice(false);
      } else {
        const timedOut = ['IMAGE_TIMEOUT', 'ECONNABORTED', 'ETIMEDOUT'].includes(error.code) || error.response?.status === 504;
        onError(timedOut ? new Error(t('news.imageTimeout')) : error);
      }
    }
    finally { inFlight.current = false; setPending(false); onBusyChange?.(false); }
  };
  return <Box borderWidth="1px" borderRadius="md" p={3}>
    <Text fontSize="sm" mb={3}>{t('news.imageWorldContext')}</Text>
    <Box mb={3}>
      <Text fontWeight="semibold">{t('news.imageSubjects')}</Text>
      <Text fontSize="sm" mb={2}>{t('news.imageSubjectsHelp')}</Text>
      <ArticleSubjectPicker value={selectedLinks} onChange={setImageLinks} disabled={pending || disabled} onError={onError} />
    </Box>
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
    {referenceNotice && <Text mt={2} fontSize="sm">{t('news.imageTextReferencesOnly')}</Text>}
    {referenceRejected && <Box mt={2} p={2} borderWidth="1px" borderRadius="md">
      <Text color="orange.400" fontWeight="semibold">
        Cloudflare rejected this prompt/reference-image combination.
      </Text>
      <Text fontSize="sm">
        You can change the prompt or retry without the portrait references. Tagged subjects will still be included as text context.
      </Text>
      <Button size="sm" mt={2} variant="outline" isLoading={pending}
        isDisabled={disabled || !available || !editor}
        onClick={() => generate(true)}>
        Generate without reference images
      </Button>
    </Box>}
    {providerStatus?.blocked && <Box mt={2} p={2} borderWidth="1px" borderRadius="md">
      <Text color="orange.400" fontWeight="semibold">
        {providerStatus.quotaExhausted ? 'Image quota/credits are exhausted.' : 'Image generation is temporarily unavailable.'}
      </Text>
      <Text fontSize="sm">{providerStatus.message}</Text>
      {providerStatus.retryAt && <Text fontSize="sm">
        Retry after {intl.formatDate(providerStatus.retryAt, { dateStyle: 'medium', timeStyle: 'short' })}
      </Text>}
    </Box>}
    {providerStatusError && <Text mt={2} fontSize="sm">Image provider status could not be checked.</Text>}
    <HStack mt={2} flexWrap="wrap">
      <Button size="sm" leftIcon={<RepeatIcon />} isLoading={pending} isDisabled={disabled || !available || !editor} onClick={() => generate(false)}>
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
