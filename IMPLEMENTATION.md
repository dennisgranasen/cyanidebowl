# Manual focused changes for current dev

## 1. AiReporterDefinition.java

In `AiReporterDefinition.Portrait`, add:

```java
private String avatar;
```

## 2. AiReporterProfileLoader.java

Replace:

```java
d.getPortrait().setImage(str(portrait.get("image")));
d.getPortrait().setPromptKey(str(portrait.get("prompt_key")));
```

with:

```java
String portraitImage = str(portrait.get("image"));
d.getPortrait().setImage(portraitImage);
d.getPortrait().setAvatar(defaultStr(portrait.get("avatar"), portraitImage));
d.getPortrait().setPromptKey(str(portrait.get("prompt_key")));
```

## 3. AiReporterController.java

Add `String avatarImage` immediately after `String portraitImage` in `PublicReporter`.

In `PublicReporter.from(...)`, add:

```java
d.getPortrait().getAvatar(),
```

immediately after `d.getPortrait().getImage(),`.

## 4. AiReporterAdminController.java

Add this endpoint:

```java
@GetMapping("/{id}")
public AdminReporter get(@PathVariable String id) {
    return toAdminReporter(registry.require(id));
}
```

Add `String portraitImage, String avatarImage` to `AdminReporter` after role.

Pass:

```java
definition.getPortrait().getImage(),
definition.getPortrait().getAvatar(),
```

after `definition.getRole()` in `toAdminReporter`.

## 5. AiReporterApi.js

Add:

```js
adminReporter: async (id, getAccessTokenSilently, getAccessTokenWithPopup) => {
  const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
  return (await axios.get(`/admin/ai-reporters/${encodeURIComponent(id)}`, auth)).data;
},
```

## 6. App.jsx

Immediately after the existing `/admin/ai-reporters` route, add:

```jsx
<Route
  path="/admin/ai-reporters/:reporterId"
  element={<ProtectedRoute component={AdminAiReportersPage} />}
/>
```

## 7. AdminPage.jsx

Immediately below the League Systems HeaderCard add:

```jsx
{userPermissions.writeSiteAdmin && (
  <HStack>
    <Button colorScheme="purple" onClick={() => navigate('/admin/ai-reporters')}>
      AI Reporters
    </Button>
  </HStack>
)}
```

## 8. AdminAiReportersPage.jsx

Use `useParams()` and, when `reporterId` is present, load the single reporter through
`AiReporterApi.adminReporter(reporterId, ...)`. In single-reporter mode render the large
`portraitImage` with Chakra `<Image>` and keep the runtime controls next to it.

The list page should continue to use `avatarImage || portraitImage`.

Recommended import:

```js
import { Link as RouteLink, useNavigate, useParams } from 'react-router-dom';
```

## 9. Reporter Markdown

Profiles can now use:

```yaml
portrait:
  image: /images/staff/aeltharion_full.png
  avatar: /images/staff/aeltharion_small.png
  prompt_key: aeltharion
```

If `avatar` is omitted, it falls back to `image`.
