import React, { useEffect, useState } from 'react';
import {
  Button,
  FormControl,
  FormLabel,
  HStack,
  NumberInput,
  NumberInputField,
  Stack,
  Switch,
} from '@chakra-ui/react';

function AiReporterRuntimeControls({ reporter, saving, onSave, onReset }) {
  const [writingWeight, setWritingWeight] = useState(reporter.writingWeight);

  useEffect(() => {
    setWritingWeight(reporter.writingWeight);
  }, [reporter.writingWeight]);

  return (
    <Stack spacing={3}>
      <FormControl display="flex" alignItems="center" justifyContent="space-between">
        <FormLabel mb="0" mr={3}>Enabled</FormLabel>
        <Switch
          isChecked={reporter.enabled}
          isDisabled={saving}
          onChange={(e) => onSave({ enabledOverride: e.target.checked })}
        />
      </FormControl>

      <FormControl display="flex" alignItems="center" justifyContent="space-between">
        <FormLabel mb="0" mr={3}>Can write reports</FormLabel>
        <Switch
          isChecked={reporter.reportsEnabled}
          isDisabled={!reporter.enabled || saving}
          onChange={(e) => onSave({ reportsEnabledOverride: e.target.checked })}
        />
      </FormControl>

      <FormControl display="flex" alignItems="center" justifyContent="space-between">
        <FormLabel mb="0" mr={3}>Can interact</FormLabel>
        <Switch
          isChecked={reporter.interactionsEnabled}
          isDisabled={!reporter.enabled || saving}
          onChange={(e) => onSave({ interactionsEnabledOverride: e.target.checked })}
        />
      </FormControl>

      <FormControl display="flex" alignItems="center" justifyContent="space-between">
        <FormLabel mb="0" mr={3}>Rates players</FormLabel>
        <Switch
          isChecked={reporter.playerRatingsEnabled}
          isDisabled={!reporter.enabled || saving}
          onChange={(e) => onSave({ playerRatingsEnabledOverride: e.target.checked })}
        />
      </FormControl>

      <FormControl>
        <FormLabel>Writing weight</FormLabel>
        <NumberInput
          min={0}
          step={0.05}
          value={writingWeight}
          isDisabled={!reporter.enabled || saving}
          onChange={(_, value) => {
            if (Number.isFinite(value)) setWritingWeight(value);
          }}
          onBlur={() => {
            if (Number.isFinite(writingWeight) && writingWeight !== reporter.writingWeight) {
              onSave({ writingWeightOverride: writingWeight });
            }
          }}
        >
          <NumberInputField />
        </NumberInput>
      </FormControl>

      <HStack justify="flex-end">
        <Button
          size="sm"
          variant="outline"
          isLoading={saving}
          onClick={onReset}
        >
          Reset to profile defaults
        </Button>
      </HStack>
    </Stack>
  );
}

export default AiReporterRuntimeControls;
