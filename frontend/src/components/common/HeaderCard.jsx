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
import config from '../../config';

const { smallScreenBreakpointValues } = config;

function StandardScreenHeaderCard(mainImageSrc, heading, subHeading, additionalImageSrc, children, ...props) {
  return (
    <Card direction="row" {...props}>
      <Box p="0.5rem">
        <Image objectFit="contain" maxW="140px" src={mainImageSrc} fallback={null} />
      </Box>
      <CardBody>
        <Flex>
          <Box flex="1" overflow="hidden">
            <Heading>{heading}</Heading>
            <Box mb="0.5rem">{subHeading}</Box>
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

function SmallScreenHeaderCard(
  mainImageSrc,
  heading,
  subHeading,
  detailsHeading,
  additionalImageSrc,
  children,
  ...props
) {
  const noAccordion = !detailsHeading;
  return (
    <Card {...props}>
      <Box p="0.5rem">
        <Image objectFit="contain" maxW="140px" src={mainImageSrc} />
      </Box>
      <CardBody>
        <Flex>
          <Box flex="1" overflow="hidden">
            <Heading>{heading}</Heading>
            <Box mb="0.5rem">{subHeading}</Box>
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

function HeaderCard({ mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, children, ...props }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  return isSmallScreen
    ? SmallScreenHeaderCard(mainImageSrc, heading, subHeading, detailsHeading, additionalImageSrc, children, {
        ...props,
      })
    : StandardScreenHeaderCard(mainImageSrc, heading, subHeading, additionalImageSrc, children, { ...props });
}

export default HeaderCard;
