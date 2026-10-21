import React, { useEffect, useMemo, useState } from 'react';
import { Avatar, Badge, Box, Button, HStack, Link, Text, Textarea, VStack } from '@chakra-ui/react';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import ReactionBar from './ReactionBar';
import { Link as RouteLink, useLocation } from 'react-router-dom';
import { useIntl } from 'react-intl';
import { commentAuthorProfilePath } from '../../util/communityAuthorProfiles';

const coach = (c) => c.authorContext === 'HOME_COACH' || c.authorContext === 'AWAY_COACH';
const contextLabels = {
  EDITOR: 'Redaktion',
  HOME_COACH: 'Hemmalagets coach',
  AWAY_COACH: 'Bortalagets coach',
  SPECTATOR: 'Läktaren',
  USER: 'Medlem',
};

function OneComment({ comment, match }) {
  const intl = useIntl();
  if (comment.deletedAt) return <Text opacity={0.6} fontStyle="italic">Kommentar borttagen</Text>;
  const createdAt = comment.displayCreatedAt || comment.createdAt;
  const profilePath = commentAuthorProfilePath(comment, match);
  return (
    <Box id={`comment-${comment.id}`} py={3} borderBottomWidth="1px" borderColor="whiteAlpha.200" w="full" tabIndex={-1}>
      <HStack align="start" spacing={3}>
        <Avatar size="sm" name={comment.authorDisplayName || 'User'} src={comment.authorAvatarUrl || undefined} />
        <Box minW={0} flex={1}>
          <HStack mb={1} spacing={2} flexWrap="wrap">
            {profilePath
              ? <Link as={RouteLink} to={profilePath} fontWeight="bold" _hover={{ color: 'teal.300' }}>{comment.authorDisplayName}</Link>
              : <Text fontWeight="bold">{comment.authorDisplayName}</Text>}
            {comment.authorContext && <Badge variant="subtle">{contextLabels[comment.authorContext] || comment.authorContext}</Badge>}
            {createdAt && (
              <Text ml="auto" fontSize="xs" color="gray.500"
                title={intl.formatDate(createdAt, { dateStyle: 'medium', timeStyle: 'short' })}>
                {relativeTime(createdAt, intl)}
              </Text>
            )}
          </HStack>
          <Text whiteSpace="pre-wrap">{comment.body}</Text>
          <Box mt={1}><ReactionBar targetType="COMMENT" targetId={comment.id} /></Box>
        </Box>
      </HStack>
    </Box>
  );
}

function relativeTime(timestamp, intl) {
  const seconds = (new Date(timestamp).getTime() - Date.now()) / 1000;
  if (!Number.isFinite(seconds)) return '';
  const units = [
    ['year', 31536000], ['month', 2592000], ['week', 604800], ['day', 86400],
    ['hour', 3600], ['minute', 60], ['second', 1],
  ];
  const [unit, duration] = units.find(([, size]) => Math.abs(seconds) >= size) || units[units.length - 1];
  return intl.formatRelativeTime(Math.round(seconds / duration), unit, { numeric: 'auto' });
}

function CommentThread({ targetType, targetId, match }) {
  const location = useLocation();
  const { isAuthenticated, getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [comments, setComments] = useState([]);
  const [body, setBody] = useState('');

  const load = () => EditorialCommunityApi.comments(targetType, targetId).then(setComments);
  useEffect(() => { load(); }, [targetType, targetId]);
  useEffect(() => {
    if (!location.hash.startsWith('#comment-')) return;
    const element = document.getElementById(decodeURIComponent(location.hash.slice(1)));
    if (element) { element.scrollIntoView({ block: 'center' }); element.focus({ preventScroll: true }); }
  }, [comments, location.hash]);

  const submit = async () => {
    if (!body.trim()) return;
    await EditorialCommunityApi.comment(targetType, targetId, body, getAccessTokenSilently);
    setBody('');
    await load();
  };

  const ordered = useMemo(() => targetType === 'MATCH'
    ? [...comments].sort((a, b) => Number(coach(b)) - Number(coach(a))
        || new Date(a.createdAt) - new Date(b.createdAt))
    : comments, [comments, targetType]);

  return (
    <VStack align="stretch" spacing={0}>
      {targetType === 'MATCH' && ordered.some(coach) && <Text fontWeight="bold" color="purple.200" pt={3}>Coachernas kommentarer</Text>}
      {ordered.map((comment, index) => (
        <React.Fragment key={comment.id}>
          {targetType === 'MATCH' && index > 0 && coach(ordered[index - 1]) && !coach(comment)
            && <Text fontWeight="bold" color="teal.200" pt={4}>Läktaren</Text>}
          <OneComment comment={comment} match={match} />
        </React.Fragment>
      ))}
      {isAuthenticated && (
        <Box pt={3}>
          <Textarea value={body} onChange={(e) => setBody(e.target.value)}
            placeholder="Skriv en kommentar…" maxLength={10000} />
          <Button mt={2} onClick={submit}>Publicera kommentar</Button>
        </Box>
      )}
    </VStack>
  );
}
export default CommentThread;
