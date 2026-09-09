"""Statistics from resolved BB3 sequence messages, not dice-choice prompts.

ResultRoll.Difficulty is the modified target; Outcome 0/1 is failure/success.
Block faces are the BB3 five-value enum (the two push faces share value 2).
Action outcomes use the resulting board, so skills can change the selected face's
effect. Unknown rolls remain explicitly labelled rather than guessed.
"""
import base64
import xml.etree.ElementTree as ET
from collections import defaultdict

ROLL_NAMES = {1: 'Rush', 2: 'Dodge', 4: 'Pickup', 5: 'Pass', 6: 'Leap', 7: 'Catch'}
BLOCK_FACES = {0: 'skull', 1: 'bothDown', 2: 'push', 3: 'tackle', 4: 'defenderDown'}


def number(node, path, default=None):
    value = node.findtext(path)
    try:
        return int(value) if value is not None else default
    except ValueError:
        return default


def decode_message(node):
    if node is None:
        return None
    payload = node.findtext('MessageData')
    if not payload:
        return None
    try:
        raw = payload.encode()
        for _ in range(2):
            raw = base64.b64decode(raw, validate=True)
            if raw.lstrip().startswith(b'<'):
                if b'<!DOCTYPE' in raw.upper():
                    return None
                return ET.fromstring(raw)
    except (ValueError, ET.ParseError):
        return None
    return None


def board_players(board):
    players = {}
    if board is not None:
        for team, state in enumerate(board.findall('ListTeams/TeamState')):
            for player in state.findall('ListPitchPlayers/PlayerState'):
                player_id = number(player, 'Id')
                if player_id is not None:
                    players[player_id] = (team, player)
    return players


def block_action(outcome, players):
    attacker = players.get(number(outcome, 'AttackerId'))
    defender = players.get(number(outcome, 'DefenderId'))
    if attacker is None or defender is None:
        return 'unknown'
    # Status omitted means standing; 1/2 mean prone/stunned. Removed players
    # have a nonzero Situation (bench/KO/casualty).
    def down(player):
        return number(player, 'Status', 0) in (1, 2) or number(player, 'Situation', 0) != 0
    attacker_down, defender_down = down(attacker[1]), down(defender[1])
    if attacker_down:
        return 'neutral' if defender_down else 'fail'
    if defender_down:
        return 'success'
    for push in outcome.findall('Pushbacks/ResultPushBack'):
        x, y = number(push, 'CellTo/X', 0), number(push, 'CellTo/Y', 0)
        if not (0 <= x < 26 and 0 <= y < 15):
            return 'success'
    return 'neutral'


def event_statistics(root):
    counts = defaultdict(lambda: defaultdict(lambda: defaultdict(int)))
    pending = {}
    owners = {}
    for replay_step in root.findall('ReplayStep'):
        players = board_players(replay_step.find('BoardState'))
        owners.update({pid: value[0] for pid, value in players.items()})
        for step_result in replay_step.findall('.//EventExecuteSequence/Sequence/StepResult'):
            step = decode_message(step_result.find('Step'))
            if step is None or number(step, 'IsEvaluation', 0):
                continue
            player_id = number(step, 'PlayerId')
            team = owners.get(player_id)
            key = (player_id, number(step, 'TargetId'))
            for wrapper in step_result.findall('Results/StringMessage'):
                result = decode_message(wrapper)
                if result is None:
                    continue
                if result.tag == 'ResultRoll':
                    dice = result.findall('Dice/Die')
                    outcome = number(result, 'Outcome')
                    target = number(result, 'Difficulty')
                    roll_type = number(result, 'RollType')
                    if team is None or len(dice) != 1 or number(dice[0], 'DieType') != 0:
                        continue
                    if outcome not in (0, 1) or target is None or target < 1:
                        continue
                    label = ROLL_NAMES.get(roll_type, f'Roll type {roll_type}')
                    row = counts[(label, 'd6', f'{target}+')][team]
                    row['total'] += 1
                    row['success'] += outcome
                elif result.tag == 'QuestionBlockDice':
                    dice_count = len(result.findall('Dice/Die'))
                    if dice_count:
                        pending[key] = {'dice': f'{"-" if number(result, "AttackerChoice", 1) == 0 else ""}{dice_count}D'}
                elif result.tag == 'ResultBlockRoll':
                    # This is the chosen die, not all dice offered by the prompt.
                    pending.setdefault(key, {})['face'] = BLOCK_FACES.get(number(result, 'Die/Value'), 'unknown')
                elif result.tag == 'ResultBlockOutcome':
                    block_key = (number(result, 'AttackerId'), number(result, 'DefenderId'))
                    block = pending.pop(block_key, {})
                    block_team = owners.get(block_key[0])
                    if block_team is None:
                        continue
                    bucket = block.get('dice', 'Unknown dice')
                    face_row = counts[('Block', 'blockFaces', bucket)][block_team]
                    face_row[block.get('face', 'unknown')] += 1
                    face_row['total'] += 1
                    action_row = counts[('Block outcome', 'blockActions', bucket)][block_team]
                    action_row[block_action(result, players)] += 1
                    action_row['total'] += 1
    return [dict(eventType=name, kind=kind, target=target,
                 teams=[dict(teamIndex=team, **values) for team, values in sorted(teams.items())])
            for (name, kind, target), teams in sorted(counts.items())]
