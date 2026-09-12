import React from 'react';
import { Alert, AlertDescription, AlertIcon, Heading, HStack, Spinner, Text } from '@chakra-ui/react';
import prettyPrint from '../../util/prettyPrint';
import { useIntl } from 'react-intl';

function ErrorIcon({ status }) {
  switch (status) {
    case 'error':
      return (
        <Heading as="h1" fontFamily="nuffleDice" size="3xl">
          kkk
        </Heading>
      );
    case 'warning':
      return (
        <Heading as="h1" fontFamily="nuffleDice" size="3xl">
          nn
        </Heading>
      );
    default:
      return <AlertIcon />;
  }
}

function LoadingOrErrorWrapper({ loading, error, children }) {
  const intl = useIntl();
  if (error) {
    return (
      <Alert status={error.type}>
        <ErrorIcon status={error.type} />
        <AlertDescription>
          <HStack>
            <Text fontWeight="bold">{`${intl.formatMessage({ id: `error.${error.type}`, defaultMessage: prettyPrint(error.type) })}:`}</Text>
            <Text> {error.message}</Text>
          </HStack>
        </AlertDescription>
      </Alert>
    );
  }
  if (loading) {
    return <Spinner />;
  }
  return children;
}

export default LoadingOrErrorWrapper;
