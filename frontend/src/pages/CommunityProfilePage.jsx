import React, { useEffect, useState } from 'react';
import {
  Avatar, Badge, Box, Heading, HStack, Image, SimpleGrid, Text, VStack
} from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import CommunityApi from '../CommunityApi';
import MemberComments from '../components/community/MemberComments';
import CommunityProfileMediaAdmin from '../components/community/CommunityProfileMediaAdmin';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

function CommunityProfilePage() {
  const { fanId } = useParams();
  const { userPermissions } = useAuth0WithUserPermissions();
  const [fan, setFan] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    CommunityApi.fan(fanId).then(setFan).catch(setError);
  }, [fanId]);

  if (error) {
    return <Box p={6}><Navigation currentPage="communityProfile" parentPage="community" /><Text color="red.300">{error.message || String(error)}</Text></Box>;
  }
  if (!fan) {
    return <Box p={6}><Navigation currentPage="communityProfile" parentPage="community" /><Text>Loading profile...</Text></Box>;
  }

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="communityProfile" parentPage="community" currentLabel={fan.displayName} />

      {fan.profileImageUrl && (
        <Image
          mt={6}
          src={CommunityApi.assetUrl(fan.profileImageUrl)}
          alt={fan.displayName}
          w="66%"
          maxH="720px"
          objectFit="cover"
          borderRadius="lg"
        />
      )}

      <HStack mt={fan.profileImageUrl ? -10 : 6} align="end" spacing={5}>
        <Avatar
          size="2xl"
          name={fan.displayName}
          src={CommunityApi.assetUrl(fan.avatarImageUrl || fan.profileImageUrl)}
          borderWidth="4px"
          borderColor="gray.700"
        />
        <Box pb={2}>
          <Heading>{fan.displayName}</Heading>
          <HStack mt={2}>
            {fan.species && <Badge>{fan.species}</Badge>}
            {fan.supporterArchetype && <Badge colorScheme="purple">{fan.supporterArchetype}</Badge>}
            {!fan.active && <Badge colorScheme="gray">Inactive</Badge>}
          </HStack>
        </Box>
      </HStack>

      {fan.bio && <Text mt={5} maxW="850px">{fan.bio}</Text>}

      <SimpleGrid mt={7} columns={{ base: 1, md: 2 }} spacing={6} maxW="900px">
        <VStack align="start" spacing={1}>
          <Heading size="sm">Supporter</Heading>
          <Text>Team: {fan.teamName || '-'}</Text>
          <Text>Race: {fan.teamRace || '-'}</Text>
          <Text>From: {fan.location || '-'}</Text>
          <Text>Occupation: {fan.occupation || '-'}</Text>
        </VStack>
        <VStack align="start" spacing={1}>
          <Heading size="sm">Matchday</Heading>
          <Text>Food: {fan.favoriteFood || '-'}</Text>
          <Text>Drink: {fan.favoriteDrink || '-'}</Text>
          <Text>Chant: {fan.favoriteChant || '-'}</Text>
          {fan.previousTeamId && <Text color="gray.400">Previously supported another team.</Text>}
        </VStack>
      </SimpleGrid>
      <MemberComments key={fanId} fanId={fanId} />
      {userPermissions.writeSiteAdmin && <Box mt={6}>
        <CommunityProfileMediaAdmin key={fanId} profileId={fanId} onProfileUpdated={setFan} />
      </Box>}
    </Box>
  );
}

export default CommunityProfilePage;
