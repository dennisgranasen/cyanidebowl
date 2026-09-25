import React, { useEffect, useState } from 'react';
import { ArrowBackIcon } from '@chakra-ui/icons';
import { Box, Button, Heading, Image, Text, VStack } from '@chakra-ui/react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { useIntl } from 'react-intl';
import { articleBodyStyles } from '../components/community/articleBodyStyles';
import DOMPurify from 'dompurify';
import Navigation from '../components/misc/Navigation';
import EditorialCommunityApi from '../EditorialCommunityApi';
import CommentThread from '../components/community/CommentThread';
import ReactionBar from '../components/community/ReactionBar';
import { homeSeasonTarget } from '../util/leagueSystemNavigation';

function ArticlePage() {
  const intl = useIntl();
  const { userPermissions, user } = useAuth0WithUserPermissions();
  const { slug } = useParams();
  const [article, setArticle] = useState(null);
  useEffect(() => { EditorialCommunityApi.article(slug).then(setArticle); }, [slug]);
  if (!article) return null;
  return (
    <VStack align="stretch">
      <Navigation />
      <Box maxW="900px" w="full" mx="auto" p={4}>
        {article.seasonId && (
          <Button as={RouterLink} to={homeSeasonTarget(article.leagueSystemId, article.seasonId)}
            leftIcon={<ArrowBackIcon />} size="sm" variant="ghost" mb={3}>
            {intl.formatMessage({ id: 'news.backToSeason', defaultMessage: 'Back to season' })}
          </Button>
        )}
        {article.coverImageUrl && <Image src={EditorialCommunityApi.assetUrl(article.coverImageUrl)} w="full" maxH="500px" objectFit="cover" />}
        <Heading mt={4}>{article.title}</Heading>
        <Box>{(userPermissions?.writeEditor || (user?.sub && user.sub === article.authorSubject)) && <Button as={RouterLink} size="sm" to={`/editor/articles/${article.id}`}>{intl.formatMessage({ id: 'news.edit' })}</Button>}</Box>
        <Text opacity={0.7} mt={1}>{article.authorDisplayName}</Text>
        <Box mt={6} className="article-body" sx={articleBodyStyles}
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(article.bodyHtml || '') }} />
        <Box mt={6}><ReactionBar targetType="ARTICLE" targetId={article.id} /></Box>
        <Box mt={8}><CommentThread targetType="ARTICLE" targetId={article.id} /></Box>
      </Box>
    </VStack>
  );
}
export default ArticlePage;
