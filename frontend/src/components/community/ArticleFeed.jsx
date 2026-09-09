import React, { useEffect, useState } from 'react';
import { Box, Button, Heading, HStack, Image, LinkBox, LinkOverlay, SimpleGrid, Text } from '@chakra-ui/react';
import { Link as RouterLink } from 'react-router-dom';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';

function ArticleFeed({ leagueSystemId, limit = 6 }) {
  const [articles, setArticles] = useState([]);
  const { checkPermissions, userPermissions } = useAuth0WithUserPermissions();
  useEffect(() => {
    EditorialCommunityApi.articles(leagueSystemId, limit).then(setArticles).catch(() => setArticles([]));
  }, [leagueSystemId, limit]);

  const canWrite = checkPermissions && userPermissions?.writeEditor;
  if (!articles.length && !canWrite) return null;

  const [featured, ...rest] = articles;
  return (
    <Box my={4}>
      <HStack justify="space-between" mb={3}>
        <Heading size="md">Nyheter</Heading>
        {canWrite && (
          <Button size="sm" as={RouterLink} to="/editor/articles/new">Skriv artikel</Button>
        )}
      </HStack>
      {!articles.length && <Text color="gray.500">Inga publicerade artiklar Ã¤nnu.</Text>}
      {featured && (
        <LinkBox borderWidth="1px" borderRadius="lg" overflow="hidden" mb={3}>
          {featured.coverImageUrl && <Image src={featured.coverImageUrl} w="full" maxH="320px" objectFit="cover" />}
          <Box p={4}>
            <Heading size="md"><LinkOverlay as={RouterLink} to={`/article/${featured.slug}`}>{featured.title}</LinkOverlay></Heading>
            {featured.excerpt && <Text mt={2}>{featured.excerpt}</Text>}
          </Box>
        </LinkBox>
      )}
      <SimpleGrid columns={{ base: 1, md: 2, lg: 3 }} spacing={3}>
        {rest.map((article) => (
          <LinkBox key={article.id} borderWidth="1px" borderRadius="md" p={3}>
            <Heading size="sm"><LinkOverlay as={RouterLink} to={`/article/${article.slug}`}>{article.title}</LinkOverlay></Heading>
            {article.excerpt && <Text fontSize="sm" mt={2}>{article.excerpt}</Text>}
          </LinkBox>
        ))}
      </SimpleGrid>
    </Box>
  );
}
export default ArticleFeed;
