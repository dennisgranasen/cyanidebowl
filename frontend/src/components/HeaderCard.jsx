import React from 'react';
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Card,
  CardBody,
  Flex,
  Heading,
  Image,
} from '@chakra-ui/react';

function StandardScreenHeaderCard(mainImageSrc, heading, subHeading, additionalImageSrc, children) {
  return (
    <Card direction="row">
      <Box>
        <Image objectFit="contain" maxW="140px" src={mainImageSrc} />
      </Box>
      <CardBody>
        <Flex>
          <Box flex="1">
            <Heading>{heading}</Heading>
            <Box mb="10px">{subHeading}</Box>
            {children}
          </Box>
          <Box hideBelow="lg">
            <Image objectFit="contain" maxW="140px" src={additionalImageSrc} fallback={null} />
          </Box>
        </Flex>
      </CardBody>
    </Card>
  );
}

function SmallScreenHeaderCard(mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, children) {
  return (
    <Card direction="column">
      <Box>
        <Image objectFit="contain" maxW="140px" src={mainImageSrc} />
      </Box>
      <CardBody>
        <Flex>
          <Box flex="1">
            <Heading>{heading}</Heading>
            <Box mb="10px">{subHeading}</Box>
            <Accordion allowMultiple>
              <AccordionItem>
                <AccordionButton>
                  <Box as="span" flex="1" textAlign="left">
                    {detailsHeading}
                  </Box>
                  <AccordionIcon />
                </AccordionButton>
                <AccordionPanel>{children}</AccordionPanel>
              </AccordionItem>
            </Accordion>
          </Box>
        </Flex>
      </CardBody>
    </Card>
  );
}

function HeaderCard({ mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, smallscreen, children }) {
  return smallscreen
    ? SmallScreenHeaderCard(mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, children)
    : StandardScreenHeaderCard(mainImageSrc, heading, subHeading, additionalImageSrc, children);
}

export default HeaderCard;
