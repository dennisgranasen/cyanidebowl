# ReporterProfilePage.jsx – exact change

The redesigned profile page should NOT have an Edit button/link.

Use an admin-only settings gear in the hero.

Required Chakra imports:

```jsx
IconButton,
Popover,
PopoverArrow,
PopoverBody,
PopoverCloseButton,
PopoverContent,
PopoverHeader,
PopoverTrigger,
```

Use:

```jsx
import { SettingsIcon } from '@chakra-ui/icons';
import AiReporterRuntimeControls from '../components/ai-reporters/AiReporterRuntimeControls';
```

Remove `EditIcon` and any `RouteLink` used only for the Edit button.

Extend the auth hook destructuring with:

```jsx
getAccessTokenSilently,
getAccessTokenWithPopup,
```

Add state:

```jsx
const [adminReporter, setAdminReporter] = useState(null);
const [adminSaving, setAdminSaving] = useState(false);
const [adminError, setAdminError] = useState(null);
```

Add:

```jsx
const loadAdminReporter = async () => {
  if (!canEdit || adminReporter) return;

  setAdminError(null);
  try {
    const item = await AiReporterApi.adminReporter(
      reporter.id,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    );
    setAdminReporter(item);
  } catch (reason) {
    setAdminError(reason);
  }
};

const buildAdminPayload = (current, patch) => {
  const runtime = current.runtime || {};
  return {
    enabledOverride: runtime.enabledOverride ?? current.enabled,
    reportsEnabledOverride: runtime.reportsEnabledOverride ?? current.reportsEnabled,
    interactionsEnabledOverride:
      runtime.interactionsEnabledOverride ?? current.interactionsEnabled,
    playerRatingsEnabledOverride:
      runtime.playerRatingsEnabledOverride ?? current.playerRatingsEnabled,
    writingWeightOverride:
      runtime.writingWeightOverride ?? current.writingWeight,
    commentProbabilityOverride: runtime.commentProbabilityOverride ?? null,
    reactionProbabilityOverride: runtime.reactionProbabilityOverride ?? null,
    replyProbabilityOverride: runtime.replyProbabilityOverride ?? null,
    ...patch,
  };
};

const saveAdminReporter = async (patch) => {
  if (!adminReporter) return;

  setAdminSaving(true);
  setAdminError(null);
  try {
    const updated = await AiReporterApi.updateRuntime(
      reporter.id,
      buildAdminPayload(adminReporter, patch),
      getAccessTokenSilently,
      getAccessTokenWithPopup
    );
    setAdminReporter(updated);
  } catch (reason) {
    setAdminError(reason);
  } finally {
    setAdminSaving(false);
  }
};

const resetAdminReporter = () => saveAdminReporter({
  enabledOverride: null,
  reportsEnabledOverride: null,
  interactionsEnabledOverride: null,
  playerRatingsEnabledOverride: null,
  writingWeightOverride: null,
  commentProbabilityOverride: null,
  reactionProbabilityOverride: null,
  replyProbabilityOverride: null,
});
```

Replace the old Edit button block with:

```jsx
{canEdit && (
  <Popover placement="bottom-end" onOpen={loadAdminReporter}>
    <PopoverTrigger>
      <IconButton
        aria-label={`Settings for ${reporter.alias}`}
        title="Reporter settings"
        icon={<SettingsIcon />}
        size="sm"
        variant="outline"
      />
    </PopoverTrigger>

    <PopoverContent w="340px">
      <PopoverArrow />
      <PopoverCloseButton />
      <PopoverHeader fontWeight="700">
        Reporter settings
      </PopoverHeader>
      <PopoverBody>
        {adminError && (
          <Text color="red.400" fontSize="sm" mb={3}>
            {adminError.message || String(adminError)}
          </Text>
        )}

        {!adminReporter && !adminError && <Spinner size="sm" />}

        {adminReporter && (
          <AiReporterRuntimeControls
            reporter={adminReporter}
            saving={adminSaving}
            onSave={saveAdminReporter}
            onReset={resetAdminReporter}
          />
        )}
      </PopoverBody>
    </PopoverContent>
  </Popover>
)}
```

The `/staff` gallery itself must contain no admin Edit button at all.
