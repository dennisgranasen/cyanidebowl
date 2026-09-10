# Apply these edits to ReporterProfilePage.jsx

1. Extend Chakra imports:

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

2. Replace:

```jsx
import { EditIcon } from '@chakra-ui/icons';
```

with:

```jsx
import { SettingsIcon } from '@chakra-ui/icons';
```

3. Add:

```jsx
import AiReporterRuntimeControls from '../components/ai-reporters/AiReporterRuntimeControls';
```

4. Extend the auth hook destructuring so it includes:

```jsx
getAccessTokenSilently,
getAccessTokenWithPopup,
```

5. Add state near the other state:

```jsx
const [adminReporter, setAdminReporter] = useState(null);
const [adminSaving, setAdminSaving] = useState(false);
```

6. Add these helpers inside the component:

```jsx
const loadAdminReporter = async () => {
  if (!canEdit) return;
  const item = await AiReporterApi.adminReporter(
    reporter.id,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  );
  setAdminReporter(item);
};

const saveAdminReporter = async (patch) => {
  if (!adminReporter) return;

  setAdminSaving(true);
  try {
    const runtime = adminReporter.runtime || {};
    const payload = {
      enabledOverride: runtime.enabledOverride ?? adminReporter.enabled,
      reportsEnabledOverride:
        runtime.reportsEnabledOverride ?? adminReporter.reportsEnabled,
      interactionsEnabledOverride:
        runtime.interactionsEnabledOverride ?? adminReporter.interactionsEnabled,
      playerRatingsEnabledOverride:
        runtime.playerRatingsEnabledOverride ?? adminReporter.playerRatingsEnabled,
      writingWeightOverride:
        runtime.writingWeightOverride ?? adminReporter.writingWeight,
      commentProbabilityOverride: runtime.commentProbabilityOverride ?? null,
      reactionProbabilityOverride: runtime.reactionProbabilityOverride ?? null,
      replyProbabilityOverride: runtime.replyProbabilityOverride ?? null,
      ...patch,
    };

    const updated = await AiReporterApi.updateRuntime(
      reporter.id,
      payload,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    );
    setAdminReporter(updated);
  } finally {
    setAdminSaving(false);
  }
};

const resetAdminReporter = () =>
  saveAdminReporter({
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

7. Replace the current admin Edit button in the hero with:

```jsx
{canEdit && (
  <Popover placement="bottom-end" onOpen={loadAdminReporter}>
    <PopoverTrigger>
      <IconButton
        aria-label={`Runtime settings for ${reporter.alias}`}
        title="Runtime settings"
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
        {!adminReporter ? (
          <Spinner size="sm" />
        ) : (
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

8. The separate `/admin/ai-reporters/:reporterId` route can now be removed from App.jsx.
