# BlaskScore authentication

BlaskScore uses Auth0 as its identity broker. Enable Google and GitHub as Auth0
connections for the production application; the existing Universal Login then
presents them without provider-specific code in this repository. NAF can be
added as another enterprise/custom connection once NAF's supported identity
protocol and claims are confirmed.

Steam has two distinct roles:

1. **Sign in to BlaskScore.** Steam's website identity mechanism can be bridged
   into Auth0 as a custom connection. It identifies a Steam account but does not
   give pybb3 a reusable BB3 ticket.
2. **Connect Blood Bowl 3.** The account page asks for Steam credentials and
   drives Steam Guard through the internal pybb3 service. Only the Steam login
   name and public Steam ID are persisted. Passwords, Guard codes, refresh
   tokens and BB3 session IDs are not stored in MongoDB.

Auth0 account linking must be configured if the same person should be able to
alternate between Google, GitHub, Steam and NAF while retaining one profile.
Only link identities after re-authenticating both accounts; never merge solely
on an unverified matching email address.

## Authorization and coach ownership

Authentication and application authorization are separate concerns. Auth0 identifies
the signed-in user by subject; BlaskScore resolves application roles and coach
ownership through its persisted user record and `UserPermissionService`.

The canonical authorization model is:

- `siteAdmin` is the application super-role;
- global LeagueSystem administration grants administration across LeagueSystems;
- `adminForLeagueSystems` grants administration only for the listed LeagueSystems;
- register-league permission is independent unless implied by `siteAdmin`;
- method-security checks resolve these permissions through `UserPermissionService`
  rather than directly trusting UI state.

Coach ownership uses `coachClaims`. A claim is game-aware, so an ID from BB1, BB2 or
BB3 is not treated as the same identity merely because its numeric/string coach ID
matches another edition. Features such as **My teams** and personal statistics use
these claims when deciding which teams/coaches belong to the signed-in user.

Legacy parallel representations such as a bare global `coachIds` list or separate
controller-specific admin lists must not be reintroduced. Permission data returned to
the frontend is derived from the canonical backend model; the frontend must remain
deny-by-default while that data is loading or unavailable.

`write:league_admin`, `write:site_admin` and related JWT authorities may still be
accepted where configured, but persisted user roles/scopes are resolved by the same
permission service so callers do not need separate authorization models.

## Service configuration

Required service configuration:

- `PYBB3_INTERNAL_API_KEY`: a long random value shared only by Spring and pybb3
- `PYBB3_SERVICE_URL`: internal pybb3 service URL
- `PYBB3_COOKIE_SECURE=true` in HTTPS production
- `PYBB3_REF`: preferably an immutable pybb3 tag or commit for Docker builds
## Replay service credential

The scheduled replay account is separate from users' short-lived Steam sessions. Its refresh ticket is AES-encrypted
by pybb3 and stored in the `pybb3-credentials` volume. Set a long, stable
`PYBB3_CREDENTIAL_ENCRYPTION_KEY`; losing or changing it requires an administrator to authenticate the account again.
Cyanidebowl stores only schedule/status metadata in MongoDB and never receives the refresh ticket.

The replay client connects only for a scheduled batch and closes immediately afterwards. If Steam reports that the
account is active elsewhere, the run is skipped and recorded as `STEAM_ACCOUNT_ACTIVE`; the credential remains valid.
Invalid/expired credentials are treated separately and produce an administrator warning.
