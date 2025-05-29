# Discord Bot

[warp-scores](https://warp-scores.net) operates a Discord Bot that you can utilize to interact with this website on your own Discord server.

## Invitation/Installation of the Bot
To install the bot on your Discord server (you must be the owner of the server), just use this [invitation link](https://discord.com/oauth2/authorize?client_id=1252283014327570585).
The bot requires the scopes `application.commands` and `bot` and needs the permissions `Send Messages`, `Add Reactions`, `Attach Files`.

## Usage
After you have invited the bot to your Discord server, you get access to the bot's commands as described below:

### Command: Help
#### `/help`
Sends you a message with information similar to this guide.

### Command: Api-Status
#### `/apistatus`
Shows the current known status of Cyanide's API.

### Command: Lookup
#### `/lookup leaguename [NAME]`
Looks up a league in the Cyanide API with the given `[NAME]` (returns UUID).

**Options:**
- `leaguename`: The exact name of the league you want to look up as in BB3.

### Command: League
You have three possible use cases with this command:
1. Register or update a league with the given `[UUID]` to publish to the current channel.
2. Unregister a league with the given `[UUID]` from publishing to the current channel.
3. Show information of registered leagues for this channel.

#### `/league uuid [UUID] spoiler [true|false]`

Register a league or update registration options.

**Options:**
- `uuid [UUID]`: The UUID of the league. You can copy this from the URL issued by the `/lookup` command.
- `spoiler [true]`: If `true`, the results in messages for new matches will be hidden as a spoiler.

#### `/league uuid [UUID] unregister true`
Unregister the league with the given `[UUID]` from publishing to the current channel.

#### `/league`
Show all leagues currently registered to publish to the current channel.

### Command: Latest Matches
#### `/latestmatches`
Show the latest matches of current leagues registered to this channel.

**Options:**
- `count [1-12]`: Number of matches to show (default: 6, min: 1, max: 12).
- `spoiler [true|false]`: If `true`, the results of the matches will be hidden as a spoiler.
