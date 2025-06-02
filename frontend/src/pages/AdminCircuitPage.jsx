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
  HStack,
  Input,
  Select,
  SimpleGrid,
  Table,
  TableContainer,
  Tbody,
  Tfoot,
  Td,
  Th,
  Thead,
  Tr,
  VStack,
} from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import { Field, Form, Formik } from 'formik';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import CircuitLeg from '../components/circuit/CircuitLeg';
import HeaderCard from '../components/common/HeaderCard';
import prettyPrint from '../util/prettyPrint';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import config from '../config';

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

function AdminCircuitPage() {
  const { isAuthenticated, isLoading, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();

  const bbVersion = config.bbVersion || 3;
  const bbVersions = [ 1, 2, 3 ];

  const platforms = [ 'PC', 'Playstation', 'Xbox', 'Switch', 'Cross platform', 'Tabletop', 'Fumbbl' ];
    /*
    'bb1.pc',
    'bb2.pc',
    'bb2.ps',
    'bb2.xbox',
    'bb3.cross',
    'bb3.pc',
    'bb3.ps',
    'bb3.switch',
    'bb3.xbox',
    */
    /*
    "fumbbl.lrb6",
    "fumbbl.2020",
    "tt.lrb6",
    "tt.2020" */
  
  const legTypes = ['League', 'Competition', 'Circuit'];
  const treatLadderOptions = [
    { value: 'knockout', label: 'Treat as Knockout' },
    { value: 'round-robin', label: 'Treat as Round Robin League' },
    { value: 'ladder', label: 'Treat as Ladder' },
  ];


  const initialFormValues = {
    competitionOrLeagueId: '',
    competitionOrLeagueName: '',
    legType: '',
    //platform: 'bb3.cross',
    label: '',
    collectData: true,
    treatLadderAs: '',
  };

  const { circuitId } = useParams();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [circuit, setCircuit] = useState();
  const [searchResults, setSearchResults] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [selectedCompetitionId, setSelectedCompetitionId] = useState('');


  // Handler for row click
  const handleCompetitionClick = (id) => {
    setSelectedCompetitionId(id);
    setSelectedLeagueId('')
  };
  // Handler for row click
  const handleLeagueClick = (id) => {
    setSelectedCompetitionId('');
    setSelectedLeagueId(id)
  };

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
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(setLoading(false));
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

  const onSearchClicked = (values, actions) => {
    console.log('Searching for', values.competitionOrLeagueName, values.platform);
    
    WarpScoresApiService.lookup({
      league_name: values.competitionOrLeagueName,
      bb: values.bbVersion[values.bbVersion.length - 1],
      //platform: values.platform,
      exact: 1, 
      fallback: false
    })
      .then((res) => {
        console.log(res);
        setSearchResults(res);
      })
      .catch((err) => {
        setSearchResults([]);
        console.log(err);
      })
      .finally(() => {
        actions.setSubmitting(false);
      });
  };

  useEffect(() => {
    fetchCircuit(circuitId);
  }, []);

  return (
    <VStack align="left">      
      <Box>
        <Navigation parentPage="admin" currentPage="circuits" circuit={[circuitId, circuit?.circuitName]} />
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
        <Checkbox isChecked={isAuthenticated}>Auth</Checkbox>
        <HStack>
          <Formik initialValues={{ competitionOrLeagueName: '' }} 
            onSubmit={(values, actions) => onSearchClicked(values, actions)}>
            {(props) => (
              <Card as={Form} variant="outline" size="sm">
                <CardHeader>Search for id</CardHeader>
                <SimpleGrid as={CardBody} columns={{ base: 1, md: 2, xl: 3 }} gap="1rem">
                  <Box>
                    <Field
                      name="competitionOrLeagueName"
                      validate={(value) => (value?.trim().length > 0 ? null : 'Competition or League name required.')}
                    >
                      {({ field, form }) => (
                        <FormControl isInvalid={form.errors.competitionOrLeagueName && form.touched.competitionOrLeagueName}>
                          <FormLabel>Name</FormLabel>
                          <FormHelperText>Name of the Competition or League to search for</FormHelperText>
                          <Input {...field} placeholder="Competition/League name" />
                          <FormErrorMessage>{form.errors.competitionOrLeagueName}</FormErrorMessage>
                        </FormControl>
                      )}
                    </Field>
                  <Field
                    name="bbVersion"
                    validate={(value) => (value?.trim().length > 0 ? null : 'Specify version')}
                  >
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.platform && form.touched.platform}>
                        <FormLabel>Blood Bowl version</FormLabel>
                        <FormHelperText>Select which version of Blood bowl the leg is registered for?</FormHelperText>
                        <Select {...field} variant="outlined" placeholder="Select version">
                          {bbVersions.map((versionOption) => (
                            <option value={versionOption} key={versionOption}>
                              {"Blood Bowl " + versionOption.toString()}
                            </option>
                          ))}
                        </Select>
                        <FormErrorMessage>{form.errors.bbVersion}</FormErrorMessage>
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
                      isDisabled={isLoading || !isAuthenticated}
                    >
                      Search
                    </Button>
                  </Box>
                </CardFooter>
              </Card>
            )}
          </Formik>
           {/* Search Results Table */}
          {searchResults.leagues && searchResults.leagues.length > 0 && (
            <Box mt={4}>
              <Heading size="sm" mb={2}>Leagues</Heading>
              <TableContainer>
                <Table variant="simple" size="sm">
                  <Thead>
                    <Tr>
                      <Th>Name</Th>
                      <Th>ID</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {searchResults.leagues.map((item) => (
                      <Tr key={item.id || item.uuid || item.leagueId}
                                _hover={{ bg: 'gray.100', cursor: 'pointer' }}
                                onClick={() => handleLeagueClick(item.id || item.uuid || item.leagueId)}>
                        <Td>{item.name || item.leagueName}</Td>
                        <Td>{item.id || item.uuid || item.leagueId}</Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </TableContainer>
            </Box>
          )}
          {searchResults.competitions && searchResults.competitions.length > 0 && (
            <Box mt={4}>
              <Heading size="sm" mb={2}>Competitions</Heading>
              <TableContainer>
                <Table variant="simple" size="sm">
                  <Thead>
                    <Tr>
                      <Th>Name</Th>
                      <Th>ID</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {searchResults.competitions.map((item) => (
                      <Tr key={item.id || item.uuid || item.leagueId}
                                _hover={{ bg: 'gray.100', cursor: 'pointer' }}
                                onClick={() => handleCompetitionClick(item.id || item.uuid || item.competitionId)}>
                        <Td>{item.name || item.leagueName}</Td>
                        <Td>{item.id || item.uuid || item.competitionId}</Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </TableContainer>
            </Box>
          )}
        </HStack>


        {/* Add Leg Form */}
        <Formik initialValues={
          {
            ...initialFormValues, 
            competitionOrLeagueId: selectedCompetitionId || '',
          }}
                onSubmit={(values, actions) => onAddLegClicked(values, actions)}>
          {(props) => (
            <Card as={Form} variant="outline" size="sm">
              <CardHeader>New Leg</CardHeader>
              <SimpleGrid as={CardBody} columns={{ base: 1, md: 2, xl: 3 }} gap="1rem">
                <Box>
                  {useEffect(() => {
                      if (selectedCompetitionId) {
                        props.setFieldValue('competitionOrLeagueId', selectedCompetitionId);
                      } else if (selectedLeagueId) {
                        props.setFieldValue('competitionOrLeagueId', selectedLeagueId);
                      } 
                    }, [selectedCompetitionId, selectedLeagueId])
                  }                 
                  <Field
                    name="competitionOrLeagueId"
                    validate={(value) => (value?.trim().length > 0 ? null : 'Competition or League id required.')}
                  >
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.competitionOrLeagueId && form.touched.competitionOrLeagueId}>
                        <FormLabel>Id</FormLabel>
                        <FormHelperText>Id of the Competition or League to add to Circuit</FormHelperText>
                        <Input {...field} placeholder="Competition/League Uuid" />
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
                        <FormHelperText>Custom name/label for this circuit leg</FormHelperText>
                        <Input {...field} placeholder="Enter custom label" />
                        <FormErrorMessage>{form.errors.label}</FormErrorMessage>
                      </FormControl>
                    )}
                  </Field>
                </Box>
                <Box>
                  <Field
                    name="legType"
                    validate={(value) => (value?.trim().length > 0 ? null : 'Select League, Competition or Circuit')}
                  >
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.legType && form.touched.legType}>
                        <FormLabel>Type</FormLabel>
                        <FormHelperText>Is the id for a League, a Competition or a sub-circuit?</FormHelperText>
                        <Select {...field} variant="outlined" placeholder="Select leg type">
                          {legTypes.map((legTypeOption) => (
                            <option value={legTypeOption} key={legTypeOption}>
                              {legTypeOption}
                            </option>
                          ))}
                        </Select>
                        <FormErrorMessage>{form.errors.legType}</FormErrorMessage>
                      </FormControl>
                    )}
                  </Field>
                </Box>
                <Box>
                  <Field name="collectData">
                    {({ field }) => (
                      <FormControl>
                        <FormLabel>Data collection</FormLabel>
                        <FormHelperText>Should this site collect data from the competition/league?</FormHelperText>
                        <Checkbox {...field} readOnly={false}>
                          Collect data?
                        </Checkbox>
                      </FormControl>
                    )}
                  </Field>
                </Box>

                <Box>
                  <Field name="treatLadderAs">
                      {({ field, form }) => (
                        <FormControl isInvalid={form.errors.treatLadderAs && form.touched.treatLadderAs}>
                          <FormLabel>Ladder specific</FormLabel>
                          <FormHelperText>How should ladder competitions be handled (this is only used if the format is ladder in cyanide database)?</FormHelperText>
                          <Select {...field} placeholder="Select option">
                            {treatLadderOptions.map((option) => (
                              <option value={option.value} key={option.value}>
                                {option.label}
                              </option>
                            ))}
                          </Select>
                          <FormErrorMessage>{form.errors.treatLadderAs}</FormErrorMessage>
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
                    isDisabled={isLoading || !isAuthenticated}
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

export default AdminCircuitPage;
