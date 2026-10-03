import React, { useEffect, useState } from 'react';
import { Box, Button, Heading, HStack, Image, LinkBox, LinkOverlay, SimpleGrid, Text, useDisclosure } from '@chakra-ui/react';
import { Link as RouterLink } from 'react-router-dom';
import { useIntl } from 'react-intl';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import WarpScoresApiService from '../../WarpScoresApiService';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import { articleEditorUrl } from '../../util/articleContext';
import MatchModal from '../contest/MatchModalWithRosters';

function ArticleFeed({ leagueSystemId, seasonId, limit = 6, compact = false, type, subjectId }) {
  const intl = useIntl();
  const [articles, setArticles] = useState([]);
  const [error, setError] = useState(false);
  const [selectedMatch, setSelectedMatch] = useState(null);
  const { isOpen, onOpen, onClose } = useDisclosure();
  const { isAuthenticated } = useAuth0WithUserPermissions();
  useEffect(() => {
    let active = true;
    setArticles([]); setError(false);
    EditorialCommunityApi.articles(leagueSystemId, limit, seasonId, type, subjectId)
      .then(data => { if (active) setArticles(data); })
      .catch(() => { if (active) setError(true); });
    return () => { active = false; };
  }, [leagueSystemId, seasonId, limit, type, subjectId]);
  const openMatchArticle = async article => {
    try {
      const match = await WarpScoresApiService.match(article.matchId);
      if (match) {
        setSelectedMatch(match);
        onOpen();
      }
    } catch {
      setError(true);
    }
  };
  const closeMatchArticle = () => {
    onClose();
    setSelectedMatch(null);
  };
  const canWrite = isAuthenticated;
  const context = [{ type: 'LEAGUE_SYSTEM', id: leagueSystemId }, { type: 'SEASON', id: seasonId }, { type, id: subjectId }].filter(l => l.id);
  return <Box my={4} w="full">
    <HStack justify="space-between" mb={3} flexWrap="wrap">
      <Heading size="sm">{intl.formatMessage({ id: 'news.heading' })}</Heading>
      {canWrite && <Button size="xs" as={RouterLink} to={articleEditorUrl(context)}>{intl.formatMessage({ id: 'news.write' })}</Button>}
    </HStack>
    {error && <Text fontSize="sm">{intl.formatMessage({ id: 'news.error' })}</Text>}
    <SimpleGrid columns={compact ? 1 : { base: 1, md: 2, lg: 3 }} spacing={3}>
      {articles.map(article => <LinkBox key={article.id} borderWidth="1px" borderRadius="md" overflow="hidden">
        <HStack align="start" p={3}>
          {article.coverImageUrl && <Image alt="" src={EditorialCommunityApi.assetUrl(article.coverImageUrl)} boxSize="64px" objectFit="cover" borderRadius="sm" />}
          <Box minW={0}><Heading size="sm">{article.matchArticle
            ? <Button variant="link" p={0} h="auto" textAlign="left" whiteSpace="normal" onClick={() => openMatchArticle(article)}>{article.title}</Button>
            : <LinkOverlay as={RouterLink} to={`/article/${article.slug}`}>{article.title}</LinkOverlay>}</Heading>
            {article.excerpt && <Text fontSize="sm" mt={1} noOfLines={2}>{article.excerpt}</Text>}
          </Box>
        </HStack>
      </LinkBox>)}
    </SimpleGrid>
    {selectedMatch && <MatchModal isOpen={isOpen} onClose={closeMatchArticle} match={selectedMatch} initialTabIndex={3}/>}
  </Box>;
}
export default ArticleFeed;
