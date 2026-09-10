# Responsive AI reporter gallery patch

The `/staff` gallery deliberately has **no maximum column count**.

It uses:

```jsx
gridTemplateColumns="repeat(auto-fill, minmax(min(100%, 180px), 1fr))"
```

The viewport therefore determines how many cards fit per row from the card's preferred
minimum width, rather than from explicit breakpoints such as 3, 4 or 6 columns.

Included complete replacement files:

- `frontend/src/pages/StaffPage.jsx`
- `frontend/src/pages/ReporterProfilePage.jsx`

`IMPLEMENTATION.md` contains the small backend/API/router/admin-page edits that should
be merged into current `dev` rather than replacing those larger files blindly.

After applying, run:

```bash
mvn test -Pserver -DskipDocker -pl api,cyanide-api,backend -am
cd frontend
npm test -- --runInBand
npm run build
```
