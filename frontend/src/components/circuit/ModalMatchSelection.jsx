import React, { useEffect, useState } from 'react';
import { Checkbox, Td, Tr, Button, Input, IconButton, Box, Modal, ModalOverlay, ModalContent, ModalHeader, ModalBody, ModalFooter, ModalCloseButton, useDisclosure, FormControl, FormLabel, Spinner, List, ListItem, ButtonGroup } from '@chakra-ui/react';
import { EditIcon, CheckIcon, CloseIcon, ChevronDownIcon, ChevronRightIcon } from '@chakra-ui/icons';


function MatchSelectionModal({ isOpen, onClose, entity, loadingMatches, allMatches, onSave, selectedEntity, handleEntitySave }) {
    return (
        <ModalContent>
          <ModalHeader>Edit Leg Entity</ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            {loadingMatches ? (
              <Spinner />
            ) : selectedEntity && (
              <Box display="flex" gap={4}>
                {/* All matches */}
                <Box flex={1}>
                  <FormLabel>All Matches</FormLabel>
                  <List borderWidth="1px" borderRadius="md" maxH="200px" overflowY="auto">
                    {allMatches
                      .filter(m =>
                        !(selectedEntity.included || []).includes(m.id) &&
                        !(selectedEntity.excluded || []).includes(m.id)
                      )
                      .map(m => (
                        <ListItem key={m.id} display="flex" alignItems="center" justifyContent="space-between">
                          <span>{m.name || m.id}</span>
                          <ButtonGroup size="xs" isAttached>
                            <Button
                              colorScheme="green"
                              onClick={() =>
                                setSelectedEntity({
                                  ...selectedEntity,
                                  included: [...(selectedEntity.included || []), m.id]
                                })
                              }
                            >Include</Button>
                            <Button
                              colorScheme="red"
                              onClick={() =>
                                setSelectedEntity({
                                  ...selectedEntity,
                                  excluded: [...(selectedEntity.excluded || []), m.id]
                                })
                              }
                            >Exclude</Button>
                          </ButtonGroup>
                        </ListItem>
                      ))}
                  </List>
                </Box>
                {/* Included */}
                <Box flex={1}>
                  <FormLabel>Included</FormLabel>
                  <List borderWidth="1px" borderRadius="md" maxH="200px" overflowY="auto">
                    {(selectedEntity.included || []).map(id => (
                      <ListItem key={id} display="flex" alignItems="center" justifyContent="space-between">
                        <span>{allMatches.find(m => m.id === id)?.name || id}</span>
                        <Button
                          size="xs"
                          onClick={() =>
                            setSelectedEntity({
                              ...selectedEntity,
                              included: selectedEntity.included.filter(x => x !== id)
                            })
                          }
                        >Remove</Button>
                      </ListItem>
                    ))}
                  </List>
                </Box>
                {/* Excluded */}
                <Box flex={1}>
                  <FormLabel>Excluded</FormLabel>
                  <List borderWidth="1px" borderRadius="md" maxH="200px" overflowY="auto">
                    {(selectedEntity.excluded || []).map(id => (
                      <ListItem key={id} display="flex" alignItems="center" justifyContent="space-between">
                        <span>{allMatches.find(m => m.id === id)?.name || id}</span>
                        <Button
                          size="xs"
                          onClick={() =>
                            setSelectedEntity({
                              ...selectedEntity,
                              excluded: selectedEntity.excluded.filter(x => x !== id)
                            })
                          }
                        >Remove</Button>
                      </ListItem>
                    ))}
                  </List>
                </Box>
              </Box>
            )}
            <Box fontSize="sm" color="gray.500" mt={2}>
              If <b>Included</b> is non-empty, only those matches are included.<br />
              If <b>Excluded</b> is non-empty, those matches are excluded.
            </Box>
          </ModalBody>
          <ModalFooter>
            <Button colorScheme="blue" mr={3} onClick={handleEntitySave}>
              Save
            </Button>
            <Button onClick={onClose}>Cancel</Button>
          </ModalFooter>
        </ModalContent>
    )
};

export default MatchSelectionModal;