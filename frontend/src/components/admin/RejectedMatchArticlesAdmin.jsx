import React, { useEffect, useState } from 'react';
import { Badge, Box, Button, Heading, HStack, Text, VStack } from '@chakra-ui/react';
import EditorialCommunityApi from '../../EditorialCommunityApi';

export default function RejectedMatchArticlesAdmin({ getAccessTokenSilently, onError }) {
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setArticles(await EditorialCommunityApi.rejectedMatchArticles(getAccessTokenSilently));
    } catch (e) {
      onError?.(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const remove = async (article) => {
    if (!window.confirm(`Radera "${article.title}" permanent från databasen?`)) return;
    try {
      await EditorialCommunityApi.deleteRejectedMatchArticle(article.id, getAccessTokenSilently);
      setArticles(current => current.filter(item => item.id !== article.id));
    } catch (e) {
      onError?.(e);
    }
  };

  return (
    <Box borderWidth="1px" borderRadius="md" p={4}>
      <HStack justify="space-between" mb={3}>
        <Box>
          <Heading size="sm">Refuserade matchartiklar</Heading>
          <Text fontSize="sm" color="gray.500">
            Dolda från redaktörsvyn. Siteadmin kan radera dem permanent ur databasen.
          </Text>
        </Box>
        <Button size="sm" variant="outline" onClick={load} isLoading={loading}>Uppdatera</Button>
      </HStack>
      <VStack align="stretch" spacing={2}>
        {articles.map(article => (
          <Box key={article.id} borderWidth="1px" borderRadius="md" p={3}>
            <HStack justify="space-between" align="start">
              <Box>
                <HStack>
                  <Text fontWeight="bold">{article.title}</Text>
                  <Badge colorScheme="red">REJECTED</Badge>
                  {article.authorType === 'AI' && <Badge colorScheme="cyan">{article.reporterAlias || 'AI'}</Badge>}
                </HStack>
                <Text fontSize="sm" color="gray.500">
                  {article.authorDisplayName || article.authorSubject} · match {article.matchId}
                </Text>
              </Box>
              <Button size="sm" colorScheme="red" variant="outline" onClick={() => remove(article)}>
                Radera permanent
              </Button>
            </HStack>
          </Box>
        ))}
        {!loading && articles.length === 0 && <Text color="gray.500">Inga refuserade matchartiklar.</Text>}
      </VStack>
    </Box>
  );
}
