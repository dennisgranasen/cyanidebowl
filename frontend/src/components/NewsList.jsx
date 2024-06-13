import { Box, List, Text } from '@chakra-ui/react';
import React from 'react';
import NewsItem from './NewsItem';
import hashCode from '../util/HashCode';

function NewsList({ news, headerSize, textSize, ...props }) {
  return (
    news &&
    news.length > 0 && (
      <Box fontSize={headerSize} {...props}>
        <Text fontStyle="italic">Latest BB3 news</Text>
        <List>
          {news
            .sort((news1, news2) => {
              if (!news1.title) return -1;
              if (!news2.title) return 1;
              return 0;
            })
            .map((newsItem) => (
              <NewsItem
                key={hashCode(newsItem.title, newsItem.message)}
                title={newsItem.title}
                message={newsItem.message}
                fontSize={textSize}
              />
            ))}
        </List>
      </Box>
    )
  );
}

export default NewsList;
