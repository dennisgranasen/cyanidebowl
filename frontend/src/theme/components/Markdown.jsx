import { Link } from '@chakra-ui/react';
import React from 'react';
import { ExternalLinkIcon } from '@chakra-ui/icons';

const markDownTheme = {
  a: (props) => {
    const noImage = !props.children[0].type;
    return /https?:/.test(props.href) ? (
      <Link isExternal href={props.href}>
        {props.children}
        {noImage && <ExternalLinkIcon mx="2px" />}
      </Link>
    ) : (
      <Link href={props.href}>{props.children}</Link>
    );
  },
};

export default markDownTheme;
