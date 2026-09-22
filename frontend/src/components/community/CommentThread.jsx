import React, { useEffect, useMemo, useState } from 'react';
import { Badge, Box, Button, HStack, Text, Textarea, VStack } from '@chakra-ui/react';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import ReactionBar from './ReactionBar';
import { useLocation } from 'react-router-dom';

const coach = (c) => c.authorContext === 'HOME_COACH' || c.authorContext === 'AWAY_COACH';

function OneComment({ comment }) {
  if (comment.deletedAt) return <Text opacity={0.6} fontStyle="italic">Kommentar borttagen</Text>;
  return (
    <Box id={`comment-${comment.id}`} borderWidth="1px" borderRadius="md" p={3} w="full" tabIndex={-1}>
      <HStack mb={2}>
        <Text fontWeight="bold">{comment.authorDisplayName}</Text>
        <Badge>{comment.authorContext}</Badge>
      </HStack>
      <Text whiteSpace="pre-wrap">{comment.body}</Text>
      <Box mt={2}><ReactionBar targetType="COMMENT" targetId={comment.id} /></Box>
    </Box>
  );
}

function CommentThread({ targetType, targetId }) {
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
    <VStack align="stretch" spacing={3}>
      {targetType === 'MATCH' && ordered.some(coach) && <Text fontWeight="bold">Coachernas kommentarer</Text>}
      {ordered.map((comment, index) => (
        <React.Fragment key={comment.id}>
          {targetType === 'MATCH' && index > 0 && coach(ordered[index - 1]) && !coach(comment)
            && <Text fontWeight="bold" mt={4}>Läktaren</Text>}
          <OneComment comment={comment} />
        </React.Fragment>
      ))}
      {isAuthenticated && (
        <Box>
          <Textarea value={body} onChange={(e) => setBody(e.target.value)}
            placeholder="Skriv en kommentar…" maxLength={10000} />
          <Button mt={2} onClick={submit}>Publicera kommentar</Button>
        </Box>
      )}
    </VStack>
  );
}
export default CommentThread;
