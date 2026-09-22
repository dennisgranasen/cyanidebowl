import React, { useEffect, useRef, useState } from 'react';
import { Box, Button, FormControl, FormLabel, Heading, HStack, Image, Select, SimpleGrid, Text, Textarea, VStack } from '@chakra-ui/react';
import Api from '../../WarpScoresApiService';
import CommunityApi from '../../CommunityApi';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';

const types = [
  { target: 'PROFILE_IMAGE', label: 'Profile image', url: 'profileImageUrl', prompt: 'profileImagePrompt' },
  { target: 'AVATAR', label: 'Avatar', url: 'avatarImageUrl', prompt: 'avatarPrompt' },
];
const active = job => ['QUEUED', 'RUNNING'].includes(job.status);

export default function CommunityProfileMediaAdmin({ profileId, onProfileUpdated }) {
  const { getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];
  const [profile, setProfile] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [providers, setProviders] = useState([]);
  const [provider, setProvider] = useState('');
  const [target, setTarget] = useState('PROFILE_IMAGE');
  const [prompt, setPrompt] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [refresh, setRefresh] = useState(0);
  const callback = useRef(onProfileUpdated);
  callback.current = onProfileUpdated;
  const initialized = useRef(false);
  useEffect(() => {
    let mounted = true, timer;
    const load = async () => {
      try {
        const [fan, media, options] = await Promise.all([
          Api.adminCommunityFan(profileId, ...auth), Api.adminCommunityFanMedia(profileId, ...auth),
          Api.communityImageProviders(...auth),
        ]);
        if (!mounted) return;
        setProfile(fan); setJobs(media); setProviders(options);
        setProvider(current => current || options.find(p => p.configured)?.id || '');
        if (!initialized.current) { setPrompt(fan.profileImagePrompt || ''); initialized.current = true; }
        callback.current?.(fan);
        setError('');
        if (media.some(active)) timer = setTimeout(load, 5000);
      } catch (e) { if (mounted) setError(e.message || String(e)); }
    };
    load();
    return () => { mounted = false; clearTimeout(timer); };
  }, [profileId, refresh, getAccessTokenSilently, getAccessTokenWithPopup]);

  const run = async action => {
    if (busy) return;
    setBusy(true); setError(''); setMessage('');
    try { await action(); setRefresh(n => n + 1); }
    catch (e) { setError(e.response?.data?.message || e.message || String(e)); }
    finally { setBusy(false); }
  };
  const regenerate = type => run(async () => {
    setJobs(await Api.regenerateAdminCommunityFanMedia(profileId, ...auth, type.target));
    setMessage(`${type.label} queued. The other image will be kept.`);
  });
  const preview = () => run(async () => {
    const job = await Api.previewCommunityFanMedia(profileId, { target, provider, prompt }, ...auth);
    setJobs(current => [job, ...current]);
    setMessage('Preview queued. Your current image stays in place until you approve.');
  });
  const review = (job, approve) => run(async () => {
    const fan = await Api.reviewCommunityFanMedia(profileId, job.id, approve, ...auth);
    setProfile(fan); callback.current?.(fan);
    setMessage(approve ? 'Image approved.' : 'Preview discarded. Your current image was kept.');
  });

  return <Box borderWidth="1px" borderRadius="md" p={4}>
    <HStack justify="space-between"><Heading size="sm">Profile images</Heading>
      <Button size="xs" isDisabled={busy} onClick={() => setRefresh(n => n + 1)}>Refresh</Button></HStack>
    {error && <Text role="alert" color="red.300">{error}</Text>}
    {message && <Text role="status" mt={2}>{message}</Text>}
    {!profile ? <Text>Loading images...</Text> : <>
      <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4} mt={3}>
        {types.map(type => {
          const pending = jobs.some(job => job.target === type.target && active(job));
          const latest = jobs.find(job => job.target === type.target);
          return <Box key={type.target}>
            <Text fontWeight="bold">{type.label}</Text>
            {profile[type.url]?.trim() ? <Image src={CommunityApi.assetUrl(profile[type.url])} alt={type.label} maxH="180px" /> : <Text>No image</Text>}
            {latest && <Text fontSize="sm">Latest job: {latest.status}{latest.error ? ` · ${latest.error}` : ''}</Text>}
            <Button mt={2} size="sm" isDisabled={busy || pending || !profile[type.prompt]?.trim()} onClick={() => regenerate(type)}>
              {pending ? 'Already queued / running' : `Generate ${type.label.toLowerCase()}`}
            </Button>
            {!profile[type.prompt]?.trim() && <Text fontSize="xs">Save an image prompt to enable generation.</Text>}
          </Box>;
        })}
      </SimpleGrid>
      <Text mt={2} fontSize="sm">These buttons replace only the selected image when generation succeeds. Use a preview below to review it first.</Text>
      <Heading size="sm" mt={5}>Try a new image</Heading>
      <FormControl mt={3}><FormLabel>Image type</FormLabel><Select value={target} isDisabled={busy} onChange={e => {
        setTarget(e.target.value); setPrompt(profile[types.find(t => t.target === e.target.value).prompt] || '');
      }}>{types.map(t => <option key={t.target} value={t.target}>{t.label}</option>)}</Select></FormControl>
      <FormControl mt={3}><FormLabel>Image service</FormLabel><Select value={provider} isDisabled={busy} onChange={e => setProvider(e.target.value)}>
        {!provider && <option value="">Select a configured service</option>}
        {providers.map(p => <option key={p.id} value={p.id} disabled={!p.configured}>{p.name}{p.configured ? '' : ' (not configured)'}</option>)}
      </Select></FormControl>
      <FormControl mt={3}><FormLabel>Prompt for this preview</FormLabel><Textarea value={prompt} onChange={e => setPrompt(e.target.value)} isDisabled={busy} /></FormControl>
      <Button mt={3} onClick={preview} isLoading={busy} isDisabled={!provider || !prompt.trim() || jobs.some(j => j.target === target && active(j))}>Queue preview</Button>
      <Text fontSize="sm" mt={2}>The current images stay unchanged until you approve a preview.</Text>
      <VStack align="stretch" mt={4} spacing={4}>
        {jobs.filter(j => j.status === 'AWAITING_APPROVAL').map(job => <Box key={job.id} borderWidth="1px" p={3} borderRadius="md">
          <Text>{types.find(t => t.target === job.target)?.label} · {job.provider || job.requestedProvider} · {job.model}</Text>
          <Image mt={2} src={CommunityApi.assetUrl(job.assetUrl)} alt="Generated preview awaiting approval" maxH="400px" />
          <HStack mt={3}><Button colorScheme="green" isDisabled={busy} onClick={() => review(job, true)}>Approve</Button>
            <Button variant="outline" isDisabled={busy} onClick={() => review(job, false)}>Discard</Button></HStack>
        </Box>)}
      </VStack>
    </>}
  </Box>;
}
