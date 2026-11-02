import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Box, Button, Heading, HStack, Spinner, Text, VStack } from '@chakra-ui/react';
import { Link as RouterLink } from 'react-router-dom';
import { useIntl } from 'react-intl';
import EditorialCommunityApi from '../../EditorialCommunityApi';

export default function EditorialArticleReviewQueue({ userPermissions, getAccessTokenSilently }) {
  const intl = useIntl();
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const isGlobalEditor = Boolean(userPermissions.writeSiteAdmin || userPermissions.globalEditor);
  const leagueSystemId = isGlobalEditor
    ? null
    : userPermissions.editorForLeagueSystems?.[0] || null;
  const hasReviewScope = isGlobalEditor || Boolean(leagueSystemId);

  const load = useCallback(async () => {
    setError('');
    if (!hasReviewScope) {
      setLoading(false);
      setArticles([]);
      return;
    }
    setLoading(true);
    try {
      setArticles(await EditorialCommunityApi.reviewQueue(leagueSystemId, getAccessTokenSilently));
    } catch (reason) {
      setError(reason?.response?.data?.message || reason?.message || intl.formatMessage({ id: 'editorialReview.error' }));
    } finally {
      setLoading(false);
    }
  }, [getAccessTokenSilently, hasReviewScope, intl, leagueSystemId]);

  useEffect(() => { load(); }, [load]);

  return (
    <Box mt={5} borderWidth="1px" borderRadius="md" p={4}>
      <HStack justify="space-between" mb={3}>
        <Box>
          <Heading size="md">{intl.formatMessage({ id: 'editorialReview.heading' })}</Heading>
          <Text mt={1} fontSize="sm" color="gray.500">
            {intl.formatMessage({ id: 'editorialReview.help' })}
          </Text>
        </Box>
        <Button size="sm" variant="outline" onClick={load} isLoading={loading}>
          {intl.formatMessage({ id: 'editorialReview.refresh' })}
        </Button>
      </HStack>

      {error && <Alert status="error" mb={3}>{error}</Alert>}
      {!hasReviewScope && <Text color="gray.500">{intl.formatMessage({ id: 'editorialReview.noScope' })}</Text>}
      {loading && <HStack><Spinner size="sm" /><Text>{intl.formatMessage({ id: 'editorialReview.loading' })}</Text></HStack>}
      {!loading && hasReviewScope && !error && articles.length === 0 && (
        <Text color="gray.500">{intl.formatMessage({ id: 'editorialReview.empty' })}</Text>
      )}
      {!loading && articles.length > 0 && (
        <VStack align="stretch" spacing={2}>
          {articles.map((article) => (
            <HStack key={article.id} justify="space-between" borderWidth="1px" borderRadius="md" p={3}>
              <Box minW={0}>
                <Text fontWeight="600" noOfLines={2}>{article.title}</Text>
                {article.authorDisplayName && <Text fontSize="sm" color="gray.500">{article.authorDisplayName}</Text>}
              </Box>
              <Button
                as={RouterLink}
                to={`/editor/articles/${encodeURIComponent(article.id)}`}
                size="sm"
                flexShrink={0}
              >
                {intl.formatMessage({ id: 'editorialReview.open' })}
              </Button>
            </HStack>
          ))}
        </VStack>
      )}
    </Box>
  );
}