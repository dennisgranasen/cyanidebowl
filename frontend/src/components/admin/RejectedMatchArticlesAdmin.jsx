import React, { useEffect, useState } from 'react';
import { Badge, Box, Button, Heading, HStack, Text, VStack } from '@chakra-ui/react';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import { useIntl } from 'react-intl';

export default function RejectedMatchArticlesAdmin({ getAccessTokenSilently, onError }) {
  const intl = useIntl();
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
    if (!window.confirm(intl.formatMessage({ id: 'rejected.confirmDelete' }, { title: article.title }))) return;
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
          <Heading size="sm">{intl.formatMessage({ id: 'rejected.heading' })}</Heading>
          <Text fontSize="sm" color="gray.500">
            {intl.formatMessage({ id: 'rejected.help' })}
          </Text>
        </Box>
        <Button size="sm" variant="outline" onClick={load} isLoading={loading}>{intl.formatMessage({ id: 'rejected.refresh' })}</Button>
      </HStack>
      <VStack align="stretch" spacing={2}>
        {articles.map(article => (
          <Box key={article.id} borderWidth="1px" borderRadius="md" p={3}>
            <HStack justify="space-between" align="start">
              <Box>
                <HStack>
                  <Text fontWeight="bold">{article.title}</Text>
                  <Badge colorScheme="red">{intl.formatMessage({ id: 'rejected.badge' })}</Badge>
                  {article.authorType === 'AI' && <Badge colorScheme="cyan">{article.reporterAlias || 'AI'}</Badge>}
                </HStack>
                <Text fontSize="sm" color="gray.500">
                  {intl.formatMessage({ id: 'rejected.byMatch' }, { author: article.authorDisplayName || article.authorSubject, matchId: article.matchId })}
                </Text>
              </Box>
              <Button size="sm" colorScheme="red" variant="outline" onClick={() => remove(article)}>
                {intl.formatMessage({ id: 'rejected.delete' })}
              </Button>
            </HStack>
          </Box>
        ))}
        {!loading && articles.length === 0 && <Text color="gray.500">{intl.formatMessage({ id: 'rejected.empty' })}</Text>}
      </VStack>
    </Box>
  );
}
