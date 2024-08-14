import { Card, CardBody, Image, Link } from '@chakra-ui/react';
import { ExternalLinkIcon } from '@chakra-ui/icons';
import React from 'react';
import { Link as RouteLink } from 'react-router-dom';
import ImageUrls from '../../ImageUrls';

function DbbcCard() {
  return (
    <Card direction={{ base: 'column', md: 'row' }} variant="outline" overflow="hidden">
      <Image objectFit="contain" maxW="100px" src={ImageUrls.dbbcLogoPng('small')} p="0.8rem" />
      <CardBody>
        It was initially coded to support the{' '}
        <Link href="http://dbbcev.de" isExternal>
          Deutsche Blood Bowl Community <ExternalLinkIcon mx="2x" />
        </Link>{' '}
        (German Blood Bowl Community) organising it&apos;s League &quot;
        <Link as={RouteLink} to="/94dd6ae4-83fa-11ee-b910-02000090a64f">
          Deutsche Blood Bowl Liga
        </Link>
        &quot; but is about to open to support other private leagues as well.
      </CardBody>
    </Card>
  );
}

export default DbbcCard;
