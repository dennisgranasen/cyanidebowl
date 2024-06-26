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
  useBreakpointValue,
} from '@chakra-ui/react';

function StandardScreenHeaderCard(mainImageSrc, heading, subHeading, additionalImageSrc, children) {
  return (
    <Card direction="row">
      <Box p="10px">
        <Image objectFit="contain" maxW="140px" src={mainImageSrc} fallback={null} />
      </Box>
      <CardBody>
        <Flex>
          <Box flex="1" overflow="hidden">
            <Heading>{heading}</Heading>
            <Box mb="10px">{subHeading}</Box>
            {children}
          </Box>
          <Box hideBelow="lg">
            <Image hideBelow="lg" objectFit="cover" maxH="140px" src={additionalImageSrc} fallback={null} />
          </Box>
        </Flex>
      </CardBody>
    </Card>
  );
}

function SmallScreenHeaderCard(mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, children) {
  const noAccordion = !detailsHeading;
  return (
    <Card direction="column">
      <Box p="10px">
        <Image objectFit="contain" maxW="140px" src={mainImageSrc} />
      </Box>
      <CardBody>
        <Flex>
          <Box flex="1" overflow="hidden">
            <Heading>{heading}</Heading>
            <Box mb="10px">{subHeading}</Box>
            {noAccordion && children}
            {!noAccordion && (
              <Accordion allowMultiple defaultIndex={!detailsHeading ? [0] : null}>
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
            )}
          </Box>
        </Flex>
      </CardBody>
    </Card>
  );
}

function HeaderCard({ mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, children }) {
  const isSmallScreen = useBreakpointValue( { base: true, sm: true, md: false } );
  return isSmallScreen
    ? SmallScreenHeaderCard(mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, children)
    : StandardScreenHeaderCard(mainImageSrc, heading, subHeading, additionalImageSrc, children);
}

export default HeaderCard;
