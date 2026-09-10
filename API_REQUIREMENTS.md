# AiReporterApi.js

Make sure this method exists:

```js
adminReporter: async (id, getAccessTokenSilently, getAccessTokenWithPopup) => {
  const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
  return (await axios.get(
    `/admin/ai-reporters/${encodeURIComponent(id)}`,
    auth
  )).data;
},
```

The backend should expose:

```java
@GetMapping("/{id}")
public AdminReporter get(@PathVariable String id) {
    return toAdminReporter(registry.require(id));
}
```

under `/admin/ai-reporters`.
