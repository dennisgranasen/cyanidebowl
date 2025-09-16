import React, { useState, useEffect } from 'react';
import { Checkbox, Td, Tr, Button, Input, IconButton, Box, Modal, ModalOverlay, ModalContent, ModalHeader, ModalBody, ModalFooter, ModalCloseButton, useDisclosure, FormControl, FormLabel, Spinner, List, ListItem, ButtonGroup } from '@chakra-ui/react';
import { EditIcon, CheckIcon, CloseIcon, ChevronDownIcon, ChevronRightIcon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import MatchSelectionModal from './ModalMatchSelection';
import { useNavigate } from 'react-router-dom';

function CircuitLeg({circuitId, circuitLeg}) {
  const [expanded, setExpanded] = useState(false);
  const navigate = useNavigate();

  // Modal state
  const [selectedEntity, setSelectedEntity] = useState(null);
  const { isOpen, onOpen, onClose } = useDisclosure();

  const [allMatches, setAllMatches] = useState([]);
  const [loadingMatches, setLoadingMatches] = useState(false);

  const gotoCircuitLeg = (circuitId, legId) => {
      navigate(`/circuit/${circuitId}/leg/${legId}`);
  };

  const gotoCircuitLegEntity = (circuitId, legId, index) => {
      console.log("Navigating to entity:", circuitId, legId, index);
      navigate(`/circuit/${circuitId}/leg/${legId}/${index}`);
  };
    
  const handleEntityClick = async (entity) => {
    setSelectedEntity(entity);
    setLoadingMatches(true);
    //const response = await fetch(`/api/legs/${circuitLeg.circuitLegId}/entities/${entity.entityId}/matches`);
    //const matches = await response.json();
    console.log("Fetching matches for entity:", entity);
    let fetchMatches = () => {
      console.log("Fetching matches for legType:", entity.legType);
      switch (entity.legType) {
        case 'Competition':
          return WarpScoresApiService.competitionMatches(entity.entityId);
        case 'League':
          return WarpScoresApiService.leagueMatches(entity.entityId);
        default:
          return Promise.resolve([]);
    }};
    fetchMatches().then(matches => {
      if (!Array.isArray(matches)) {
        throw new Error('Invalid matches data');
      }
      console.log("Fetched matches:", matches);
      let m = matches.map(m => ({
        id: m.id,
        name: m.name || m.id,
        date: m.date,
      }));
      setAllMatches(m);
    })
    .catch(() => {
      setAllMatches([])
    })
    .finally(() => {
      setLoadingMatches(false);
      onOpen();
    });
  };


  const renderRow = (entity, idx) => (
    <Tr
      key={`${circuitLeg.circuitLegId}-${idx}`}
      onClick={() => {
        if (circuitLeg.entities?.length > 1 && expanded) {
          gotoCircuitLegEntity(circuitId, circuitLeg.circuitLegId, entity.entityId.key);
        } else {
          gotoCircuitLeg(circuitId, circuitLeg.circuitLegId);
        }
      }}
      style={{ cursor: 'pointer' }}
    >
      <Td p={0} m={0} width="1%" onClick={e => e.stopPropagation()}>
        {idx === 0 && (circuitLeg.entities?.length > 1) ? (
          <IconButton
            icon={expanded ? <ChevronDownIcon /> : <ChevronRightIcon />}
            size="sm"
            variant="ghost"
            aria-label={expanded ? "Collapse" : "Expand"}
            onClick={e => { e.stopPropagation(); setExpanded(ex => !ex); }}
            m={0}
            p={0}
          />
        ) : (
          <Box width="32px" height="32px" />
        )}
      </Td>
      <Td>
        {circuitLeg.label}
      </Td>
      <Td>{entity.entityNames && entity.entityNames[0] || ''}</Td>
      <Td>{entity.entityNames ? (entity.entityNames.length > 1 ? entity.entityNames[1] : '*') : ''}</Td>
      <Td>{entity.legType}</Td>
      <Td>{entity.game}</Td>
      <Td>{entity.platform}</Td>
      <Td>{entity.ruleset}</Td>
      {/* Conditionally render Ladder column */}
      {entity.legType === 'COMPETITION' && entity.ladderOption ? (
        <Td>{entity.ladderOption}</Td>
      ) : (
        <Td /> // Empty cell for alignment
      )}
    </Tr>
  );

  // Only show the first row if not expanded, otherwise show all
  const rows = circuitLeg.entities && circuitLeg.entities.length > 0
    ? (expanded
        ? circuitLeg.entities.map(renderRow)
        : [renderRow(circuitLeg.entities[0], 0)])
    : [];

  return (
    <>
      {rows}
    </>
  );
}

export default CircuitLeg;
