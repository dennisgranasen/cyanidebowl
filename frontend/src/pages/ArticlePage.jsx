import React, { useEffect, useState } from 'react';
import { Box, Heading, Image, Text, VStack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import DOMPurify from 'dompurify';
import Navigation from '../components/misc/Navigation';
import EditorialCommunityApi from '../EditorialCommunityApi';
import CommentThread from '../components/community/CommentThread';
import ReactionBar from '../components/community/ReactionBar';

function ArticlePage() {
  const { slug } = useParams();
  const [article, setArticle] = useState(null);
  useEffect(() => { EditorialCommunityApi.article(slug).then(setArticle); }, [slug]);
  if (!article) return null;
  return (
    <VStack align="stretch">
      <Navigation />
      <Box maxW="900px" w="full" mx="auto" p={4}>
        {article.coverImageUrl && <Image src={article.coverImageUrl} w="full" maxH="500px" objectFit="cover" />}
        <Heading mt={4}>{article.title}</Heading>
        <Text opacity={0.7} mt={1}>{article.authorDisplayName}</Text>
        <Box mt={6} className="article-body"
          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(article.bodyHtml || '') }} />
        <Box mt={6}><ReactionBar targetType="ARTICLE" targetId={article.id} /></Box>
        <Box mt={8}><CommentThread targetType="ARTICLE" targetId={article.id} /></Box>
      </Box>
    </VStack>
  );
}
export default ArticlePage;
