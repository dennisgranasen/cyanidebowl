import React, { useEffect, useState } from 'react';
import { Box, Heading, Link, Text } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import { useIntl } from 'react-intl';
import Navigation from '../components/misc/Navigation';
import CommentThread from '../components/community/CommentThread';
import CommunityApi from '../CommunityApi';

export default function CommunityDiscussionPage() {
  const { type, targetId } = useParams();
  const intl = useIntl();
  const [discussion, setDiscussion] = useState(null);
  const [error, setError] = useState(null);
  useEffect(() => {
    let active = true;
    setDiscussion(null); setError(null);
    CommunityApi.discussion(type, targetId).then(d => { if (active) setDiscussion(d); }).catch(e => { if (active) setError(e); });
    return () => { active = false; };
  }, [type, targetId]);
  return <Box p={6}>
    <Navigation currentPage="community" />
    {error && <Text mt={4}>{intl.formatMessage({ id: 'community.discussionUnavailable' })}</Text>}
    {discussion && <Box mt={6} maxW="900px">
      <Heading mb={4}>{discussion.title}</Heading>
      {discussion.sourceUrl && <Link as={RouteLink} to={discussion.sourceUrl}>{intl.formatMessage({ id: 'community.openSource' })}</Link>}
      <Box mt={4}><CommentThread targetType={type} targetId={targetId} /></Box>
    </Box>}
  </Box>;
}
