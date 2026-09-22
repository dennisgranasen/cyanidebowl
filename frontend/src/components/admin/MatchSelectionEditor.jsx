import React, { useEffect, useState } from 'react';
import { Alert, Box, Checkbox, Heading, Table, TableContainer, Tbody, Td, Text, Th, Thead, Tr } from '@chakra-ui/react';
import WarpScoresApiService from '../../WarpScoresApiService';
import { isMatchSelected, toggleMatch, matchDuration } from './matchSelection';

const number = (value) => (value === '' || value == null ? null : Number(value));

export default function MatchSelectionEditor({ source, onChange, getAccessTokenSilently, getAccessTokenWithPopup }) {
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState(null);
  const draft = JSON.stringify({ firstId: source.firstId, lastId: source.lastId,
    firstIndex: number(source.firstIndex), lastIndex: number(source.lastIndex), isArchived: source.isArchived });
  useEffect(() => {
    let cancelled = false;
    setPreview(null);
    setError(null);
    const timer = setTimeout(() => {
      WarpScoresApiService.previewMatchSelection(source.id, JSON.parse(draft), getAccessTokenSilently, getAccessTokenWithPopup)
        .then((result) => { if (!cancelled) setPreview(result); })
        .catch((reason) => { if (!cancelled) setError(reason?.message || 'Could not load matches'); });
    }, 250);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [source.id, draft, getAccessTokenSilently, getAccessTokenWithPopup]);

  return <Box>
    <Heading size="sm" mb={2}>Matches in selected source</Heading>
    <Text mb={2}>Checked matches are included in this selection. Changes remain a draft until reviewed and confirmed.</Text>
    <Text fontSize="sm" color="gray.500">Dates and start times use your local time zone. Unknown duration means the source has no usable start/end times. Separate match rulings may still exclude a match from standings.</Text>
    {error && <Alert status="error">{error}</Alert>}
    {!error && !preview && <Text>Loading matches…</Text>}
    {preview && <>
      {preview.warnings.map((warning) => <Alert status="warning" key={warning} my={2}>{warning}</Alert>)}
      <Text my={2}>{preview.matches.filter((match) => isMatchSelected(match, source)).length} of {preview.matches.length} matches selected</Text>
      {preview.matches.length === 0 ? <Text>No registered matches found.</Text> :
        <TableContainer maxH="32rem" overflowY="auto"><Table size="sm">
          <Thead><Tr>{['Include', 'Date', 'Start', 'Duration', 'Teams', 'Result', 'Match ID'].map((label) => <Th key={label}>{label}</Th>)}</Tr></Thead>
          <Tbody>{preview.matches.map((match) => {
            const date = match.startedAt ? new Date(match.startedAt) : null;
            const teams = match.teams.map((team) => team.name || 'Unknown team').join(' – ');
            return <Tr key={match.key}>
              <Td><Checkbox aria-label={`Include ${teams} (${match.key})`} isChecked={isMatchSelected(match, source)}
                onChange={(event) => onChange(toggleMatch(source, match.key, event.target.checked))} /></Td>
              <Td>{date ? date.toLocaleDateString() : 'Unknown'}</Td>
              <Td>{date ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Unknown'}</Td>
              <Td>{matchDuration(match.startedAt, match.finishedAt)}</Td><Td>{teams || 'Unknown teams'}</Td>
              <Td>{match.teams.map((team) => team.score ?? '–').join(' – ')}</Td><Td>{match.key}</Td>
            </Tr>;
          })}</Tbody>
        </Table></TableContainer>}
    </>}
  </Box>;
}
