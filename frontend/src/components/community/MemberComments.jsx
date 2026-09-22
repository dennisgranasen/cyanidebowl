import React, { useEffect, useState } from 'react';
import { Box, Button, Heading, Link, Text } from '@chakra-ui/react';
import { Link as RouteLink, useLocation } from 'react-router-dom';
import { useIntl } from 'react-intl';
import CommunityApi from '../../CommunityApi';

export default function MemberComments({ fanId }) {
  const intl = useIntl();
  const location = useLocation();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    let active = true;
    setLoading(true); setError(null);
    CommunityApi.comments(fanId, page).then(result => {
      if (active) setData(previous => ({ ...result, comments: page === 0 ? result.comments : [...(previous?.comments || []), ...result.comments] }));
    }).catch(e => { if (active) setError(e); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [fanId, page]);
  return <Box mt={8} maxW="900px">
    <Heading size="md">{intl.formatMessage({ id: 'community.commentHistory' })}{data ? ` (${data.total})` : ''}</Heading>
    {error && <Text color="red.400">{intl.formatMessage({ id: 'community.commentsError' })}</Text>}
    {data?.comments.map(comment => <Box key={comment.id} mt={3} p={3} borderWidth="1px" borderRadius="md">
      <Link as={RouteLink} to={comment.url.replace('#', `${location.search}#`)} fontWeight="bold">{comment.title}</Link>
      {comment.createdAt && <Text fontSize="xs">{intl.formatDate(comment.createdAt, { dateStyle: 'medium', timeStyle: 'short' })}</Text>}
      <Text mt={2} whiteSpace="pre-wrap">{comment.body}</Text>
    </Box>)}
    {data?.total === 0 && <Text mt={3}>{intl.formatMessage({ id: 'community.noComments' })}</Text>}
    {loading && <Text mt={3}>{intl.formatMessage({ id: 'common.loading' })}</Text>}
    {data?.hasMore && !error && <Button mt={3} isDisabled={loading} onClick={() => setPage(p => p + 1)}>{intl.formatMessage({ id: 'community.moreComments' })}</Button>}
  </Box>;
}
