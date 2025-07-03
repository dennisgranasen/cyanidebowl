import React, { useEffect, useState } from 'react';
import { Checkbox, Image, Td, Tr, Button, Input, IconButton } from '@chakra-ui/react';
import { QuestionOutlineIcon, EditIcon, CheckIcon, CloseIcon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import imageUrls from '../../imageUrls';
import config from '../../config';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import logger from '../../util/logger';
import useFetchCompetition from '../../hooks/useFetchCompetition';
import { identityUtils } from '../../util/identityUtil';

const { boxSize } = config;

function CircuitLeg({
  circuitLeg,
  onRemoveLeg,
  onCollectDataChanged,
  onArchivedChanged,
  onLabelChanged,
  onAddEntityToLeg
}) {
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(false);
  const [label, setLabel] = useState(circuitLeg.label);

  // Helper for entity fields
  const renderRow = (entity, idx) => {
    // You may want to fetch competition/league info here per entity if needed
    // For now, just use entity fields directly
    return (
      <Tr key={`${circuitLeg.circuitLegId}-${idx}`}
        onDragOver={e => e.preventDefault()}
        onDrop={handleDrop}
        style={{ background: 'inherit' }}>
        <Td>{/* Logo or image if you want, or leave blank */}</Td>
        <Td>
          {idx === 0 && editing ? (
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
              {idx === 0 && (
                <>
                  {circuitLeg.label}
                  <IconButton icon={<EditIcon />} size="sm" onClick={() => setEditing(true)} aria-label="Edit" ml={2} />
                </>
              )}
            </>
          )}
        </Td>
        <Td>{entity.entityId?.value}</Td>
        <Td>{entity.legType}</Td>
        <Td>{entity.game}</Td>
        <Td>{entity.platform}</Td>
        <Td>{entity.ruleset}</Td>
        <Td>{entity.ladderOption}</Td>
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
              onClick={() => onRemoveLeg && onRemoveLeg(circuitLeg.circuitLegId)}
            >
              Remove
            </Button>
          )}
        </Td>
      </Tr>
    );
  };

  const handleSave = () => {
    if (label !== circuitLeg.label) {
      onLabelChanged(circuitLeg.circuitLegId, label);
    }
    setEditing(false);
  };

  // Drop handler
  const handleDrop = (e) => {
    e.preventDefault();
    const data = JSON.parse(e.dataTransfer.getData('application/json'));
    // Call parent handler to add entity to this leg
    if (onAddEntityToLeg) {
      onAddEntityToLeg(circuitLeg.circuitLegId, data);
    }
  };

  return (
    <>
      {(circuitLeg.entities || []).map((entity, idx) => renderRow(entity, idx))}
    </>
  );
}

export default CircuitLeg;
