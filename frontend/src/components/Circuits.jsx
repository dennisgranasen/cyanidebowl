import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  Heading,
  Input,
  SimpleGrid,
  VStack,
} from '@chakra-ui/react';
import { Field, Form, Formik } from 'formik';
import { useAuth0 } from '@auth0/auth0-react';
import WarpScoresApiService from '../WarpScoresApiService';
import CircuitCard from './circuit/CircuitCard';
import LoadingOrErrorWrapper from './common/LoadingOrErrorWrapper';
import config from '../config';

function Circuits() {
  const { isAuthenticated, isLoading, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState();
  const [circuits, setCircuits] = useState([]);

  const validateCircuitName = (value) => {
    return value?.trim().length > 0 ? null : 'Name is required';
  };

  const addCircuit = (values, actions) => {
    WarpScoresApiService.newCircuit(values.circuitName, getAccessTokenSilently, getAccessTokenWithPopup)
      .then((newCircuit) => {
        const newCircuits = circuits.concat([newCircuit]);
        newCircuits.sort((aCircuit, otherCircuit) => aCircuit.circuitName.localeCompare(otherCircuit.circuitName));
        setCircuits(newCircuits);
      })
      .catch((reason) => {
        actions.setFieldError('circuitName', `Error: ${reason.message}`);
      })
      .then(() => {
        values.circuitName = '';
        actions.setSubmitting(false);
      });
  };

  const fetchCircuits = () => {
    setLoading(true);
    WarpScoresApiService.circuits()
      .then((data) => {
        setCircuits(data);
      })
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString(config.locale) });
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchCircuits();
  }, []);

  return (
    <VStack align="left">
      <LoadingOrErrorWrapper loading={loading} error={error}>
        {circuits?.length > 0 && (
          <>
            <Heading size="md">Circuits</Heading>
            <SimpleGrid columns={{ base: 1, md: 2, lg: 3 }} spacing="1rem" mb="1rem">
              {circuits.map((circuit) => (
                <CircuitCard key={circuit.circuitId} circuit={circuit} showConfigureLink variant="outline" />
              ))}
            </SimpleGrid>
          </>
        )}
      </LoadingOrErrorWrapper>
      <Heading size="md">Create Circuit</Heading>
      <Box mb="1rem">
        Circuits are a collection of leagues and/or competitions that you consider to belong together in order to obtain
        statistics. For example, an ‘NAF Tournament’ circuit could contain all tournaments (competitions) organised by
        the NAF or the ‘Cyanide World Cup Qualifiers’ circuit could contain all leagues and competitions that include
        qualifying tournaments for the Cyanide World Cup.
      </Box>
      <Formik initialValues={{ circuitName: '' }} onSubmit={(values, actions) => addCircuit(values, actions)}>
        {(props) => (
          <Form>
            <Card variant="outline" size="sm">
              <CardHeader>New Circuit</CardHeader>
              <CardBody>
                <Field name="circuitName" validate={validateCircuitName}>
                  {({ field, form }) => (
                    <FormControl isInvalid={form.errors.circuitName && form.touched.circuitName}>
                      <FormLabel>Circuit name</FormLabel>
                      <Input {...field} placeholder="New circuit name" />
                      <FormHelperText>Choose a name, that describes your circuit.</FormHelperText>
                      <FormErrorMessage>{form.errors.circuitName}</FormErrorMessage>
                    </FormControl>
                  )}
                </Field>
              </CardBody>
              <CardFooter>
                <Button
                  mt="1rem"
                  type="submit"
                  isLoading={props.isSubmitting}
                  isDisabled={isLoading || !isAuthenticated}
                >
                  Add Circuit
                </Button>
              </CardFooter>
            </Card>
          </Form>
        )}
      </Formik>
    </VStack>
  );
}

export default Circuits;
