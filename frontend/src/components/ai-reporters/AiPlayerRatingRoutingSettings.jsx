import React, { useEffect, useState } from 'react';
import { Alert, Box, Heading, Select, Spinner, Text } from '@chakra-ui/react';
import AiReporterApi from '../../AiReporterApi';

export default function AiPlayerRatingRoutingSettings({
  getAccessTokenSilently,
  getAccessTokenWithPopup,
}) {
  const [routing, setRouting] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    AiReporterApi.playerRatingRouting(getAccessTokenSilently, getAccessTokenWithPopup)
      .then((value) => { if (active) setRouting(value); })
      .catch((reason) => {
        if (active) setError(reason?.response?.data?.message || reason?.message || String(reason));
      });
    return () => { active = false; };
  }, [getAccessTokenSilently, getAccessTokenWithPopup]);

  const save = async (field, value) => {
    setSaving(true);
    setError('');
    try {
      setRouting(await AiReporterApi.updatePlayerRatingRouting({
        editorialTargetId: field === 'editorialTargetId' ? value : routing.editorialTargetId,
        fanTargetId: field === 'fanTargetId' ? value : routing.fanTargetId,
      }, getAccessTokenSilently, getAccessTokenWithPopup));
    } catch (reason) {
      setError(reason?.response?.data?.message || reason?.message || String(reason));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box mt={5} maxW="520px">
      <Heading size="md" mb={3}>Player rating providers</Heading>
      {error && <Alert status="error" mb={3}>{error}</Alert>}
      {!routing ? <Spinner /> : <>
        <Text mb={1} fontSize="sm" fontWeight="700">Default provider for editorial player ratings</Text>
        <Select size="sm" value={routing.editorialTargetId || ''} isDisabled={saving}
          onChange={(event) => save('editorialTargetId', event.target.value || null)}>
          <option value="">Use task routing policy</option>
          {routing.editorialTargets.map((target) => (
            <option key={target.targetId} value={target.targetId}>
              {target.targetId} · {target.provider} / {target.model}
            </option>
          ))}
        </Select>
        <Text mt={1} fontSize="xs" color="gray.500">
          Applies to newly queued editorial ratings. Existing queued ratings can be moved in the work queue.
        </Text>
        <Text mt={4} mb={1} fontSize="sm" fontWeight="700">Default provider for fan player ratings</Text>
        <Select size="sm" value={routing.fanTargetId || ''} isDisabled={saving}
          onChange={(event) => save('fanTargetId', event.target.value || null)}>
          <option value="">Use task routing policy</option>
          {routing.fanTargets.map((target) => (
            <option key={target.targetId} value={target.targetId}>
              {target.targetId} · {target.provider} / {target.model}
            </option>
          ))}
        </Select>
        <Text mt={1} fontSize="xs" color="gray.500">
          Applies to newly queued fan ratings. Running calls stay on their current provider.
        </Text>
      </>}
    </Box>
  );
}