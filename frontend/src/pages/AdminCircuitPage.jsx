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
  isCollected: true,
  isArchived: false,
  ladderOption: '',
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
      <Th>Archived?</Th>
    </Tr>
  );
}

// --- Main component ---
function AdminCircuitPage() {

  const leagueRefs = useRef({});
  const competitionRefs = useRef({});
  const parentRef = useRef(null);
  const [leagueOffsets, setLeagueOffsets] = useState({});

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
  const [selectedCompetitionFormat, setSelectedCompetitionFormat] = useState('');
  const [selectedGameType, setSelectedGameType] = useState('');
  const [selectedPlatform, setSelectedPlatform] = useState('');
  const [selectedRuleset, setSelectedRuleset] = useState('');
  //const [isCollected, setIsCollected] = useState(initialFormValues.isCollected);
  //const [isArchived, setIsArchived] = useState(initialFormValues.isArchived);

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


  useEffect(() => {
    if (!searchResults.leagueDetails || !searchResults.competitionDetails) return;
    const newOffsets = {};
    let accumulatedOffset = 0;
    searchResults.leagueDetails.forEach(league => {
      const leagueId = league.leagueId.toString();
      const leagueEl = leagueRefs.current[leagueId];
      if (!leagueEl) {
        newOffsets[leagueId] = 0;
        return;
      }
      // Get the league's natural top (relative to the container)
      const leagueRect = leagueEl.getBoundingClientRect();
      
      // Find all competitions for this league
      const comps = searchResults.competitionDetails.filter(c => c.leagueId?.value === leagueId);
      console.log(`Calculating offset for league ${league.name} (${leagueId}) with ${comps.length} competitions`);

      const compEls = comps
        .map(c => competitionRefs.current[c.identity.value])
        .filter(Boolean);
      if (compEls.length) {
        // Get bounding rects
        const compRects = compEls.map(el => el.getBoundingClientRect());
        
        // Calculate the vertical center of the group
        const top = compRects[0].top;
        const bottom = compRects[compRects.length - 1].bottom;
        const center = (top + bottom) / 2;
        const leagueCenter = leagueRect.top + leagueRect.height / 2;
        // Calculate offset relative to natural position, minus accumulated offset
        const offset = center - leagueCenter - accumulatedOffset;
        newOffsets[leagueId] = offset;
        accumulatedOffset += offset;
      } else {
        newOffsets[leagueId] = 0;
      }
    });
    setLeagueOffsets(newOffsets);
  }, [searchResults.leagueDetails, searchResults.competitionDetails]);

  useEffect(() => {
    const timeout = setTimeout(() => {
      const newLines = [];
      if (!searchResults.leagueDetails || !searchResults.competitionDetails) return;

      searchResults.leagueDetails.forEach(league => {
        const leagueId = league.leagueId.toString();
        const leagueEl = leagueRefs.current[leagueId];
        if (!leagueEl) return;

        // Use offsetTop relative to the parent container, plus marginTop
        const marginTop = parseFloat(leagueEl.style.marginTop || 0);
        const leagueCenterY = leagueEl.offsetTop + marginTop + leagueEl.offsetHeight / 2;
        const leagueRightX = leagueEl.offsetLeft + leagueEl.offsetWidth;

        searchResults.competitionDetails
          .filter(comp => comp.leagueId?.value === leagueId)
          .forEach(comp => {

            const compKey = comp.identity?.value?.toString();
            const compEl = competitionRefs.current[compKey];
            if (!compEl) return;

            const parentRect = parentRef.current.getBoundingClientRect();
            const leagueRect = leagueEl.getBoundingClientRect();
            const compRect = compEl.getBoundingClientRect();

            const leagueCenterY = leagueRect.top + leagueRect.height / 2 - parentRect.top;
            const leagueRightX = leagueRect.right - parentRect.left;
            const compCenterY = compRect.top + compRect.height / 2 - parentRect.top;
            const compLeftX = compRect.left - parentRect.left;

            newLines.push({
              x1: leagueRightX + 2,
              y1: leagueCenterY,
              x2: compLeftX - 4,
              y2: compCenterY,
            });
            /*
            const compKey = comp.identity?.value?.toString();
            const compEl = competitionRefs.current[compKey];
            if (!compEl) return;
            const compCenterY = compEl.offsetTop + compEl.offsetHeight / 2;
            const compLeftX = compEl.offsetLeft;

            newLines.push({
              x1: leagueRightX,
              y1: leagueCenterY,
              x2: compLeftX,
              y2: compCenterY,
          });*/
          
        });
      });

      setLines(newLines);
    }, 0);
    return () => clearTimeout(timeout);
  }, [searchResults.leagueDetails, searchResults.competitionDetails, leagueOffsets]);
      


  // --- Handlers ---
  const handleCompetitionClick = (competition) => {
    setSelectedCompetitionId(competition.id || competition.uuid || competition.competitionId);
    setLabel(competition.name);
    
    const compObj = (searchResults.competitions || []).find(
      c => (c.id || c.uuid || c.competitionId) === competition.id
    );

    const name = competition.name || competition.leagueName || 'Unknown Competition';
    setSelectedCompetition(competition);
    const gameTypeKey = bbVersionToGameTypeKey(bbVersion);
    setSelectedGameType(gameTypeKey);
    setSelectedPlatform(getDefaultPlatform(gameTypeKey));
    setSelectedRuleset(getDefaultRuleset(gameTypeKey));
    setSelectedCompetitionFormat(competition.format || compObj?.format || '');
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
              console.log('Checking league:', league.name, league.id, bbVersion);
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
    WarpScoresApiService.addLegToCircuit(
      circuit.circuitId,
      values.leagueId,
      values.competitionId,
      values.legType,
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

  const [lines, setLines] = useState([]);


  const onSearchClicked = async (values, actions) => {
    setBbVersion(values.bbVersion);
    try {
      const res = await WarpScoresApiService.lookup({
        league_name: values.searchName,
        opus: values.bbVersion,
        exact: values.exact ? 1 : 0,
        hint: 'HAS_CONTESTS',
        fallback: 0,
        includeDetails: true
      });

      /*
      if (res.leagues && res.leagues.length > 0) {
        if (res.leagueDetails && res.leagueDetails.length > 0) {

        }
      }
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
                )
                console.log('Fetched competitions for league:', league.name, competitions);
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
                c => (c.id || c.uuid || c.competitionId) === comp.id
              );
              if (detailedComp) break;
            }
          }
          return detailedComp || comp;
        });
      }
      */

      console.log("search: {}", {
        ...res,
        leagueDetails: res.leagueDetails || res.leagues || [],
        competitionDetails: res.competitionDetails || res.competitions || [] // expandedCompetitions,
      });

      setSearchResults({
        ...res,
        leagueDetails: res.leagueDetails || res.leagues || [],
        competitionDetails: res.competitionDetails || res.competitions || [] // expandedCompetitions,
      });
/*
      console.log('Search results:', {
        ...res,
        leagues: detailedLeagues,
        competitions: expandedCompetitions,
      });
*/

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
                  onCollectDataChanged={handleCircuitLegIsCollectedChanged}
                  onArchivedChanged={handleCircuitLegIsArchivedChanged}
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
        <HStack>
          <Formik
            initialValues={{ searchName: '', bbVersion: bbVersion, exact: true }}
            onSubmit={onSearchClicked}
          >
            {(props) => (
              <Card as={Form} variant="outline" size="sm" maxW="md">
                <CardHeader>Search for id</CardHeader>
                <SimpleGrid as={CardBody} columns={1} gap="1rem">
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
                    <Field name="exact" type="checkbox">
                      {({ field, form }) => (
                        <FormControl>
                          <Checkbox
                            {...field}
                            isChecked={field.value}
                            onChange={e => {
                              if (!e.target.checked) {
                                const confirmed = window.confirm(
                                  "Disabling exact search may return a large number of results and could be VERY slow. Are you sure you want to continue?"
                                );
                                if (!confirmed) {
                                  // Prevent unchecking
                                  return;
                                }
                              }
                              form.setFieldValue('exact', e.target.checked);
                            }}
                          >
                            Exact search
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
          <Box ref={parentRef} display="flex" flexDirection="row" alignItems="flex-start" gap="4rem" position="relative">
            {searchResults.leagueDetails && searchResults.leagueDetails.length > 0 && (
              <Box mt={4}>
                <Heading size="sm" mb={2}>Leagues</Heading>
                {searchResults.leagueDetails.map((item) => {
                  console.log(item);
                  const leagueId = item.leagueId?.toString()
                  return (
                    <Box 
                        key={leagueId}
                        className='league-row'
                        ref={(el) => {leagueRefs.current[leagueId] = el;}}                        
                        _hover={{ bg: 'gray.100', opacity: 0.8,  color: 'black', cursor: 'pointer' }}
                        style={{
                            //position: "absolute",
                            //top: leaguePositions[leagueId] ? leaguePositions[leagueId] - 12 : undefined, // 12 = half league row height
                            marginTop: leagueOffsets[leagueId] || 0,
                            //left: ,
                            //width: "200px",
                            //height: "24px"

                        }}
                        onClick={() => handleLeagueClick(item)}>
                      {item.name || item.leagueName} ({item.id || item.uuid || item.leagueId})
                    </Box>
                  )})}
              </Box>
            )}
            {searchResults.competitionDetails && searchResults.competitionDetails.length > 0 && (
              <Box mt={4}>
                <Heading size="sm" mb={2}>Competitions</Heading>
                {searchResults.competitionDetails.map((item) => {
                  // Check if this competition is detailed (has more than just id/name)
                  const isDetailed = !!(item.format ||  item.leagueId || item.identity || item.status || item.teams || item.rounds);
                  return (
                    <Box 
                        key={item.id || item.uuid || item.competitionId}
                        className="competition-row"
                        ref={el => {competitionRefs.current[item.identity?.value?.toString()] = el;}}
                        _hover={isDetailed ? { bg: 'gray.100', opacity: 0.8, color: 'black', cursor: 'pointer' } : undefined}
                        style={isDetailed ? {} : { color: 'red', cursor: 'not-allowed' }}
                        onClick={isDetailed ? () => handleCompetitionClick(item) : undefined}
                      >
                        {item.name || item.leagueName} ({item.id || item.uuid || item.competitionId})
                    </Box>
                  );
                })}
              </Box>
          )}
                <svg
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          pointerEvents: "none",
          width: "100%",
          height: "100%",
          zIndex: 10,
        }}
      >
        {lines.map((line, idx) => (
          <line
            key={idx}
            x1={line.x1}
            y1={line.y1}
            x2={line.x2}
            y2={line.y2}
            stroke="pink"
            strokeWidth={1.2}
            strokeDasharray="5,3"
          />
        ))}
      </svg>
          </Box>
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
