import React, { useState, useEffect } from 'react';
import { Checkbox, Td, Tr, Button, Input, IconButton, Box, Modal, ModalOverlay, ModalContent, ModalHeader, ModalBody, ModalFooter, ModalCloseButton, useDisclosure, FormControl, FormLabel, Spinner, List, ListItem, ButtonGroup } from '@chakra-ui/react';
import { EditIcon, CheckIcon, CloseIcon, ChevronDownIcon, ChevronRightIcon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import MatchSelectionModal from './ModalMatchSelection';

function AdminCircuitLeg({
  circuitLeg,
  onRemoveLeg,
  onCollectDataChanged,
  onArchivedChanged,
  onLabelChanged,
  onAddEntityToLeg,
  onEditEntity // <-- add this prop for saving edits
}) {
  const [editing, setEditing] = useState(false);
  const [label, setLabel] = useState(circuitLeg.label);
  const [expanded, setExpanded] = useState(false);

  // Modal state
  const [selectedEntity, setSelectedEntity] = useState(null);
  const { isOpen, onOpen, onClose } = useDisclosure();

  const [allMatches, setAllMatches] = useState([]);
  const [loadingMatches, setLoadingMatches] = useState(false);

  const handleSave = () => {
    if (label !== circuitLeg.label) {
      onLabelChanged(circuitLeg.circuitLegId, label);
    }
    setEditing(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const data = JSON.parse(e.dataTransfer.getData('application/json'));
    if (onAddEntityToLeg) {
      onAddEntityToLeg(circuitLeg.circuitLegId, data);
    }
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

  const handleEntitySave = () => {
    if (onEditEntity && selectedEntity) {
      onEditEntity(circuitLeg.circuitLegId, selectedEntity);
    }
    onClose();
  };

  const renderRow = (entity, idx) => (
    <Tr
      key={`${circuitLeg.circuitLegId}-${idx}`}
      onDrop={handleDrop}
      onDragOver={e => e.preventDefault()}
      onClick={() => handleEntityClick(entity)}
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
        {idx === 0 ? (
          editing ? (
            <>
              <Input
                size="sm"
                value={label}
                onChange={e => setLabel(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter') handleSave();
                  if (e.key === 'Escape') { setLabel(circuitLeg.label); setEditing(false); }
                }}
                autoFocus
                width="auto"
              />
              <IconButton icon={<CheckIcon />} size="sm" onClick={handleSave} aria-label="Save" ml={1} />
              <IconButton icon={<CloseIcon />} size="sm" onClick={() => { setLabel(circuitLeg.label); setEditing(false); }} aria-label="Cancel" ml={1} />
            </>
          ) : (
            <>
              {circuitLeg.label}
              <IconButton icon={<EditIcon />} size="sm" onClick={() => setEditing(true)} aria-label="Edit" ml={2} />
            </>
          )
        ) : null}
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
      <Td>
        {idx === 0 && (
          <Checkbox
            defaultChecked={circuitLeg.isCollected}
            onChange={() => onCollectDataChanged &&
              onCollectDataChanged(circuitLeg.circuitLegId, !circuitLeg.isCollected)}
          />
        )}
      </Td>
      <Td>
        {idx === 0 && (
          <Checkbox
            defaultChecked={circuitLeg.isArchived}
            onChange={() => onArchivedChanged &&
              onArchivedChanged(circuitLeg.circuitLegId, !circuitLeg.isArchived)}
          />
        )}
      </Td>
      <Td>
        {idx === 0 && (
          <Button
            colorScheme="red"
            size="xs"
            onClick={e => { e.stopPropagation(); onRemoveLeg && onRemoveLeg(circuitLeg.circuitLegId); }}
          >
            Remove
          </Button>
        )}
      </Td>
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
      <Modal isOpen={isOpen} onClose={onClose}>
        <ModalOverlay />
        <MatchSelectionModal loadingMatches={loadingMatches} allMatches={allMatches} selectedEntity={selectedEntity} handleEntitySave={handleEntitySave} />
      </Modal>
    </>
  );
}

export default AdminCircuitLeg;
