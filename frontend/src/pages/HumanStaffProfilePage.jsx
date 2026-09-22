import React, { useEffect, useState } from 'react';
import { Avatar, Box, Card, CardBody, Container, Heading, Image, Spinner, Stack, Text } from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import { useParams } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import StaffApi from '../StaffApi';
export default function HumanStaffProfilePage() {
  const intl=useIntl(); const { profileId }=useParams(); const [profile,setProfile]=useState(null); const [error,setError]=useState(null);
  useEffect(()=>{setProfile(null);setError(null);StaffApi.user(profileId).then(setProfile).catch(setError);},[profileId]);
  return <Container maxW="5xl" px={{base:3,md:6}} py={{base:3,md:5}}>
    <Navigation currentPage="staffProfile" parentPage="staff" currentLabel={profile?.displayName}/>{error&&<Text mt={6} color="red.400">{intl.formatMessage({id:'staff.noHumanProfile'})}</Text>}{!profile&&!error&&<Spinner mt={8}/>}
    {profile&&<Card mt={6} overflow="hidden" variant="outline"><Stack direction={{base:'column',md:'row'}} spacing={0}><Box flexShrink={0} w={{base:'100%',md:'300px'}} minH="300px" bg="gray.800">{profile.portraitUrl?<Image src={profile.portraitUrl} alt={profile.displayName} w="100%" h="100%" objectFit="cover"/>:<Box h="100%" display="grid" placeItems="center"><Avatar size="2xl" name={profile.displayName} src={profile.avatarUrl||undefined}/></Box>}</Box><CardBody px={{base:5,md:8}} py={{base:6,md:8}}><Text fontSize="xs" fontWeight="700" textTransform="uppercase" color="purple.500">{intl.formatMessage({id:'staff.human'})}</Text><Heading as="h1" size="xl" mt={2}>{profile.displayName}</Heading>{profile.bio?<Text mt={6} whiteSpace="pre-wrap" lineHeight="1.8">{profile.bio}</Text>:<Text mt={6} color="gray.500">{intl.formatMessage({id:'staff.noBio'})}</Text>}</CardBody></Stack></Card>}
  </Container>;
}
