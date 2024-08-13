import { Card, CardBody } from '@chakra-ui/react';
import React from 'react';
import { FaSection } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import Disclaimer from '../../components/misc/Disclaimer';

function DisclaimerCard() {
  return (
    <Card direction={{ base: 'column', md: 'row' }} variant="outline">
      <Icon as={FaSection} boxSize="100px" p="0.8rem" />
      <CardBody>
        <Disclaimer />
      </CardBody>
    </Card>
  );
}

export default DisclaimerCard;
