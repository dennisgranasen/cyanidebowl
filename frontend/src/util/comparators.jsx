function getMatchOrContestUuid(contest) {
  if (!contest) return null;
  return contest.matchId || contest.matchUuid || contest.contestUuid;
}

function getContestUuid(contest) {
  if (!contest) return null;
  return contest.contestUuid;
}

const getDateFromUUID = (uuid) => {
  if (!uuid) return null;
  const timestampHex = `${uuid.substr(15, 3)}${uuid.substr(9, 4)}${uuid.substr(0, 8)}`;
  const timestamp = parseInt(timestampHex, 16);
  let seconds = timestamp / (10 * 1000 * 1000);
  // we can convert this to unix time by subtracting the number of seconds between that date and January 1, 1970
  seconds -= 141427 * 24 * 60 * 60;
  return new Date(seconds * 1000);
};

const compareAsDatesAsc = (dateStr1, dateStr2) => {
  if (!dateStr1 && !dateStr2) return 0;
  if (!dateStr1) return 1;
  if (!dateStr2) return -1;

  return new Date(dateStr1).getTime() - new Date(dateStr2).getTime();
};

const compareAsDatesDesc = (dateStr1, dateStr2) => {
  if (!dateStr1 && !dateStr2) return 0;
  if (!dateStr1) return 1;
  if (!dateStr2) return -1;

  return new Date(dateStr2).getTime() - new Date(dateStr1).getTime();
};

const compareUUIDsAsDatesAsc = (uuid1, uuid2) => {
  return compareAsDatesAsc(getDateFromUUID(uuid1), getDateFromUUID(uuid2));
};

const compareUUIDsAsDatesDesc = (uuid1, uuid2) => {
  return compareAsDatesDesc(getDateFromUUID(uuid1), getDateFromUUID(uuid2));
};

const compareContestsByMatchOrContestUuidAsDatesAsc = (contest1, contest2) => {
  return compareUUIDsAsDatesAsc(getMatchOrContestUuid(contest1), getMatchOrContestUuid(contest2));
};

const compareContestsByMatchOrContestUuidAsDatesDesc = (contest1, contest2) => {
  return compareUUIDsAsDatesDesc(getMatchOrContestUuid(contest1), getMatchOrContestUuid(contest2));
};

const compareContestsByContestUuidAsDatesAsc = (contest1, contest2) => {
  return compareUUIDsAsDatesAsc(getContestUuid(contest1), getContestUuid(contest2));
};

const compareContestsByContestUuidAsDatesDesc = (contest1, contest2) => {
  return compareUUIDsAsDatesDesc(getContestUuid(contest1), getContestUuid(contest2));
};

export default {
  compareContestsByMatchOrContestUuidAsDatesAsc,
  compareContestsByMatchOrContestUuidAsDatesDesc,
  compareContestsByContestUuidAsDatesAsc,
  compareContestsByContestUuidAsDatesDesc,
};
