# cyanidebowl

Welcome to [cyanidebowl](https://bloodbowl.granasen.com), a Spike-like facade for Cyanide's BB3 API based on warp-scores (https://warp-scores.net)

## Overview

### Description

This is a Spike-like web page to show match results and data from BB3 obtained through Cyanide's API.

### Build Status

[![dev](https://gitlab.com/warp-scores/warp-scores/badges/dev/pipeline.svg?key_text=dev&key_width=50)](https://gitlab.com/warp-scores/warp-scores/-/pipelines?page=1&scope=branches&ref=dev) [![main](https://gitlab.com/warp-scores/warp-scores/badges/main/pipeline.svg?key_text=main&key_width=50)](https://gitlab.com/warp-scores/warp-scores/-/pipelines?page=1&scope=branches&ref=main)

### Roadmap

- 🟢 Show last matches
- 🟢 Show live matches
- 🟢 Generate Round Robin Schedules
- 🟢 Support Swiss (Wissen) Tournaments
- 🟢 [Support Knockout Tournaments](https://gitlab.com/warp-scores/warp-scores/-/issues/4)
- 🟢 [Increase Mobile UI/UX](https://gitlab.com/warp-scores/warp-scores/-/issues/3)
- 🟡 [Match-Details](https://gitlab.com/warp-scores/warp-scores/-/issues/5)
- 🟡 [Coach page](https://gitlab.com/warp-scores/warp-scores/-/issues/6)
- [League-Statistics](https://gitlab.com/warp-scores/warp-scores/-/issues/7)
- [Competition-Statistics](https://gitlab.com/warp-scores/warp-scores/-/issues/8)
- 🟢 [Discord publishing of match results](https://gitlab.com/warp-scores/warp-scores/-/issues/9)
- 🟢 [Authentication (🟢 Discord-,🟢 NAF-OAuth)](https://gitlab.com/warp-scores/warp-scores/-/issues/10)
- 🟢 [NAF Data export for tournaments](https://gitlab.com/warp-scores/warp-scores/-/issues/11)
- [Admin/Edit results? Win/Tiebreaker editor?](https://gitlab.com/warp-scores/warp-scores/-/issues/12)
- Others: -> See [Issues on GitLab](https://gitlab.com/warp-scores/warp-scores/-/issues/)

### Legend

- 🟢 Finished
- 🟡 In Progress
- 🔴 Obsolete/Canceled

### Configuration
The following variables need to be set.

## For development
Set these variables in your .env file:
FRONTEND_URI=http://localhost:8022
BACKEND_URI=http://localhost:8080
REACT_APP_BACKEND_URI=http://localhost:8080
AUTH_URI="http://localhost:8080/"
SPRING_PROFILES_ACTIVE="dev"
SERVER_PORT=8080
AUTH_AUDIENCE="nst-scores-backend"

## For production
Set these variables in your deployment system, e.g. using fly.toml:
FRONTEND_URI=<Your frontend URI>
BACKEND_URI=<Your backend URI>
REACT_APP_BACKEND_URI=<Same as BACKEND_URI>
AUTH_URI=<Your Auth0 Prodiver URI>
SPRING_PROFILES_ACTIVE="production"
SERVER_PORT=8080
AUTH_AUDIENCE="nst-scores-backend"

## Secrets
The following secrets should be set, in production mode they should be set according to your host platform. For local development they can reside in your .env file, but don't share them with anyone.
SPRING_DATA_MONGODB_URI=<Your MongoDb Connection String>
CYANIDE_API_KEY=<Cyanide API Key>

### Building
To build the server for running locally, run the command:
mvn clean package -P server -DskipDocker -DskipTest -pl api,cyanide-api,backend -am

If you have made changes to api or cyanide-api respectively, you may need to run mvn install in their respective folder.

### Testing
To test the system, I recommend running the server on your development machine with all settings loaded from your .env file.

First, if you haven't already, install dotenv-cli to be able to load .env file into your environment:
npm install dotenv-cli

Then, to run the server:
npx dotenv --  mvn spring-boot:run -P server -pl backend

Or, to run the data-fetcher:
npx dotenv --  mvn spring-boot:run -P fetcher -pl backend

To run the frontend:
cd frontend; npm run dev

### Discord Bot

Please refer to the [Discord Bot documentation](discord-bot.md).

### Changelog

See the [Changelog](CHANGELOG.md).

## Contribute / Get Involved

- [Join Discord](https://discord.gg/hZDU6ymyrj)

## Support & Donations

If you appreciate my work, you can buy me a coffee in person or [online](https://buymeacoffee.com/naytsyrhc).

## Additional Projects

- **Scoreboard and Clock for Blood Bowl**: [bbclock](https://bbclock.warp-scores.net)
- **Blood Bowl Reference Sheet**: [YaRSfBB2020](https://gitlab.com/naytsyrhc/YaRSfBB2020)
- **3D Models**: [Cults](https://cults3d.com/en/users/naytsyrhc)

## Similar / Related Projects

### Blood Bowl 3

- [nuffle.xyz](https://nuffle.xyz) by galentio
- [Nuffles Numbers](https://www.nufflesnumbers.net) by trev
- [Bloodbowl 3 statistics](https://spike.bugeat.com/en/stats) by thierry
- [rebbl.net](https://rebbl.net) by majorbyte
- [bb3replay](https://bb3replay.com/) by TinTuna
- [dicedornot](https://huggingface.co/spaces/mrMesmer/dicedornot) by mrMesmer (based on work by raspel and TinTuna)
- [Ladder Result Predictor](https://huggingface.co/spaces/raspel/BB_predictions) by raspel

### Blood Bowl (General)

- [Dave's Action Calculator](https://www.bloodbowldave.com/) by dave
- [Dadidimerda](https://www.dadidimerda.it/) by Gherardo/Steel

### Disclaimer

This site is completely unofficial and not affiliated
with [Cyanide](https://cyanide-studio.com), [Nacon](https://www.nacongaming.com)
or [Games Workshop](https://www.nacongaming.com).

[Blood Bowl](https://start-warhammer.com/blood-bowl/), [BB3](https://www.bloodbowl-thegame.com/) and probably a lot more names are trademarks of their respective owners. Used without
permission. No challenge to their status intended.

#### Fonts used

I used some free fonts on this web page and within the logo.

##### Sports World
- Designer: Sergiy S. Tkachenko
- Designer URL: http://www.4thfebruary.com.ua

##### Big Star
- Designer: Henrik (HENRIavecunK)

##### Nuffle
- Designer: Neale Davidson, Pixel-Sagas
- https://www.fontspace.com/pixel-sagas
- https://www.pixelsagas.com/
