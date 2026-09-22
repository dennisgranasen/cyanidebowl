import React, { useEffect, useState } from 'react';
import {
  Avatar, Badge, Box, Heading, HStack, Spinner, Text
} from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import CommunityApi from '../../CommunityApi';

function TeamSupporters({ teamId, dedicatedFans }) {
  const [fans, setFans] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!teamId) return;
    CommunityApi.fans(teamId).then(setFans).catch(setError);
  }, [teamId]);

  return (
    <Box mt={6}>
      <HStack mb={3}>
        <Heading size="md">Supporters</Heading>
        {fans && (
          <Badge colorScheme="purple">
            {fans.length} community / {dedicatedFans ?? '?'} Dedicated Fans
          </Badge>
        )}
      </HStack>

      {!fans && !error && <Spinner size="sm" />}
      {error && <Text color="red.300" fontSize="sm">{error.message || String(error)}</Text>}
      {fans && fans.length === 0 && (
        <Text color="gray.500" fontSize="sm">
          No generated community supporters for this team yet.
        </Text>
      )}

      <HStack align="stretch" spacing={3} overflowX="auto" pb={2}>
        {(fans || []).map((fan) => (
          <Box
            key={fan.id}
            as={RouteLink}
            to={`/community/${encodeURIComponent(fan.id)}`}
            borderWidth="1px"
            borderRadius="md"
            p={3}
            minW="180px"
            maxW="220px"
            textAlign="center"
            _hover={{ textDecoration: 'none', transform: 'translateY(-2px)' }}
            transition="120ms ease"
          >
            <Avatar
              size="lg"
              name={fan.displayName}
              src={CommunityApi.assetUrl(
                fan.avatarImageUrl || fan.profileImageUrl
              )}
            />
            <Text mt={2} fontWeight="bold" noOfLines={1}>
              {fan.displayName}
            </Text>
            <Text color="gray.500" fontSize="xs" noOfLines={1}>
              {fan.species}
            </Text>
            {fan.supporterArchetype && (
              <Badge mt={2} colorScheme="purple">
                {fan.supporterArchetype}
              </Badge>
            )}
          </Box>
        ))}
      </HStack>
    </Box>
  );
}

export default TeamSupporters;
