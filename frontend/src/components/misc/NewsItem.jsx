import { Link, List, ListItem } from '@chakra-ui/react';
import { ExternalLinkIcon } from '@chakra-ui/icons';
import React from 'react';

function NewsItem({ title, message, fontSize }) {
  const hasStringTitle = title && typeof title === 'string' && title.length > 0;
  const hasStringMessage = message && typeof message === 'string' && message.length > 0;
  const color = hasStringMessage && message.match(/maintenance/i) ? 'orange' : null;
  let text;
  if (hasStringTitle) {
    text = title;
  } else if (hasStringMessage && !message.match(/^http/)) text = message;
  const url = hasStringMessage && message.match(/^http/) ? message : null;
  return (
    <List color={color} fontSize={fontSize}>
      <ListItem>
        {url ? (
          <Link href={url} isExternal>
            <>
              {text}
              <ExternalLinkIcon mx="2px" />
            </>
          </Link>
        ) : (
          text
        )}
      </ListItem>
    </List>
  );
}

export default NewsItem;
