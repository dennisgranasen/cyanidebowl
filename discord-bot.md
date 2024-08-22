# Discord Bot

[warp-scores](https://warp-scores.net) operates a Discord Bot that you can utilize to interact with this website on your own Discord server.

## Invitation/Installation of the Bot
In order to install the bot on your Discord server (you must be the owner of the server), just use this [Invitation Link](https://discord.com/oauth2/authorize?client_id=1252283014327570585).

The bot has the scopes `application.commands` and `bot` and needs the permissions `Send Messages`, `Add Reactions`, `Attach Files`.

## Usage

After you have invited the bot to your Discord server, you get access to the bot's commands as decribed below: 

### Command "Help"

#### `/help`

Will send you a message with similar content as this.

### Command "Api-Status"

#### `/apistatus`

Shows the current known status of Cyanides API.

### Command "Lookup"

#### `/lookup leaguename [NAME]`

Does a lookup in Cyanide API for a league with given `[NAME]` (returns UUID).

Options:
- `leaguename`: The name of the league you want to lookup. Must be the exact name as in BB3.

### Command "League"

You have three possible use cases with this command:

- Register a league (or update registration options) with given `[UUID]` to publish to current channel.
- Unregister a league with given `[UUID]` from publishing to current channel.
- Show information of registered leagues for this channel.

#### `/league uuid [UUID] spoiler [true|false]`

Register a league or update registration options.

Options:
- `uuid [UUID]`: The UUID of the league. You can copy that from URL in warp-scores.
- `spoiler [true]`: If `true` the results in messages for new matches will be hidden as spoiler.

#### `/league uuid [UUID] unregister true`

Unregister the league with given `[UUID]` from publishing to current channel.

#### `/league`

Show all leagues currently registered to publish to current channel.

### Command "Latest Matches"

#### `/latestmatches`

Show latest matches of current leagues registered to this channel.

Options:
- `count [1-12]`: Number of matches to show (default: 6, min: 1 max: 12)
- `spoiler [true|false]`: If 'true' the results of the matches will be hidden as spoiler.

