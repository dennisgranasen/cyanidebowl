/* global db, print, process */

// Set MIGRATION_RUN_ID to the run printed by the apply migration.
// Read-only by default; set MIGRATION_APPLY=true to perform the rollback.
const APPLY = String(process.env.MIGRATION_APPLY || '').toLowerCase() === 'true';
const RUN_ID = process.env.MIGRATION_RUN_ID;
const BACKUP_COLLECTION = 'leagueStructureMigrationBackup';

if (!RUN_ID) throw new Error('MIGRATION_RUN_ID is required');

const backups = db.getCollection(BACKUP_COLLECTION).find({ runId: RUN_ID }).sort({ _id: -1 }).toArray();
print(`League structure rollback ${RUN_ID}: ${APPLY ? 'APPLY' : 'DRY RUN'}`);
print(`${backups.length} operation(s) found.`);

backups.forEach((backup) => {
  if (backup.operation === 'CREATE') {
    print(`DELETE created ${backup.collection} ${EJSON.stringify(backup.documentId)}`);
    if (APPLY) db.getCollection(backup.collection).deleteOne({ _id: backup.documentId });
  } else if (backup.operation === 'UPDATE' && backup.document) {
    print(`RESTORE ${backup.collection} ${EJSON.stringify(backup.document._id)}`);
    if (APPLY) db.getCollection(backup.collection).replaceOne({ _id: backup.document._id }, backup.document, { upsert: true });
  }
});

print(APPLY ? 'Rollback complete.' : 'Rollback dry run complete; database unchanged.');
