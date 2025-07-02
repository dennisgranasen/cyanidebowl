
import React, { useEffect, useState, useRef } from 'react';
import {
  Box, Button, Card, CardBody, CardFooter, CardHeader, Checkbox, FormControl, FormErrorMessage,
  FormHelperText, FormLabel, Heading, HStack, Input, Select, SimpleGrid
} from '@chakra-ui/react';
import { Field, Form, Formik } from 'formik';
import {bbVersions } from '../../util/bbVersions.js';
import WarpScoresApiService from '../../WarpScoresApiService';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import config from '../../config';



function EntitySearchForm({ handleLeagueClick, handleCompetitionClick }) {
    const [bbVersion, setBbVersion] = useState(String(config.defaultOpus || 3));
    const { isAuthenticated, isLoading, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
    const [loading, setLoading] = useState(true);
    const [lines, setLines] = useState([]);
    const leagueRefs = useRef({});
    const competitionRefs = useRef({});
    const parentRef = useRef(null);
    const [leagueOffsets, setLeagueOffsets] = useState({});
    const [searchResults, setSearchResults] = useState([]);
    
  useEffect(() => {
    if (!searchResults.leagueDetails || !searchResults.competitionDetails) return;
    const newOffsets = {};
    let accumulatedOffset = 0;
    searchResults.leagueDetails.forEach(league => {
      const leagueId = league.id;
      const leagueEl = leagueRefs.current[leagueId.key];
      if (!leagueEl) {
        newOffsets[leagueId.key] = 0;
        return;
      }
      // Get the league's natural top (relative to the container)
      const leagueRect = leagueEl.getBoundingClientRect();
      
      // Find all competitions for this league
      const comps = searchResults.competitionDetails.filter(c => c.leagueId?.value === leagueId.value);

      const compEls = comps
        .map(c => competitionRefs.current[c.id.key])
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
        newOffsets[leagueId.key] = offset;
        accumulatedOffset += offset;
      } else {
        newOffsets[leagueId.key] = 0;
      }
    });
    setLeagueOffsets(newOffsets);
  }, [searchResults.leagueDetails, searchResults.competitionDetails]);

  useEffect(() => {
    const timeout = setTimeout(() => {
      const newLines = [];
      if (!searchResults.leagueDetails || !searchResults.competitionDetails) return;

      searchResults.leagueDetails.forEach(league => {
        const leagueId = league.id.value;
        const leagueEl = leagueRefs.current[league.id.key];
        if (!leagueEl) return;

        // Use offsetTop relative to the parent container, plus marginTop
        const marginTop = parseFloat(leagueEl.style.marginTop || 0);
        const leagueCenterY = leagueEl.offsetTop + marginTop + leagueEl.offsetHeight / 2;
        const leagueRightX = leagueEl.offsetLeft + leagueEl.offsetWidth;

        searchResults.competitionDetails
          .filter(comp => comp.leagueId?.value === leagueId)
          .forEach(comp => {

            const compKey = comp.id.key;
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

  const myHandleCompetitionClick = (item) => {
    console.log('Competition clicked:', item);
    
    if (!item || !item.id || !item.id.value) {
      console.warn('Invalid competition item clicked:', item);
      return;
    }
    WarpScoresApiService.latestCompetitionMatches()
        .then((matches) => {
            console.log('Latest matches for competition:', item.id.value, matches);
        })
        .catch((err) => {
        });
    
    if (handleCompetitionClick) {
      handleCompetitionClick(item);
    }
  };

  const onSearchClicked = async (values, actions) => {
    try {
      const res = await WarpScoresApiService.lookup({
        league_name: values.searchName,
        opus: values.bbVersion,
        exact: values.exact ? 1 : 0,
        hint: 'HAS_CONTESTS',
        fallback: 0,
        includeDetails: true
      });

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


    return (
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
                  const leagueId = item.id.key
                  return (
                    <Box 
                        key={leagueId}
                        className='league-row'
                        ref={(el) => {leagueRefs.current[leagueId] = el;}}                        
                        _hover={{ bg: 'gray.100', opacity: 0.8,  color: 'black', cursor: 'pointer' }}
                        style={{
                            marginTop: leagueOffsets[leagueId] || 0,
                        }}
                        onClick={() => handleLeagueClick && handleLeagueClick(item)}>
                      {item.name || item.leagueName}
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
                        key={item.id.key}
                        className="competition-row"
                        ref={el => {competitionRefs.current[item.id.key] = el;}}
                        _hover={isDetailed ? { bg: 'gray.100', opacity: 0.8, color: 'black', cursor: 'pointer' } : undefined}
                        style={isDetailed ? {} : { color: 'red', cursor: 'not-allowed' }}
                        onClick={isDetailed ? () => myHandleCompetitionClick(item) : undefined}
                      >
                        {item.name || item.leagueName}
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
    </HStack>);
}

export default EntitySearchForm;