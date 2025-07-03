import React, { useEffect, useState, useRef } from 'react';
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
import EntitySearchForm from '../components/common/EntitySearchForm';
import prettyPrint from '../util/prettyPrint';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import config from '../config';
import gameTypes from '../util/gameTypes.js';
import ladderOptions from '../util/ladderOptions.js';
import entityTypes from '../util/entityTypes.js';
import {bbVersions, getGameFromOpus, getOpusFromGame } from '../util/bbVersions.js';
import { identityUtils } from '../util/identityUtil.jsx';
// --- Constants and helpers ---

const initialFormValues = {
  leagueId: '',
  leagueName: '',
  competitionId: '',
  competitionName: '',
  legType: '',
  label: '',
  isCollected: true,
  isArchived: false,
  ladderOption: '',
  competitionFormat: '',
  opus: String(config.defaultOpus || 3),
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
      <Th>Id</Th>
      <Th>LegType</Th>
      <Th>Game</Th>
      <Th>Platform</Th>
      <Th>Ruleset</Th>
      <Th>Ladder</Th>
      <Th>Collect?</Th>
      <Th>Archived?</Th>
    </Tr>
  );
}

// --- Main component ---
function AdminCircuitPage() {

  // --- Auth and params ---
  const { isAuthenticated, isLoading, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const { circuitId } = useParams();
  const [bbVersion, setBbVersion] = useState(String(config.defaultOpus || 3));

  // --- State ---
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState();
  const [circuit, setCircuit] = useState();
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [selectedCompetitionId, setSelectedCompetitionId] = useState('');
  const [label, setLabel] = useState('');
  const [selectedLeague, setSelectedLeague] = useState(null);
  const [selectedCompetition, setSelectedCompetition] = useState(null);
  const [selectedCompetitionFormat, setSelectedCompetitionFormat] = useState('');
  const [selectedGameType, setSelectedGameType] = useState('');
  const [selectedPlatform, setSelectedPlatform] = useState('');
  const [selectedRuleset, setSelectedRuleset] = useState('');
  //const [isCollected, setIsCollected] = useState(initialFormValues.isCollected);
  //const [isArchived, setIsArchived] = useState(initialFormValues.isArchived);

  // --- Effects ---
  useEffect(() => { fetchCircuit(circuitId); }, []);
/*
    // ...inside Formik render function...
  useEffect(() => {
    // Map selectedGameType to opus value
    let newOpus = '3';
    if (selectedGameType === 'bb1') newOpus = '1';
    else if (selectedGameType === 'bb2') newOpus = '2';
    else if (selectedGameType === 'bb3') newOpus = '3';
    props.setFieldValue('opus', newOpus);
  }, [selectedGameType]);
*/
  useEffect(() => {
    if (!selectedGameType) return;
    // Platform
    const platforms = getPlatforms(selectedGameType);
    setSelectedPlatform(platforms.length === 1 ? platforms[0] : getDefaultPlatform(selectedGameType));
    // Ruleset
    const rulesets = getRulesets(selectedGameType);
    setSelectedRuleset(rulesets.length === 1 ? rulesets[0] : getDefaultRuleset(selectedGameType));

    //const opus = getOpus(selectedGameType);
    console.log('Setting BB version based on game type:', selectedGameType);
    setBbVersion(selectedGameType);

  }, [selectedGameType]);



  // --- Handlers ---
  const handleCompetitionClick = (competition) => {
    console.log('Competition clicked:', competition);
    setSelectedCompetitionId(competition.id.parts[1]);
    setSelectedLeagueId(competition.id.parts[0]);
    setLabel(competition.name);
    
    const name = competition.name || competition.leagueName || 'Unknown Competition';
    setSelectedCompetition(competition);
    const opus = competition.id.opus
    const gameTypeKey = getGameFromOpus(opus).toLowerCase();
    setSelectedGameType(gameTypeKey);
    setSelectedPlatform(getDefaultPlatform(gameTypeKey));
    setSelectedRuleset(getDefaultRuleset(gameTypeKey));
    setSelectedCompetitionFormat(competition.format || compObj?.format || '');
  };

  const handleLeagueClick = (league) => {
    console.log('League clicked:', league);
    setSelectedCompetitionId('');
    setSelectedLeagueId(league.id.value);
    setLabel(league.name);
    const gameTypeKey = getGameFromOpus(league.id.opus).toLowerCase();
    setSelectedGameType(gameTypeKey);
    setSelectedPlatform(getDefaultPlatform(gameTypeKey));
    setSelectedRuleset(getDefaultRuleset(gameTypeKey));
    console.log('Fetching league 155:', league.leagueId, league.id.opus);
    if ((league.id == null || league.id === '') && league.leagueId == undefined) 
      console.log('League has no ID, cannot fetch details:', league);
    WarpScoresApiService.leagues(league.id)
      .then((res) => setSelectedLeague(res))
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }));
  };

  const handleRemoveLeg = (circuitLegId) => {
    if (!window.confirm('Are you sure you want to remove this leg?')) return;
    WarpScoresApiService.removeCircuitLeg(circuit.circuitId, circuitLegId)
      .then(() => fetchCircuit(circuitId))
      .catch((err) => setError({ type: 'error', message: err.toLocaleString() }));
  };

  const handleCircuitLegIsCollectedChanged = (circuitLegId, isChecked) => {
    WarpScoresApiService.updateCircuitLeg(
      circuit.circuitId,
      circuitLegId,
      {isCollected: isChecked},
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(() => fetchCircuit(circuitId))
      .catch((err) => setError({ type: 'error', message: err.toLocaleString() }));      
  };

  const handleCircuitLegIsArchivedChanged = (circuitLegId, isChecked) => {
    WarpScoresApiService.updateCircuitLeg(
      circuit.circuitId,
      circuitLegId,
      {isArchived: isChecked},
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
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
    console.log('Adding leg with values:', values);
    WarpScoresApiService.addLegToCircuit(
      circuit.circuitId,
      values.leagueId,
      values.competitionId,
      values.competitionId ? entityTypes.competition : entityTypes.league,
      values.label,
      values.gameType,
      values.platform,
      values.ruleset,
      values.isCollected,
      values.isArchived,
      values.ladderOption,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(() => fetchCircuit(circuitId))
      .catch((err) => setError({ type: 'error', message: err.toLocaleString() }))
      .finally(() => {
        actions.setSubmitting(false);
      });
  };

  const handleSearchResultDragStart = (item) => {
    console.log('Search result drag started:', item);
    if (item.competitionId) {
      handleCompetitionClick(item);
    } else if (item.leagueId) {
      handleLeagueClick(item);
    } else {
      console.warn('Unknown item type for drag start:', item.type);
    }
  };  

  const handleAddEntityToLeg = (circuitLegId, entityData) => {
    console.log('Adding entity to leg:', circuitLegId, entityData);
    WarpScoresApiService.addEntityToCircuitLeg(
      circuit.circuitId,
      circuitLegId,
      {
        entityId: identityUtils.key(entityData.id),
        entityType: entityData.type,
        name: entityData.name,
        game: entityData.gameType || selectedGameType,
        platform: entityData.platform || selectedPlatform,
        ruleset: entityData.ruleset || selectedRuleset,
        ladderOption: entityData.ladderOption || ''
      },
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(() => fetchCircuit(circuitId))
      .catch((err) => setError({ type: 'error', message: err.toLocaleString() }));
  };

  const handleGameTypeChange = (e) => { 
    const gameType = e.target.value;
    console.log('Game type changed:', gameType);
    setSelectedGameType(gameType);

        // Set platform and ruleset automatically
    const platforms = getPlatforms(gameType);
    const rulesets = getRulesets(gameType);

    setSelectedPlatform(getDefaultPlatform(gameType));
    setSelectedRuleset(getDefaultRuleset(gameType));
    setSelectedCompetitionFormat('');
    setSelectedLeagueId('');
    setSelectedCompetitionId('');
    setLabel('');
    
    // Reset league and competition selections
    setSelectedLeague(null);
    setSelectedCompetition(null);

    let platform = '';
    if (platforms.length === 1) {
      platform = platforms[0];
    } else if (platforms.length > 1) {
      platform = getDefaultPlatform(gameType);
    }
    setSelectedPlatform(platform);
    //props.setFieldValue('platform', platform);

    let ruleset = '';
    if (rulesets.length === 1) {
      ruleset = rulesets[0];
    } else if (rulesets.length > 1) {
      ruleset = getDefaultRuleset(gameType);
    }
    setSelectedRuleset(ruleset);
    //props.setFieldValue('ruleset', ruleset);

    //props.setFieldValue('gameType', gameType);
  }

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
                  onCollectDataChanged={handleCircuitLegIsCollectedChanged}
                  onArchivedChanged={handleCircuitLegIsArchivedChanged}
                  onRemoveLeg={handleRemoveLeg}
                  onAddEntityToLeg={handleAddEntityToLeg}
                />
              ))}
            </Tbody>
            <Tfoot>
              <TableColumns />
            </Tfoot>
          </Table>
        </TableContainer>

        <EntitySearchForm
          handleCompetitionClick={handleCompetitionClick} 
          handleLeagueClick={handleLeagueClick}
          onSearchResultDragStart={handleSearchResultDragStart}
        />
        
        <Heading size="md">Add Leg</Heading>
        <Box mb="1rem">
          A Circuit Leg is either a competition or a league (with all its competitions), specified by leg type.
          You may add a custom label and define if data from Cyanide API should be collected periodically.
          If you select "treat Ladder as Knockout", all competitions of type ladder will be rendered as if they were knockout tournaments.
        </Box>        

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
            competitionFormat: selectedCompetitionFormat || '',
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
            /*
            useEffect(() => {
              if (selectedCompetition && selectedCompetition.format) {
                props.setFieldValue('competitionFormat', selectedCompetition.format);
              } else {
                props.setFieldValue('competitionFormat', '');
              }
            }, [selectedCompetition]);
            */
            return (
              <Card as={Form} variant="outline" size="sm">
                <CardHeader>New Leg</CardHeader>
                <SimpleGrid as={CardBody} columns={{ base: 1, md: 2, xl: 3 }} gap="1rem">
                  {/* Label */}
                  <Box>
                    <Field name="label" validate={value => value ? null : 'Label is required'}>
                      {({ field, form }) => (
                        <FormControl isInvalid={form.errors.label && form.touched.label}>
                          <FormLabel>Label</FormLabel>
                          <Input {...field} value={props.values.label} placeholder="Enter label" />
                          <FormErrorMessage>{form.errors.label}</FormErrorMessage>
                        </FormControl>
                      )}
                    </Field>
                  </Box>
                  {/* Game Type */}
                  <Box>
                    <Field name="gameType" validate={value => value ? null : 'Select game type'}>
                      {({ field, form }) => (
                        <FormControl isInvalid={form.errors.gameType && form.touched.gameType}>
                          <FormLabel>Game Type</FormLabel>
                          <Select
                            {...field}
                            placeholder="Select game type"
                            value={selectedGameType}
                            onChange={handleGameTypeChange}
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
                    {/* League Id field: only show for BB1, BB2, BB3 */}
                    {['bb1', 'bb2', 'bb3'].includes(selectedGameType) && (
                      <Box>
                        <Field
                          name="leagueId"
                          validate={(value) => (value.length > 0 ? null : 'League id required.')}
                        >
                          {({ field, form }) => (
                            <FormControl isInvalid={form.errors.leagueId && form.touched.leagueId}>
                              <FormLabel>League Id</FormLabel>
                              <FormHelperText>Id of the League to add to Circuit</FormHelperText>
                              <Input {...field} placeholder="League Id" value={selectedLeagueId || field.value?.leagueId?.value} readOnly={!!selectedLeagueId} />
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
                        <Field name="isCollected" type="checkbox">
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
                  {['bb1', 'bb2', 'bb3'].includes(selectedGameType) && selectedCompetitionFormat.toLowerCase() === "ladder" && (
                    <Box>
                      <Field name="ladderOption" validate={(value) => (value ? null : 'Select ladder option')}>
                        {({ field, form }) => (
                          <FormControl isInvalid={form.errors.treatLadderAs && form.touched.treatLadderAs}>
                            <FormLabel>Ladder specific</FormLabel>
                            <FormHelperText>
                              How should ladder competitions be handled (this is only used if the format is ladder in cyanide database)?
                            </FormHelperText>
                            <Select {...field} placeholder="Select option">
                              {ladderOptions.map((option) => (
                                <option value={option.value} key={option.value}>
                                  Treat as {option.label}
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
