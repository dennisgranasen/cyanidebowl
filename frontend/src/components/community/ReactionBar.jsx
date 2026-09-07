import React, { useEffect, useState } from 'react';
import { Button, ButtonGroup, Text, HStack } from '@chakra-ui/react';
import { useAuth0 } from '@auth0/auth0-react';
import EditorialCommunityApi from '../../EditorialCommunityApi';

const TYPES = [
  ['POW', '💥', 'pow'],
  ['DOUBLE_POW', '💥×2', 'doublePow'],
  ['TRIPLE_POW', '💥×3', 'triplePow'],
  ['SKULL', '☠', 'skull'],
  ['DOUBLE_SKULL', '☠×2', 'doubleSkull'],
  ['TRIPLE_SKULL', '☠×3', 'tripleSkull'],
];

function ReactionBar({ targetType, targetId }) {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0();
  const [summary, setSummary] = useState(null);

  const load = () => EditorialCommunityApi.reactions(targetType, targetId).then(setSummary);
  useEffect(() => { load(); }, [targetType, targetId]);

  const react = async (type) => {
    if (!isAuthenticated) return;
    await EditorialCommunityApi.react(targetType, targetId, type, getAccessTokenSilently);
    await load();
  };

  if (!summary) return null;
  return (
    <HStack wrap="wrap" spacing={2}>
      <ButtonGroup size="xs" isAttached={false}>
        {TYPES.map(([type, label, key]) => (
          <Button key={type} variant={summary.mine === type ? 'solid' : 'outline'}
            onClick={() => react(type)} isDisabled={!isAuthenticated}>
            {label} {summary[key]}
          </Button>
        ))}
      </ButtonGroup>
      <Text fontSize="sm">net {summary.score > 0 ? '+' : ''}{summary.score}</Text>
    </HStack>
  );
}
export default ReactionBar;
