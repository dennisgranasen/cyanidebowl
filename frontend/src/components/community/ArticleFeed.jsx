import React, { useEffect, useState } from 'react';
import { Box, Heading, Image, LinkBox, LinkOverlay, SimpleGrid, Text } from '@chakra-ui/react';
import { Link as RouterLink } from 'react-router-dom';
import EditorialCommunityApi from '../../EditorialCommunityApi';

function ArticleFeed({ leagueSystemId, limit = 6 }) {
  const [articles, setArticles] = useState([]);
  useEffect(() => {
    EditorialCommunityApi.articles(leagueSystemId, limit).then(setArticles).catch(() => setArticles([]));
  }, [leagueSystemId, limit]);

  if (!articles.length) return null;
  const [featured, ...rest] = articles;
  return (
    <Box my={4}>
      <Heading size="md" mb={3}>Nyheter</Heading>
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
