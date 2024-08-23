import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardBody,
  CardFooter,
  CardHeader,
  Checkbox,
  FormControl,
  FormErrorMessage,
  FormHelperText,
  FormLabel,
  Heading,
  Input,
  Select,
  SimpleGrid,
  Table,
  TableContainer,
  Tbody,
  Tfoot,
  Th,
  Thead,
  Tr,
  VStack,
} from '@chakra-ui/react';
import {useNavigate, useParams} from 'react-router-dom';
import { Field, Form, Formik } from 'formik';
import { useAuth0 } from '@auth0/auth0-react';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import CircuitLeg from '../components/circuit/CircuitLeg';
import HeaderCard from '../components/common/HeaderCard';
import prettyPrint from '../util/PrettyPrint';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import config from '../config';
import {useAuth0WithUserPermissions} from "../hooks/useAuth0WithUserPermissions";

function TableColumns() {
  return (
    <Tr>
      <Th />
      <Th>Label</Th>
      <Th>League/Competition</Th>
      <Th>LegType</Th>
      <Th>Game version</Th>
      <Th>Platform</Th>
      <Th>Knockout?</Th>
      <Th>Collect?</Th>
    </Tr>
  );
}

const initialFormValues = {
  leagueOrCompetitionId: '',
  legType: '',
  platform: '',
  label: '',
  isCollect: true,
  isKnockout: false,
};

function CircuitPage() {
  const {
    authenticationReady,
      checkPermissions,
    userPermissions,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  } = useAuth0WithUserPermissions();

  const navigate = useNavigate()

  useEffect(() => {
    if ( authenticationReady && checkPermissions && !userPermissions.readCurrentUser)
    {
      navigate("/");
    }
  }, [authenticationReady, checkPermissions, userPermissions]);

  const platforms = [
    'bb1.pc',
    'bb2.pc',
    'bb2.ps',
    'bb2.xbox',
    'bb3.cross',
    'bb3.pc',
    'bb3.ps',
    'bb3.switch',
    'bb3.xbox',
    /*
    "fumbbl.lrb6",
    "fumbbl.2020",
    "tt.lrb6",
    "tt.2020" */
  ];
  const legTypes = ['League', 'Competition'];
  const { circuitId } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [circuit, setCircuit] = useState();

  const compareLegs = (leg, otherLeg) => {
    return leg.label.localeCompare(otherLeg.label);
  };

  const fetchCircuit = (id) => {
    setLoading(true);
    WarpScoresApiService.circuits(id)
      .then((res) => {
        if (res.circuitLegs == null) res.circuitLegs = [];
        else res.circuitLegs.sort(compareLegs);
        setCircuit(res);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString(config.locale) }))
      .finally(() => setLoading(false));
  };

  const onAddLegClicked = (values, actions) => {
    const platformParts = values.platform.split('.');
    const game = platformParts[0].toUpperCase();
    const p = platformParts[1].toUpperCase();
    WarpScoresApiService.addLegToCircuit(
      circuit.circuitId,
      values.leagueOrCompetitionId,
      values.legType,
      values.label,
      game,
      p,
      values.isCollect,
      values.isKnockout,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(() => fetchCircuit(circuitId))
      .catch((err) => console.log(err))
      .finally(() => {
        values = initialFormValues;
        actions.setSubmitting(false);
      });
  };

  useEffect(() => {
    fetchCircuit(circuitId);
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="circuits" circuit={[circuitId, circuit?.circuitName]} />
      </Box>
      <HeaderCard heading={circuit ? circuit.circuitName : 'Circuit'} detailsHeading="Circuit details" />
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <Heading size="md">Circuit legs</Heading>
        <TableContainer mb="1rem">
          <Table variant="simpleClickable" size="sm">
            <Thead>
              <TableColumns />
            </Thead>
            <Tbody>
              {circuit?.circuitLegs.map((circuitLeg) => (
                <CircuitLeg key={circuitLeg.circuitLegId} circuitLeg={circuitLeg} />
              ))}
            </Tbody>
            <Tfoot>
              <TableColumns />
            </Tfoot>
          </Table>
        </TableContainer>

        <Heading size="md">Add Leg</Heading>
        <Box mb="1rem">
          A Circuit Leg is either a competition or a league (with all it&apos;s competitions), specified by leg type.
          You may add a custom label and define, if data from Cyanide API should be collected periodically. If you
          select &quot;treat Ladder as Knockout&quot;, all competitions of type ladder will be rendered as if they were
          knockout tournaments.
        </Box>
        <Formik initialValues={initialFormValues} onSubmit={(values, actions) => onAddLegClicked(values, actions)}>
          {(props) => (
            <Card as={Form} variant="outline" size="sm">
              <CardHeader>New Leg</CardHeader>
              <SimpleGrid as={CardBody} columns={{ base: 1, md: 2, xl: 3 }} gap="1rem">
                <Box>
                  <Field
                    name="competitionOrLeagueId"
                    validate={(value) => (value?.trim().length > 0 ? null : 'Competition or League id required.')}
                  >
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.competitionOrLeagueId && form.touched.competitionOrLeagueId}>
                        <FormLabel>Id</FormLabel>
                        <Input {...field} placeholder="Competition/League Uuid" />
                        <FormHelperText>Id of the Competition or League to add to Circuit</FormHelperText>
                        <FormErrorMessage>{form.errors.competitionOrLeagueId}</FormErrorMessage>
                      </FormControl>
                    )}
                  </Field>
                </Box>
                <Box>
                  <Field name="label" validate={(value) => (value?.trim().length > 0 ? null : 'Enter a custom label')}>
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.label && form.touched.label}>
                        <FormLabel>Label</FormLabel>
                        <Input {...field} placeholder="Enter custom label" />
                        <FormHelperText>Custom name/label for this circuit leg</FormHelperText>
                        <FormErrorMessage>{form.errors.label}</FormErrorMessage>
                      </FormControl>
                    )}
                  </Field>
                </Box>
                <Box>
                  <Field
                    name="legType"
                    validate={(value) => (value?.trim().length > 0 ? null : 'Select either League or Competition')}
                  >
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.legType && form.touched.legType}>
                        <FormLabel>Type</FormLabel>
                        <Select {...field} variant="outlined" placeholder="Select leg type">
                          {legTypes.map((legTypeOption) => (
                            <option value={legTypeOption} key={legTypeOption}>
                              {legTypeOption}
                            </option>
                          ))}
                        </Select>
                        <FormHelperText>League or Competition?</FormHelperText>
                        <FormErrorMessage>{form.errors.legType}</FormErrorMessage>
                      </FormControl>
                    )}
                  </Field>
                </Box>
                <Box>
                  <Field
                    name="platform"
                    validate={(value) => (value?.trim().length > 0 ? null : 'Specify platform (e.g. BB3 Cross)')}
                  >
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.platform && form.touched.platform}>
                        <FormLabel>Platform</FormLabel>
                        <Select {...field} variant="outlined" placeholder="Select platform">
                          {platforms.map((platformOption) => (
                            <option value={platformOption} key={platformOption}>
                              {prettyPrint(platformOption)}
                            </option>
                          ))}
                        </Select>
                        <FormHelperText>Which platform?</FormHelperText>
                        <FormErrorMessage>{form.errors.platform}</FormErrorMessage>
                      </FormControl>
                    )}
                  </Field>
                </Box>
                <Box>
                  <FormLabel>Misc</FormLabel>
                  <Field name="treatLadderAsKnockout">
                    {({ field }) => (
                      <FormControl>
                        <Checkbox {...field} readOnly={false}>
                          Treat Ladder as Knockout?
                        </Checkbox>
                      </FormControl>
                    )}
                  </Field>
                  <Field name="collectData">
                    {({ field }) => (
                      <FormControl>
                        <Checkbox {...field} readOnly={false}>
                          Collect data?
                        </Checkbox>
                      </FormControl>
                    )}
                  </Field>
                </Box>
              </SimpleGrid>
              <CardFooter>
                <Box>
                  <Button
                    mt="1rem"
                    type="submit"
                    isLoading={props.isSubmitting}
                    isDisabled={!authenticationReady || !userPermissions.writeLeagueAdmin}
                  >
                    Add leg
                  </Button>
                </Box>
              </CardFooter>
            </Card>
          )}
        </Formik>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default CircuitPage;
