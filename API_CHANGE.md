# AiReporterApi.js requirement

Ensure this exists:

```js
adminReporter: async (id, getAccessTokenSilently, getAccessTokenWithPopup) => {
  const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
  return (await axios.get(
    `/admin/ai-reporters/${encodeURIComponent(id)}`,
    auth
  )).data;
},
```

The frontend route `/admin/ai-reporters/:reporterId` can be removed from App.jsx.
The backend GET `/admin/ai-reporters/{id}` must remain.
