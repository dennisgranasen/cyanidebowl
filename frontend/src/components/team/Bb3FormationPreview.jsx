import React from 'react';
import { Box, Heading, SimpleGrid, Stack, Text } from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import { formationPlayers } from '../../util/bb3Formation';

function FormationCard({ formation, players, intl }) {
  const positions = formationPlayers(formation.pitchMap, players);
  const onPitch = positions.filter((position) => position.onPitch);
  const sideline = positions.filter((position) => !position.onPitch);
  const name = formation.name || intl.formatMessage({ id: 'team.bb3.newFormation' });

  return (
    <Box borderWidth="1px" borderRadius="md" p={3}>
      <Text fontWeight="semibold" mb={2}>{name}</Text>
      <Box maxW="360px" mx="auto">
        <svg
          viewBox="0 0 13 15"
          width="100%"
          role="img"
          aria-label={`${name} · ${intl.formatMessage({ id: 'team.bb3.onPitch' }, { count: onPitch.length })}`}
        >
          <rect width="13" height="15" fill="#276749" />
          {Array.from({ length: 13 * 15 }, (_, index) => {
            const x = index % 13;
            const y = Math.floor(index / 13);
            return (
              <rect
                key={`${x}-${y}`}
                x={x}
                y={y}
                width="1"
                height="1"
                fill={(x + y) % 2 ? '#2f855a' : '#276749'}
                stroke="#c6f6d5"
                strokeOpacity="0.18"
                strokeWidth="0.035"
              />
            );
          })}
          <path
            d="M1 0V15 M0 4H13 M0 11H13"
            fill="none"
            stroke="#f7fafc"
            strokeOpacity="0.8"
            strokeWidth="0.1"
            strokeDasharray="0.35 0.2"
          />
          <rect x="0" y="0" width="13" height="15" fill="none" stroke="#f7fafc" strokeWidth="0.14" />
          {onPitch.map((position) => (
            <g key={position.playerId}>
              <title>{`${position.name} · ${position.playerId} · ${position.x}, ${position.y}`}</title>
              <circle cx={position.x + 0.5} cy={position.y + 0.5} r="0.38" fill="#3182ce" stroke="#fff" strokeWidth="0.08" />
              <text
                x={position.x + 0.5}
                y={position.y + 0.5}
                textAnchor="middle"
                dominantBaseline="central"
                fill="#fff"
                fontSize="0.3"
                fontWeight="bold"
              >
                {position.label}
              </text>
            </g>
          ))}
        </svg>
      </Box>
      <Text fontSize="sm" color="gray.500" mt={2}>
        {intl.formatMessage({ id: 'team.bb3.onPitch' }, { count: onPitch.length })}
      </Text>
      {sideline.length > 0 && (
        <Text fontSize="sm" color="gray.500">
          {intl.formatMessage({ id: 'team.bb3.sideline' }, {
            players: sideline.map((position) => `${position.name} (${position.x ?? '?'}, ${position.y ?? '?'})`).join(', '),
          })}
        </Text>
      )}
    </Box>
  );
}

export default function Bb3FormationPreview({ formations, players }) {
  const intl = useIntl();
  const groups = [
    { type: 0, title: 'team.bb3.defensive' },
    { type: 1, title: 'team.bb3.offensive' },
  ];

  return (
    <SimpleGrid columns={{ base: 1, xl: 2 }} spacing={4}>
      {groups.map(({ type, title }) => {
        const matching = formations.filter((formation) => Number(formation.formationType) === type);
        return (
          <Box key={type} borderWidth="1px" borderRadius="md" p={4}>
            <Heading size="sm" mb={3}>{intl.formatMessage({ id: title })}</Heading>
            {matching.length > 0 ? (
              <Stack spacing={3}>
                {matching.map((formation, index) => (
                  <FormationCard
                    key={formation.formationId || `${type}-${index}`}
                    formation={formation}
                    players={players}
                    intl={intl}
                  />
                ))}
              </Stack>
            ) : (
              <Text fontSize="sm" color="gray.500">{intl.formatMessage({ id: 'team.bb3.noFormations' })}</Text>
            )}
          </Box>
        );
      })}
    </SimpleGrid>
  );
}
