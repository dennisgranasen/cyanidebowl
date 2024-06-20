import React from 'react';
import { Alert, AlertDescription, AlertIcon, Spinner } from '@chakra-ui/react';

function LoadingOrErrorWrapper({ loading, error, children }) {
  if (error) {
    return (
      <Alert status={error.type}>
        <AlertIcon />
        <AlertDescription>{error.message}</AlertDescription>
      </Alert>
    );
  }
  if (loading) {
    return <Spinner />;
  }
  return children;
}

export default LoadingOrErrorWrapper;
