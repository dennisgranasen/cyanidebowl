/* global db, print, printjson, process */

// Run with mongosh. Read-only by default; set MIGRATION_APPLY=true to write.
const APPLY = String(process.env.MIGRATION_APPLY || '').toLowerCase() === 'true';
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');
const BACKUP_COLLECTION = 'leagueStructureMigrationBackup';

const seasons = db.getCollection('season');
const stages = db.getCollection('stage');
const selections = db.getCollection('stageSource');
const phases = db.getCollection('phase');
const registeredSources = db.getCollection('registeredSource');
const dataCollections = db.getCollection('dataCollection');

const counters = { stages: 0, phases: 0, selections: 0, sources: 0, collections: 0 };
const backedUp = new Set();
const plannedPhases = new Set();
const plannedSources = new Map();
const plannedCollections = new Set();

function text(value) { return value == null ? '' : String(value); }
function contains(value, terms) { return terms.some((term) => value.includes(term)); }
function stablePart(value) {
  return encodeURIComponent(text(value)).replace(/%/g, '_').slice(0, 240);
}
function sourceKey(source) {
  const identity = source.sourceEntityId;
  if (!identity) return text(source._id);
  return text(identity.key || identity._id || identity.value || JSON.stringify(identity));
}
function phaseType(stage) {
  const value = `${text(stage.phase)} ${text(stage.name)}`.toLowerCase();
  if (contains(value, ['off-season', 'offseason'])) return 'OFF_SEASON';
  if (contains(value, ['preseason', 'pre-season', 'försäsong'])) return 'PRESEASON';
  if (contains(value, ['friendly', 'friendlies', 'träning'])) return 'FRIENDLIES';
  if (contains(value, ['qualif', 'qualification', 'kval'])) return 'QUALIFICATION';
  if (contains(value, ['playoff', 'play-off', 'slutspel', 'play-in', 'quarter', 'kvart', 'semi', 'final', 'bronze', 'brons'])) return 'PLAYOFFS';
  if (contains(value, ['group', 'grupp', 'östra', 'västra', 'east', 'west'])) return 'GROUP_STAGE';
  if (/\bmain(?:[_ -](?:east|west))?\b/.test(value)) return 'GROUP_STAGE';
  return 'OTHER';
}
function phaseName(stage, type) {
  if (type !== 'OTHER') return ({ OFF_SEASON: 'Off-season', PRESEASON: 'Preseason', FRIENDLIES: 'Friendlies',
    QUALIFICATION: 'Qualification', GROUP_STAGE: 'Group stage', PLAYOFFS: 'Playoffs',
  })[type];
  const legacy = text(stage.phase).trim();
  if (/consolation/i.test(legacy)) return 'Consolation';
  return legacy || 'Main competition';
}
function phaseSequence(type) {
  return ({ OFF_SEASON: 0, PRESEASON: 10, FRIENDLIES: 10, QUALIFICATION: 20,
    GROUP_STAGE: 30, PLAYOFFS: 40, OTHER: 50 })[type];
}
function stageType(stage, type) {
  const value = text(stage.name).toLowerCase();
  if (type === 'GROUP_STAGE' || contains(value, ['group', 'grupp', 'östra', 'västra', 'east', 'west'])) return 'GROUP';
  if (type === 'PLAYOFFS' || contains(value, ['round', 'omgång', 'final', 'semi', 'kvart', 'play-in'])) return 'ROUND';
  return 'OTHER';
}
function stageStep(stage) {
  const value = `${text(stage.phase)} ${text(stage.name)}`.toLowerCase();
  if (contains(value, ['play-in', 'play in'])) return 1;
  if (contains(value, ['quarter', 'kvart'])) return 2;
  if (contains(value, ['semi'])) return 3;
  if (contains(value, ['final', 'bronze', 'brons'])) return 4;
  // Legacy sequence was global across the season. A stage's step is local to its phase.
  return 1;
}
function stageDisplayOrder(stage) {
  const value = `${text(stage.phase)} ${text(stage.name)}`.toLowerCase();
  if (contains(value, ['west', 'västra'])) return 2;
  if (contains(value, ['east', 'östra'])) return 1;
  return stageStep(stage);
}
function backup(collection, document) {
  if (!APPLY || !document) return;
  const key = `${collection}:${EJSON.stringify(document._id)}`;
  if (backedUp.has(key)) return;
  db.getCollection(BACKUP_COLLECTION).insertOne({ runId: RUN_ID, operation: 'UPDATE', collection, document, backedUpAt: new Date() });
  backedUp.add(key);
}
function recordCreate(collection, id) {
  if (APPLY) db.getCollection(BACKUP_COLLECTION).insertOne({
    runId: RUN_ID, operation: 'CREATE', collection, documentId: id, backedUpAt: new Date(),
  });
}

print(`League structure migration: ${APPLY ? 'APPLY' : 'DRY RUN'}`);
if (!APPLY) print('No database writes will be made. Set MIGRATION_APPLY=true to apply this plan.');

seasons.find({}).forEach((season) => {
  stages.find({ seasonId: season._id }).sort({ sequence: 1 }).forEach((stage) => {
    let phaseId = stage.phaseId;
    if (!phaseId) {
      const type = phaseType(stage);
      const name = phaseName(stage, type);
      phaseId = `migrated:phase:${stablePart(season._id)}:${stablePart(type)}:${stablePart(name)}`;
      const phase = { _id: phaseId, seasonId: season._id, leagueSystemId: season.leagueSystemId,
        name, type, sequence: phaseSequence(type) };
      if (!phases.findOne({ _id: phaseId }) && !plannedPhases.has(phaseId)) {
        counters.phases += 1;
        print(`CREATE phase ${phaseId}: ${name} (${type})`);
        plannedPhases.add(phaseId);
        if (APPLY) { recordCreate('phase', phaseId); phases.insertOne(phase); }
      }
      const update = { phaseId, type: stage.type || stageType(stage, type),
        step: stage.step == null ? stageStep(stage) : stage.step,
        displayOrder: stage.displayOrder == null ? stageDisplayOrder(stage) : stage.displayOrder };
      counters.stages += 1;
      print(`UPDATE stage ${stage._id}:`); printjson(update);
      if (APPLY) { backup('stage', stage); stages.updateOne({ _id: stage._id }, { $set: update }); }
    }

    selections.find({ stageId: stage._id }).forEach((selection) => {
      if (selection.registeredSourceId) return;
      const key = sourceKey(selection);
      let registered = registeredSources.findOne({ seasonId: season._id, sourceEntityId: selection.sourceEntityId });
      const plannedId = `migrated:source:${stablePart(season._id)}:${stablePart(selection.sourceType)}:${stablePart(key)}`;
      registered = registered || plannedSources.get(plannedId);
      const registeredId = registered ? registered._id : plannedId;
      if (!registered) {
        registered = { _id: registeredId, seasonId: season._id, leagueSystemId: season.leagueSystemId,
          sourceEntityId: selection.sourceEntityId, sourceType: selection.sourceType, game: selection.game,
          platform: selection.platform, ruleset: selection.ruleset,
          collectionEnabled: season.isCollected !== false };
        counters.sources += 1;
        print(`CREATE registeredSource ${registeredId}: ${key}`);
        plannedSources.set(registeredId, registered);
        if (APPLY) { recordCreate('registeredSource', registeredId); registeredSources.insertOne(registered); }
      }
      counters.selections += 1;
      print(`UPDATE stageSource ${selection._id}: registeredSourceId=${registeredId}`);
      if (APPLY) { backup('stageSource', selection); selections.updateOne({ _id: selection._id }, { $set: { registeredSourceId: registeredId } }); }

      if (registered.collectionEnabled && registered.sourceEntityId && registered.sourceType
          && !dataCollections.findOne({ _id: registered.sourceEntityId }) && !plannedCollections.has(key)) {
        counters.collections += 1;
        plannedCollections.add(key);
        print(`CREATE dataCollection for ${key}`);
        if (APPLY) { recordCreate('dataCollection', registered.sourceEntityId); dataCollections.insertOne({ _id: registered.sourceEntityId, collectionType: registered.sourceType }); }
      }
    });
  });
});

print('Migration summary:');
printjson(counters);
if (APPLY) print(`Original changed documents backed up in ${BACKUP_COLLECTION} with runId ${RUN_ID}.`);
else print('Dry run complete; database unchanged.');
