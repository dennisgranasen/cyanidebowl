import React, { useEffect, useState } from 'react';
import {
  Avatar, Badge, Box, Card, CardBody, Heading, Text
} from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import CommunityApi from '../CommunityApi';

function CommunityPage() {
  const [fans, setFans] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    CommunityApi.fans().then(setFans).catch(setError);
  }, []);

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="community" />
      <Heading mt={6}>Community</Heading>
      <Text mt={2} color="gray.400">
        Supporters and other community members around the Blood Bowl world.
      </Text>

      {error && <Text mt={6} color="red.300">{error.message || String(error)}</Text>}
      {!fans && !error && <Text mt={8}>Loading community...</Text>}

      <Box
        mt={6}
        display="grid"
        gridTemplateColumns="repeat(auto-fill, minmax(min(100%, 210px), 1fr))"
        gap={4}
      >
        {(fans || []).map((fan) => (
          <Card key={fan.id} overflow="hidden">
            <Box
              as={RouteLink}
              to={`/community/${encodeURIComponent(fan.id)}`}
              display="block"
              h="100%"
              _hover={{ textDecoration: 'none' }}
            >
              {fan.profileImageUrl && (
                <Box
                  h="150px"
                  backgroundImage={`url(${CommunityApi.assetUrl(fan.profileImageUrl)})`}
                  backgroundSize="cover"
                  backgroundPosition="center"
                />
              )}
              <CardBody textAlign="center">
                <Avatar
                  mt={fan.profileImageUrl ? -12 : 0}
                  mb={3}
                  size="xl"
                  name={fan.displayName}
                  src={CommunityApi.assetUrl(fan.avatarImageUrl || fan.profileImageUrl)}
                  borderWidth={fan.profileImageUrl ? '4px' : 0}
                  borderColor="gray.700"
                />
                <Heading size="sm">{fan.displayName}</Heading>
                <Box mt={2}>
                  {fan.species && <Badge mr={1}>{fan.species}</Badge>}
                  {fan.supporterArchetype && <Badge colorScheme="purple">{fan.supporterArchetype}</Badge>}
                </Box>
                <Text mt={2} color="gray.400" fontSize="sm">{fan.teamName}</Text>
                {fan.bio && <Text mt={2} color="gray.500" fontSize="xs" noOfLines={3}>{fan.bio}</Text>}
              </CardBody>
            </Box>
          </Card>
        ))}
      </Box>
    </Box>
  );
}

export default CommunityPage;
