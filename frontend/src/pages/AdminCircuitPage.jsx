import React, { useEffect, useState } from 'react';
import {
  Box, Button, Card, CardBody, CardFooter, CardHeader, Checkbox, FormControl, FormErrorMessage,
  FormHelperText, FormLabel, Heading, HStack, Input, Select, SimpleGrid, Table, TableContainer,
  Tbody, Tfoot, Td, Th, Thead, Tr, VStack
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

// --- Constants and helpers ---

const bbVersions = [1, 2, 3];

const gameTypes = {
  bb1: { name: 'Blood Bowl 1', ruleset: ['LRB6'], platforms: ['PC'] },
  bb2: { name: 'Blood Bowl 2', ruleset: ['LRB6'], platforms: ['PC', 'Playstation', 'Xbox'] },
  bb3: { name: 'Blood Bowl 3', ruleset: ['BB2020'], platforms: ['PC', 'Playstation', 'Xbox', 'Switch'] },
  bloodbowl: { name: 'Blood Bowl', rulesets: ['LRB6', 'BB2016', 'BB2020', 'Other'], defaultRuleset: 'BB2020', platforms: ['Tabletop', 'Fumbbl', 'TTS', 'Other'] },
  sevens: { name: 'Blood Bowl 7s', rulesets: ['LRB6', 'BB2016', 'BB2020', 'Other'], defaultRuleset: 'BB2020', platforms: ['Tabletop', 'TTS'] },
  dungeonbowl: { name: 'Dungeon Bowl', ruleset: ['DB5', 'DB2021', 'Other'], defaultRuleset: 'DB2021', platforms: ['Tabletop', 'TTS'] },
  gutterbowl: { name: 'Gutter Bowl', ruleset: ['GB2023', 'Other'], platforms: ['Tabletop', 'TTS'] }
};

const treatLadderOptions = [
  { value: 'knockout', label: 'Treat as Knockout' },
  { value: 'round-robin', label: 'Treat as Round Robin League' },
  { value: 'ladder', label: 'Treat as Ladder' },
];

const initialFormValues = {
  leagueId: '',
  leagueName: '',
  competitionId: '',
  competitionName: '',
  legType: '',
  label: '',
  collectData: true,
  treatLadderAs: '',
  competitionFormat: '',
  opus: String(config.defaultOpus || 3),
};

// --- Helper functions ---

const bbVersionToGameTypeKey = (bbVersion) => {
  switch (String(bbVersion)) {
    case '1': return 'bb1';
    case '2': return 'bb2';
    case '3': return 'bb3';
    default: return 'bb3';
  }
};

const getPlatforms = (type) => gameTypes[type]?.platforms || [];
const getRulesets = (type) => gameTypes[type]?.rulesets || gameTypes[type]?.ruleset || [];
const getDefaultRuleset = (type) => {
  const gt = gameTypes[type];
  if (!gt) return '';
  if (gt.defaultRuleset) return gt.defaultRuleset;
  if (gt.rulesets && gt.rulesets.length > 0) return gt.rulesets[0];
  if (gt.ruleset && gt.ruleset.length > 0) return gt.ruleset[0];
  return '';
};
const getDefaultPlatform = (type) => {
  const gt = gameTypes[type];
  if (!gt) return '';
  if (gt.platforms && gt.platforms.length > 0) return gt.platforms[0];
  return '';
};

const compareLegs = (leg, otherLeg) => leg.label.localeCompare(otherLeg.label);

// --- Table columns as a separate component ---
function TableColumns() {
  return (
    <Tr>
      <Th />
      <Th>Label</Th>
      <Th>League</Th>
      <Th>Competition</Th>
      <Th>LegType</Th>
      <Th>Game</Th>
      <Th>Platform</Th>
      <Th>Ruleset</Th>
      <Th>Ladder</Th>
      <Th>Collect?</Th>
    </Tr>
  );
}

// --- Main component ---
function AdminCircuitPage() {
  // --- Auth and params ---
  const { isAuthenticated, isLoading, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const { circuitId } = useParams();

  // --- State ---
  const [bbVersion, setBbVersion] = useState(String(config.defaultOpus || 3));
  const [gameType, setGameType] = useState('Blood Bowl 3');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [circuit, setCircuit] = useState();
  const [searchResults, setSearchResults] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [selectedCompetitionId, setSelectedCompetitionId] = useState('');
  const [label, setLabel] = useState('');
  const [selectedLeague, setSelectedLeague] = useState(null);
  const [selectedCompetition, setSelectedCompetition] = useState(null);
  const [selectedGameType, setSelectedGameType] = useState('');
  const [selectedPlatform, setSelectedPlatform] = useState('');
  const [selectedRuleset, setSelectedRuleset] = useState('');

  // --- Effects ---
  useEffect(() => { fetchCircuit(circuitId); }, []);
  useEffect(() => {
    if (!selectedGameType) return;
    // Platform
    const platforms = getPlatforms(selectedGameType);
    setSelectedPlatform(platforms.length === 1 ? platforms[0] : getDefaultPlatform(selectedGameType));
    // Ruleset
    const rulesets = getRulesets(selectedGameType);
    setSelectedRuleset(rulesets.length === 1 ? rulesets[0] : getDefaultRuleset(selectedGameType));
  }, [selectedGameType]);

  // --- Handlers ---
  const handleCompetitionClick = (competition) => {
    setSelectedCompetitionId(competition.id || competition.uuid || competition.competitionId);
    setLabel(competition.name);
    
    console.log(searchResults);
    const compObj = (searchResults.competitions || []).find(
      c => (c.id || c.uuid || c.competitionId) === competition.id
    );

    /*
      The search results are just {id, name}
      How do I fetch the league Id if I don't know what league the competition belongs to?
      Should probably load all leagues in the search results and then find the league.
    */

    const name = competition.name || competition.leagueName || 'Unknown Competition';
    setSelectedCompetition(competition);
    const gameTypeKey = bbVersionToGameTypeKey(bbVersion);
    setSelectedGameType(gameTypeKey);
    setSelectedPlatform(getDefaultPlatform(gameTypeKey));
    setSelectedRuleset(getDefaultRuleset(gameTypeKey));
    console.log(compObj);
    if (competition.leagueId) {
      setSelectedLeagueId(competition.leagueId);
      WarpScoresApiService.leagues(competition.leagueId, bbVersion)
        .then((res) => setSelectedLeague(res))
        .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }));
    } else {
      WarpScoresApiService.competition(competition.id, bbVersion)
        .catch((reason1) => {
          if (reason1.response && reason1.response.status === 404) {
            for (const league of searchResults.leagues) {
              WarpScoresApiService.leagueCompetitions(league.id, bbVersion)
                .then((competitions) => {
                  const foundComp = competitions.find((comp) => comp.uuid === competition.id);
                  if (foundComp) {
                    setSelectedCompetition(foundComp);
                    setSelectedLeagueId(league.id);
                    setLabel(league.name + " - " + name);
                    setSelectedLeague(league);
                  }
                })
                .catch((reason2) => setError({ type: 'error', message: reason2.toLocaleString() }));
            }
          }
        });
    }
  };

  const handleLeagueClick = (league) => {
    setSelectedCompetitionId('');
    setSelectedLeagueId(league.id || league.leagueId || league.uuid);
    setLabel(league.name);
    const gameTypeKey = bbVersionToGameTypeKey(bbVersion);
    setSelectedGameType(gameTypeKey);
    setSelectedPlatform(getDefaultPlatform(gameTypeKey));
    setSelectedRuleset(getDefaultRuleset(gameTypeKey));
    WarpScoresApiService.leagues(league.id, bbVersion)
      .then((res) => setSelectedLeague(res))
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }));
  };

  const handleRemoveLeg = (circuitLegId) => {
    if (!window.confirm('Are you sure you want to remove this leg?')) return;
    WarpScoresApiService.removeCircuitLeg(circuit.circuitId, circuitLegId)
      .then(() => fetchCircuit(circuitId))
      .catch((err) => setError({ type: 'error', message: err.toLocaleString() }));
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
      .finally(() => setLoading(false));
  };

  const onAddLegClicked = (values, actions) => {
    WarpScoresApiService.addLegToCircuit(
      circuit.circuitId,
      values.leagueId,
      values.competitionId,
      values.legType,
      values.label,
      values.gameType,
      values.platform,
      values.ruleset,
      values.collectData,
      values.treatLadderAs,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(() => fetchCircuit(circuitId))
      .catch((err) => setError({ type: 'error', message: err.toLocaleString() }))
      .finally(() => {
        actions.setSubmitting(false);
      });
  };

  /*
  const onSearchClicked = (values, actions) => {
    setBbVersion(values.bbVersion);
    console.log('Searching for:', values.searchName, 'BB Version:', values.bbVersion);
    WarpScoresApiService.lookup({
      league_name: values.searchName,
      bb: values.bbVersion,
      exact: 1,
      fallback: true
    })
      .then((res) => setSearchResults(res))
      .catch((err) => setSearchResults([]))
      .finally(() => actions.setSubmitting(false));
  };

  const onSearchClicked = async (values, actions) => {
    setBbVersion(values.bbVersion);
    try {
      const res = await WarpScoresApiService.lookup({
        league_name: values.searchName,
        bb: values.bbVersion,
        exact: 1,
        fallback: false
      });
      // If leagues found, fetch details for each league in parallel
      if (res.leagues && res.leagues.length > 0) {
        const detailedLeagues = await Promise.all(
          res.leagues.map(async (league) => {
            try {
              const details = await WarpScoresApiService.leagues(league.id || league.leagueId || league.uuid, values.bbVersion);
              return { ...league, ...details };
            } catch (e) {
              // If fetch fails, just return the original league object
              return league;
            }
          })
        );
        setSearchResults({ ...res, leagues: detailedLeagues });
      } else {
        setSearchResults(res);
      }
    } catch (err) {
      setSearchResults([]);
    } finally {
      actions.setSubmitting(false);
    }
  };
  */

  const onSearchClicked = async (values, actions) => {
    setBbVersion(values.bbVersion);
    try {
      const res = await WarpScoresApiService.lookup({
        league_name: values.searchName,
        bb: values.bbVersion,
        exact: 1,
        fallback: false
      });

      let detailedLeagues = [];
      if (res.leagues && res.leagues.length > 0) {
        detailedLeagues = await Promise.all(
          res.leagues.map(async (league) => {
            try {
              const details = await WarpScoresApiService.leagues(
                league.id || league.leagueId || league.uuid,
                values.bbVersion
              );
              // Fetch competitions for this league
              let competitions = [];
              try {
                competitions = await WarpScoresApiService.leagueCompetitions(
                  league.id || league.leagueId || league.uuid,
                  values.bbVersion
                );
              } catch (e) {
                competitions = [];
              }
              return { ...league, ...details, competitions };
            } catch (e) {
              return league;
            }
          })
        );
      }

      // Expand competitions with detailed objects from leagues
      let expandedCompetitions = res.competitions || [];
      if (expandedCompetitions.length > 0 && detailedLeagues.length > 0) {
        expandedCompetitions = expandedCompetitions.map((comp) => {
          let detailedComp = null;
          for (const league of detailedLeagues) {
            if (league.competitions) {
              detailedComp = league.competitions.find(
                c =>
                  (c.id || c.uuid || c.competitionId) === (comp.id || comp.uuid || comp.competitionId)
              );
              if (detailedComp) break;
            }
          }
          return detailedComp || comp;
        });
      }

      setSearchResults({
        ...res,
        leagues: detailedLeagues,
        competitions: expandedCompetitions,
      });
    } catch (err) {
      setSearchResults([]);
    } finally {
      actions.setSubmitting(false);
    }
  };

  // --- Render ---
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
                <CircuitLeg
                  key={circuitLeg.circuitLegId}
                  circuitLeg={circuitLeg}
                  onRemoveLeg={handleRemoveLeg}
                />
              ))}
            </Tbody>
            <Tfoot>
              <TableColumns />
            </Tfoot>
          </Table>
        </TableContainer>

        {/* Search Form */}
        <Heading size="md">Add Leg</Heading>
        <Box mb="1rem">
          A Circuit Leg is either a competition or a league (with all its competitions), specified by leg type.
          You may add a custom label and define if data from Cyanide API should be collected periodically.
          If you select "treat Ladder as Knockout", all competitions of type ladder will be rendered as if they were knockout tournaments.
        </Box>
        <Checkbox isChecked={isAuthenticated}>Auth</Checkbox>
        <HStack>
          <Formik
            initialValues={{ searchName: '', bbVersion: bbVersion }}
            onSubmit={onSearchClicked}
          >
            {(props) => (
              <Card as={Form} variant="outline" size="sm">
                <CardHeader>Search for id</CardHeader>
                <SimpleGrid as={CardBody} columns={{ base: 1, md: 2, xl: 3 }} gap="1rem">
                  <Box>
                    <Field
                      name="searchName"
                      validate={(value) => (value?.trim().length > 0 ? null : 'Competition or League name required.')}
                    >
                      {({ field, form }) => (
                        <FormControl isInvalid={form.errors.searchName && form.touched.searchName}>
                          <FormLabel>Name</FormLabel>
                          <FormHelperText>Name of the Competition or League to search for</FormHelperText>
                          <Input {...field} placeholder="Competition/League name" />
                          <FormErrorMessage>{form.errors.searchName}</FormErrorMessage>
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
                                onClick={() => handleLeagueClick(item)}>
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
                                onClick={() => handleCompetitionClick(item)}>
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
            leagueId: selectedLeagueId || '',
            competitionId: selectedCompetitionId || '',
            label: label || '',
            gameType: selectedGameType,
            platform: selectedPlatform,
            ruleset: selectedRuleset,
          }}
          enableReinitialize
          onSubmit={onAddLegClicked}
        >
          {(props) => {
            // Sync Formik fields with state when state changes
            useEffect(() => {
              props.setFieldValue('gameType', selectedGameType);
              props.setFieldValue('platform', selectedPlatform);
              props.setFieldValue('ruleset', selectedRuleset);
            }, [selectedGameType, selectedPlatform, selectedRuleset]);
            useEffect(() => {
              props.setFieldValue('competitionId', selectedCompetitionId || '');
              props.setFieldValue('label', label || '');
            }, [selectedCompetitionId, label]);
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
                            {Object.keys(gameTypes).map(key => (
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
                        if (selectedLeagueId) {
                          props.setFieldValue('leagueId', selectedLeagueId);
                          props.setFieldValue('legType', 'League');
                        }
                        if (selectedCompetitionId) {
                          props.setFieldValue('competitionId', selectedCompetitionId);                        
                          props.setFieldValue('legType', 'Competition');
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
