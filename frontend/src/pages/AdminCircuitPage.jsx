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
  Tab,
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
      <Th>League</Th>
      <Th>Competition</Th>
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
  
  const gameTypes = {
    bb1: {
      name: 'Blood Bowl 1',
      ruleset: ['LRB6'],
      platforms: ['PC'],
    },
    bb2: {
      name: 'Blood Bowl 2',
      ruleset: ['LRB6'],
      platforms: ['PC', 'Playstation', 'Xbox'],
    },
    bb3: {
      name: 'Blood Bowl 3',
      ruleset: ['BB2020'],
      platforms: ['PC', 'Playstation', 'Xbox', 'Switch'],
    },
    bloodbowl: {
      name: 'Blood Bowl',
      rulesets: ['LRB6', 'BB2016', 'BB2020', 'Other'],
      defaultRuleset: 'BB2020',
      platforms: ['Tabletop', 'Fumbbl', 'TTS', 'Other'],
    },
    sevens: {
      name: 'Blood Bowl 7s',
      rulesets: ['LRB6', 'BB2016', 'BB2020', 'Other'],
      defaultRuleset: 'BB2020',
      platforms: ['Tabletop', 'TTS'],
    },
    dungeonbowl: {
      name: 'Dungeon Bowl',
      ruleset: ['DB5', 'DB2021', 'Other'],
      defaultRuleset: 'DB2021',
      platforms: ['Tabletop', 'TTS'],
    },
    gutterbowl: {
      name: 'Gutter Bowl',
      ruleset: ['GB2023', 'Other'],
      platforms: ['Tabletop', 'TTS'],
    }
  };

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
    collectData: true, // checked by default
    treatLadderAs: '',
    competitionFormat: '',
  };

  const {circuitId} = useParams();
  const [bbVersion, setBbVersion] = useState(String(config.defaultOpus || 3)); // Default to "3"
  const [gameType, setGameType] = useState('Blood Bowl 3'); // Default to BB3
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [circuit, setCircuit] = useState();
  const [searchResults, setSearchResults] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [selectedCompetitionId, setSelectedCompetitionId] = useState('');
  const [label, setLabel] = useState('');

  const [selectedLeague, setSelectedLeague] = useState(null);
  const [selectedCompetition, setSelectedCompetition] = useState(null);


  // Helper to map bbVersion to gameType key
  const bbVersionToGameTypeKey = (bbVersion) => {
    switch (String(bbVersion)) {
      case '1': return 'bb1';
      case '2': return 'bb2';
      case '3': return 'bb3';
      default: return 'bb3';
    }
  };

  const handleCompetitionClick = (id, name) => {
    setSelectedCompetitionId(id);
    setLabel(name);
    const compObj = (searchResults.competitions || []).find(
      c => (c.id || c.uuid || c.competitionId) === id
    );
    setSelectedCompetition(compObj || null);
    // Use bbVersion to determine game type
    const gameTypeKey = bbVersionToGameTypeKey(bbVersion);
    setSelectedGameType(gameTypeKey);

    // Set platform and ruleset based on game type
    const platforms = getPlatforms(gameTypeKey);
    const rulesets = getRulesets(gameTypeKey);
    const platform = platforms.length === 1 ? platforms[0] : getDefaultPlatform(gameTypeKey);
    const ruleset = rulesets.length === 1 ? rulesets[0] : getDefaultRuleset(gameTypeKey);
    setSelectedPlatform(platform);
    setSelectedRuleset(ruleset);

    if (compObj.leagueId) {
      setSelectedLeagueId(compObj.leagueId);
      WarpScoresApiService.leagues(compObj.leagueId, bbVersion)
        .then((res) => {
          //console.log('League details:', res);
          setSelectedLeague(res);
        })
        .catch((reason) => {
          //console.log('Could not fetch league details:', reason);
          setError({ type: 'error', message: reason.toLocaleString() });
        });
    } else {
      WarpScoresApiService.competition(id, bbVersion)
        .then((res1) => { 
          //console.log('Competition details:', res1); 
          //setSelectedCompetition(res1);
          //setSelectedCompetitionId(id);
        })
        .catch((reason1) => {
          //console.log('Could not fetch competition details:', reason1);
          //console.log('Trying to fetch league details instead: ', selectedLeagueId);
          if (reason1.response && reason1.response.status === 404) {
            var theLeague = null;
            for (const league of searchResults.leagues) {
              if (theLeague) break;
              WarpScoresApiService.leagueCompetitions(league.id, bbVersion)
                .then((competitions) => {
                  //console.log('Competitions:', competitions);
                    const foundComp = competitions.find((comp) => comp.uuid === id);
                    if (foundComp) {
                      console.log('Found league for competition:', name);
                      console.log('Found competition:', foundComp);
                      theLeague = league;
                      setSelectedCompetition(foundComp);
                      setSelectedLeagueId(league.id);
                      setLabel(league.name + " - " + name);
                      setSelectedLeague(league);
                    }
                })
                .catch((reason2) => {
                  //console.log('Could not fetch league details:', reason2);
                  setError({ type: 'error', message: reason2.toLocaleString() });
                }) 
            }
          }
        });  
      }
  };  
  // Handler for row click
  const handleLeagueClick = (id, name) => {
    setSelectedCompetitionId('');
    setSelectedLeagueId(id);
    setLabel(name);

    // Use bbVersion to determine game type
    const gameTypeKey = bbVersionToGameTypeKey(bbVersion);
    setSelectedGameType(gameTypeKey);

    // Set platform and ruleset based on game type
    const platforms = getPlatforms(gameTypeKey);
    const rulesets = getRulesets(gameTypeKey);
    const platform = platforms.length === 1 ? platforms[0] : getDefaultPlatform(gameTypeKey);
    const ruleset = rulesets.length === 1 ? rulesets[0] : getDefaultRuleset(gameTypeKey);
    setSelectedPlatform(platform);
    setSelectedRuleset(ruleset);

    WarpScoresApiService.leagues(id, bbVersion)
      .then((res) => { console.log('League details:', res); setSelectedLeague(res); })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }));  

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
    WarpScoresApiService.addLegToCircuit(
      circuit.circuitId,
      values.leagueId,
      values.competitionId,
      values.legType,
      values.label,
      bbVersion,
      values.platform,
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
    setBbVersion(values.bbVersion); // values.bbVersion is now always a string
    console.log('Searching for', values.competitionOrLeagueName, values.bbVersion);
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
  
  // Extract gameType keys for select options
  const gameTypeKeys = Object.keys(gameTypes);

  // Helper to get platforms/rulesets for selected gameType
  const getPlatforms = (type) => gameTypes[type]?.platforms || [];
  const getRulesets = (type) => gameTypes[type]?.rulesets || gameTypes[type]?.ruleset || [];

  // Helper to get default value for ruleset
  const getDefaultRuleset = (type) => {
    const gt = gameTypes[type];
    if (!gt) return '';
    if (gt.defaultRuleset) return gt.defaultRuleset;
    if (gt.rulesets && gt.rulesets.length > 0) return gt.rulesets[0];
    if (gt.ruleset && gt.ruleset.length > 0) return gt.ruleset[0];
    return '';
  };

  // Helper to get default value for platform
  const getDefaultPlatform = (type) => {
    const gt = gameTypes[type];
    if (!gt) return '';
    if (gt.platforms && gt.platforms.length > 0) return gt.platforms[0];
    return '';
  };

  // Add state for selected gameType, platform, and ruleset
  const [selectedGameType, setSelectedGameType] = useState('');
  const [selectedPlatform, setSelectedPlatform] = useState('');
  const [selectedRuleset, setSelectedRuleset] = useState('');

  // Update platform and ruleset automatically when gameType changes
  useEffect(() => {
    if (!selectedGameType) return;

    // Platform
    const platforms = getPlatforms(selectedGameType);
    if (platforms.length === 1) {
      setSelectedPlatform(platforms[0]);
    } else if (platforms.length > 1) {
      setSelectedPlatform(getDefaultPlatform(selectedGameType));
    } else {
      setSelectedPlatform('');
    }

    // Ruleset
    const rulesets = getRulesets(selectedGameType);
    if (rulesets.length === 1) {
      setSelectedRuleset(rulesets[0]);
    } else if (rulesets.length > 1) {
      setSelectedRuleset(getDefaultRuleset(selectedGameType));
    } else {
      setSelectedRuleset('');
    }
  }, [selectedGameType]);

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
          <Formik
  initialValues={{ competitionOrLeagueName: '', bbVersion: bbVersion }}
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
                    validate={(value) => (value?.length > 0 ? null : 'Specify version')}                    
                  >
                    {({ field, form }) => (
                      <FormControl isInvalid={form.errors.platform && form.touched.platform}>
                        <FormLabel>Blood Bowl version</FormLabel>
                        <FormHelperText>Select which version of Blood bowl the leg is registered for?</FormHelperText>
                        <Select {...field} variant="outlined" placeholder="Select version">
                          {bbVersions.map((versionOption) => (
                            <option value={String(versionOption)} key={versionOption}>
                              {"Blood Bowl " + versionOption}
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
                                onClick={() => handleLeagueClick(item.id || item.uuid || item.leagueId, item.name)}>
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
                                onClick={() => handleCompetitionClick(item.id || item.uuid || item.competitionId, item.name)}>
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
        <Formik
          initialValues={{
            ...initialFormValues,
            competitionOrLeagueId: selectedCompetitionId || '',
            label: label || '',
            gameType: selectedGameType,
            platform: selectedPlatform,
            ruleset: selectedRuleset,
          }}
          enableReinitialize
          onSubmit={(values, actions) => onAddLegClicked(values, actions)}
        >
          {(props) => {
            // Sync Formik fields with state when state changes
            useEffect(() => {
              props.setFieldValue('gameType', selectedGameType);
              props.setFieldValue('platform', selectedPlatform);
              props.setFieldValue('ruleset', selectedRuleset);
            }, [selectedGameType, selectedPlatform, selectedRuleset]);
            useEffect(() => {
              props.setFieldValue('competitionOrLeagueId', selectedCompetitionId || '');
              props.setFieldValue('label', label || '');
            }, [selectedCompetitionId, label]);
            // Set competitionFormat in Formik when a competition is selected
            useEffect(() => {
              if (selectedCompetition && selectedCompetition.format) {
                props.setFieldValue('competitionFormat', selectedCompetition.format);
              } else {
                props.setFieldValue('competitionFormat', '');
              }
            }, [selectedCompetition]);

            return (
              <Card as={Form} variant="outline" size="sm">
                <CardHeader>New Leg</CardHeader>
                <SimpleGrid as={CardBody} columns={{ base: 1, md: 2, xl: 3 }} gap="1rem">
                  {/* Game Type */}
                  <Box>
                    <Field name="gameType" validate={value => value ? null : 'Select game type'}>
                      {({ field, form }) => (
                        <FormControl isInvalid={form.errors.gameType && form.touched.gameType}>
                          <FormLabel>Game Type</FormLabel>
                          <Select
                            {...field}
                            placeholder="Select game type"
                            onChange={e => {
                              const value = e.target.value;
                              setSelectedGameType(value);

                              // Set platform and ruleset automatically
                              const platforms = getPlatforms(value);
                              const rulesets = getRulesets(value);

                              let platform = '';
                              if (platforms.length === 1) {
                                platform = platforms[0];
                              } else if (platforms.length > 1) {
                                platform = getDefaultPlatform(value);
                              }
                              setSelectedPlatform(platform);
                              props.setFieldValue('platform', platform);

                              let ruleset = '';
                              if (rulesets.length === 1) {
                                ruleset = rulesets[0];
                              } else if (rulesets.length > 1) {
                                ruleset = getDefaultRuleset(value);
                              }
                              setSelectedRuleset(ruleset);
                              props.setFieldValue('ruleset', ruleset);

                              props.setFieldValue('gameType', value);
                            }}
                          >
                            {gameTypeKeys.map(key => (
                              <option value={key} key={key}>
                                {gameTypes[key].name}
                              </option>
                            ))}
                          </Select>
                          <FormErrorMessage>{form.errors.gameType}</FormErrorMessage>
                        </FormControl>
                      )}
                    </Field>
                  </Box>

                  {/* Platform */}
                  {getPlatforms(selectedGameType).length > 1 && (
                    <Box>
                      <Field name="platform" validate={value => value ? null : 'Select platform'}>
                        {({ field, form }) => (
                          <FormControl isInvalid={form.errors.platform && form.touched.platform}>
                            <FormLabel>Platform</FormLabel>
                            <Select
                              {...field}
                              placeholder="Select platform"
                              value={props.values.platform}
                              onChange={e => {
                                setSelectedPlatform(e.target.value);
                                props.setFieldValue('platform', e.target.value);
                              }}
                              isDisabled={!selectedGameType}
                            >
                              {getPlatforms(selectedGameType).map(platform => (
                                <option value={platform} key={platform}>
                                  {platform}
                                </option>
                              ))}
                            </Select>
                            <FormErrorMessage>{form.errors.platform}</FormErrorMessage>
                          </FormControl>
                        )}
                      </Field>
                    </Box>
                  )}

                  {/* Ruleset */}
                  {getRulesets(selectedGameType).length > 1 && (
                    <Box>
                      <Field name="ruleset" validate={value => value ? null : 'Select ruleset'}>
                        {({ field, form }) => (
                          <FormControl isInvalid={form.errors.ruleset && form.touched.ruleset}>
                            <FormLabel>Ruleset</FormLabel>
                            <Select
                              {...field}
                              placeholder="Select ruleset"
                              value={props.values.ruleset}
                              onChange={e => {
                                setSelectedRuleset(e.target.value);
                                props.setFieldValue('ruleset', e.target.value);
                              }}
                              isDisabled={!selectedGameType}
                            >
                              {getRulesets(selectedGameType).map(ruleset => (
                                <option value={ruleset} key={ruleset}>
                                  {ruleset}
                                </option>
                              ))}
                            </Select>
                            <FormErrorMessage>{form.errors.ruleset}</FormErrorMessage>
                          </FormControl>
                        )}
                      </Field>
                    </Box>
                  )}
                  <Box>
                    { useEffect(() => {
                        switch(bbVersion)
                        {
                          case 1:
                            props.setFieldValue('legType', 'Blood Bowl 1: Legendary edition (PC)');
                            break;
                          case 2:
                            props.setFieldValue('legType', 'Blood Bowl 2: PC/Mac');
                            break;
                          case 3:
                            props.setFieldValue('legType', 'Blood Bowl 3: PC/Cross');
                            break;
                        }
                        if (selectedCompetitionId) {
                          props.setFieldValue('competitionOrLeagueId', selectedCompetitionId);                        
                          props.setFieldValue('legType', 'Competition');
                        } else if (selectedLeagueId) {
                          props.setFieldValue('competitionOrLeagueId', selectedLeagueId);
                          props.setFieldValue('legType', 'League');
                        } 
                        if (label) {
                          props.setFieldValue('label', label);
                        }

                      }, [selectedCompetitionId, selectedLeagueId, label])
                    }
                    {/* League Id field: only show for BB1, BB2, BB3 */}
                    {['bb1', 'bb2', 'bb3'].includes(selectedGameType) && (
                      <Box>
                        <Field
                          name="leagueId"
                          validate={(value) => (value?.trim().length > 0 ? null : 'League id required.')}
                        >
                          {({ field, form }) => (
                            <FormControl isInvalid={form.errors.leagueId && form.touched.leagueId}>
                              <FormLabel>League Id</FormLabel>
                              <FormHelperText>Id of the League to add to Circuit</FormHelperText>
                              <Input {...field} placeholder="League Uuid" value={selectedLeagueId || field.value} readOnly={!!selectedLeagueId} />
                              <FormErrorMessage>{form.errors.leagueId}</FormErrorMessage>
                            </FormControl>
                          )}
                        </Field>
                      </Box>
                    )}

                    {/* Competition Id field: only show when a competition is selected */}
                    {['bb1', 'bb2', 'bb3'].includes(selectedGameType) && selectedCompetitionId && (
                      <Box>
                        <Field
                          name="competitionId"
                          validate={(value) => (value?.trim().length > 0 ? null : 'Competition id required.')}
                        >
                          {({ field, form }) => (
                            <FormControl isInvalid={form.errors.competitionId && form.touched.competitionId}>
                              <FormLabel>Competition Id</FormLabel>
                              <FormHelperText>Id of the Competition to add to Circuit</FormHelperText>
                              <Input {...field} placeholder="Competition Uuid" value={selectedCompetitionId || field.value} readOnly={!!selectedCompetitionId} />
                              <FormErrorMessage>{form.errors.competitionId}</FormErrorMessage>
                            </FormControl>
                          )}
                        </Field>
                      </Box>
                    )}
                  </Box>
                  {/* Competition Format: only show when a competition is selected */}
                  {selectedCompetitionId && (
                    <Box>
                      <Field name="competitionFormat">
                        {({ field, form }) => {
                          // BB1, BB2, BB3: readonly input
                          if (['bb1', 'bb2', 'bb3'].includes(selectedGameType)) {
                            return (
                              <FormControl>
                                <FormLabel>Competition Format</FormLabel>
                                <FormHelperText>
                                  The format of the selected competition (e.g., ladder, knockout, round-robin)
                                </FormHelperText>
                                <Input
                                  {...field}
                                  value={field.value || ''}
                                  placeholder="Competition format"
                                  readOnly
                                />
                              </FormControl>
                            );
                          }
                          // Other game types: dropdown
                          return (
                            <FormControl>
                              <FormLabel>Competition Format</FormLabel>
                              <FormHelperText>
                                Select the format of the competition
                              </FormHelperText>
                              <Select
                                {...field}
                                placeholder="Select format"
                                value={field.value || ''}
                                onChange={e => form.setFieldValue('competitionFormat', e.target.value)}
                              >
                                <option value="round-robin">Round robin</option>
                                <option value="wissen">Wissen</option>
                                <option value="knockout">Knockout</option>
                                <option value="ladder">Ladder</option>
                              </Select>
                            </FormControl>
                          );
                        }}
                      </Field>
                    </Box>
                  )}
                  <Box>
                    {['bb1', 'bb2', 'bb3'].includes(selectedGameType) && (
                      <Box>
                        <Field name="collectData" type="checkbox">
                          {({ field }) => (
                            <FormControl>
                              <FormLabel>Data collection</FormLabel>
                              <FormHelperText>Should this site collect data from the competition/league?</FormHelperText>
                              <Checkbox
                                {...field}
                                isChecked={field.value !== false} // checked by default
                                defaultChecked
                              >
                                Collect data?
                              </Checkbox>
                            </FormControl>
                          )}
                        </Field>
                      </Box>
                    )}
                  </Box>
                  
                  {/* Ladder specific: only show if the competition format is ladder */}
                  {['bb1', 'bb2', 'bb3'].includes(selectedGameType) && selectedCompetition?.format?.toLowerCase() === "ladder" && (
                    <Box>
                      <Field name="treatLadderAs">
                        {({ field, form }) => (
                          <FormControl isInvalid={form.errors.treatLadderAs && form.touched.treatLadderAs}>
                            <FormLabel>Ladder specific</FormLabel>
                            <FormHelperText>
                              How should ladder competitions be handled (this is only used if the format is ladder in cyanide database)?
                            </FormHelperText>
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
                  )}
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
            );
          }}
        </Formik>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default AdminCircuitPage;
