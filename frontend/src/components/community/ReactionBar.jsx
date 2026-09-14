import React, { useEffect, useState } from 'react';
import { Button, ButtonGroup, Text, HStack } from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import NuffleDiceGlyph, { ratingGlyphs } from '../NuffleDiceGlyph';

const TYPES = [
  ['TRIPLE_SKULL', -3, 'tripleSkull'],
  ['DOUBLE_SKULL', -2, 'doubleSkull'],
  ['SKULL', -1, 'skull'],
  ['POW', 1, 'pow'],
  ['DOUBLE_POW', 2, 'doublePow'],
  ['TRIPLE_POW', 3, 'triplePow'],
];

function ReactionBar({ targetType, targetId }) {
  const intl = useIntl();
  const { isAuthenticated, getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [summary, setSummary] = useState(null);

  const load = () => EditorialCommunityApi.reactions(targetType, targetId).then(setSummary);
  useEffect(() => { load(); }, [targetType, targetId]);

  const react = async (type) => {
    if (!isAuthenticated) return;
    await EditorialCommunityApi.react(targetType, targetId, type, getAccessTokenSilently);
    await load();
  };

  if (!summary) return null;

  const totalReactions = TYPES.reduce(
    (sum, [, , key]) => sum + Number(summary[key] || 0),
    0,
  );
  const weightedTotal = TYPES.reduce(
    (sum, [, value, key]) => sum + value * Number(summary[key] || 0),
    0,
  );
  const average = totalReactions > 0 ? weightedTotal / totalReactions : null;

  return (
    <HStack wrap="wrap" spacing={3}>
      <ButtonGroup size="sm" isAttached={false}>
        {TYPES.map(([type, value, key]) => (
          <Button
            key={type}
            variant={summary.mine === type ? 'solid' : 'outline'}
            onClick={() => react(type)}
            isDisabled={!isAuthenticated}
            px={2.5}
          >
            <HStack spacing={1.5}>
              <NuffleDiceGlyph
                glyph={ratingGlyphs(value)}
                label={`${value > 0 ? '+' : ''}${value}`}
                fontSize="1.3rem"
              />
              <Text as="span" fontSize="xs">{summary[key]}</Text>
            </HStack>
          </Button>
        ))}
      </ButtonGroup>
      {average != null && (
        <HStack spacing={1.5}>
          <Text fontSize="sm" color="gray.500">
            {intl.formatMessage({ id: 'reactions.average' })}
          </Text>
          <NuffleDiceGlyph
            glyph={ratingGlyphs(Math.max(-3, Math.min(3, Math.round(average))))}
            label={intl.formatMessage({ id: 'reactions.average' })}
            fontSize="1.3rem"
          />
          <Text fontSize="sm" fontWeight="semibold">
            {intl.formatNumber(average, { minimumFractionDigits: 1, maximumFractionDigits: 1 })}
          </Text>
        </HStack>
      )}
    </HStack>
  );
}
export default ReactionBar;
