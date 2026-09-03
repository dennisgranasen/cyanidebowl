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

Required service configuration:

- `PYBB3_INTERNAL_API_KEY`: a long random value shared only by Spring and pybb3
- `PYBB3_SERVICE_URL`: internal pybb3 service URL
- `PYBB3_COOKIE_SECURE=true` in HTTPS production
- `PYBB3_REF`: preferably an immutable pybb3 tag or commit for Docker builds
