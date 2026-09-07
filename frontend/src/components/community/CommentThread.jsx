import React, { useEffect, useMemo, useState } from 'react';
import { Badge, Box, Button, HStack, Text, Textarea, VStack } from '@chakra-ui/react';
import { useAuth0 } from '@auth0/auth0-react';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import ReactionBar from './ReactionBar';

const coach = (c) => c.authorContext === 'HOME_COACH' || c.authorContext === 'AWAY_COACH';

function OneComment({ comment }) {
  if (comment.deletedAt) return <Text opacity={0.6} fontStyle="italic">Kommentar borttagen</Text>;
  return (
    <Box borderWidth="1px" borderRadius="md" p={3} w="full">
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
  const { isAuthenticated, getAccessTokenSilently } = useAuth0();
  const [comments, setComments] = useState([]);
  const [body, setBody] = useState('');

  const load = () => EditorialCommunityApi.comments(targetType, targetId).then(setComments);
  useEffect(() => { load(); }, [targetType, targetId]);

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
