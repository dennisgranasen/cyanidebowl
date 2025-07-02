/*
function getMatchOrContestUuid(contest) {
  return contest?.matchId ?? contest?.matchUuid ?? contest?.contestUuid ?? null;
}

function getMatchUuid(match) {
  return match?.matchId ?? match?.matchUuid ?? null;
}

function getContestUuid(contest) {
  return contest?.contestUuid ?? null;
}
*/
const getDateFromUUID = (uuid) => {
  if (!uuid) return null;
  const timestampHex = `${uuid.substr(15, 3)}${uuid.substr(9, 4)}${uuid.substr(0, 8)}`;
  const timestamp = parseInt(timestampHex, 16);
  let seconds = timestamp / (10 * 1000 * 1000);
  // we can convert this to unix time by subtracting the number of seconds between that date and January 1, 1970
  seconds -= 141427 * 24 * 60 * 60;
  return new Date(seconds * 1000);
};
/*
const compareAsDatesAsc = (dateStr1, dateStr2) => {
  if (!dateStr1 && !dateStr2) return 0;
  if (!dateStr1) return 1;
  if (!dateStr2) return -1;

  return new Date(dateStr1).getTime() - new Date(dateStr2).getTime();
};
*/
const compareAsDatesDesc = (dateStr1, dateStr2) => {
  if (!dateStr1 && !dateStr2) return 0;
  if (!dateStr1) return 1;
  if (!dateStr2) return -1;

  return new Date(dateStr2).getTime() - new Date(dateStr1).getTime();
};
/*
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

const compareMatchesByMatchUuidAsDatesDesc = (match1, match2) => {
  return compareUUIDsAsDatesDesc(getMatchUuid(match1), getMatchUuid(match2));
};
*/

const compareContestsByContestIdsAsDateDesc = (contest1, contest2) => {
  const id1 = contest1?.id?.value || contest1?.id;
  const id2 = contest2?.id?.value || contest2?.id;
  if (!id1 && !id2) {
    console.warn('Both contests have no id to compare:', contest1, contest2);
    return 0;
  }
  if (!id1) {
    console.warn('Contest 1 has no id:', contest1);
    return 1; // id1 is null, so id2 is "greater"
  }
  if (!id2) {
    console.warn('Contest 2 has no id:', contest2);
    return -1; // id2 is null, so id1 is "greater"
  }
  const date1 = getDateFromUUID(id1);
  const date2 = getDateFromUUID(id2);
  return compareAsDatesDesc(date1, date2);
}

const compareContestDatesDesc = (date1, date2) => {
  if (!date1 && !date2) {
    console.warn('Both contests have no date to compare');
    return 0;
  } else if (!date1) {
    console.warn('Contest 1 has no date');
    return 1; // date1 is null, so date2 is "greater"
  } else if (!date2) {
    console.warn('Contest 2 has no date');
    return -1; // date2 is null, so date1 is "greater"
  }
  return compareAsDatesDesc(date1, date2);
}

const compareMatchesByDateDesc = (match1, match2) => {
  const date1 = match1?.finished || match1?.started;
  const date2 = match2?.finished || match2?.started;
  if (!date1 && !date2) {
    console.warn('Both matches have no date to compare:', match1, match2);
    return 0;
  } else if (!date1) {
    console.warn('Match 1 has no date:', match1);
    return 1; // date1 is null, so date2 is "greater"
  } else if (!date2) {
    console.warn('Match 2 has no date::', match2);
    return -1; // date2 is null, so date1 is "greater"
  }
  return compareAsDatesDesc(date1, date2);
}

const compareMatchesByDateAsc = (match1, match2) => {
  return -compareMatchesByDateDesc(match1, match2);
}

const compareContestsByDateDesc = (contest1, contest2) => {
  const date1 = contest1?.matchDate;
  const date2 = contest2?.matchDate;
  return compareContestDatesDesc(date1, date2) || compareContestsByContestIdsAsDateDesc(contest1, contest2);
}

const compareContestsByDateAsc = (contest1, contest2) => {
  return -compareContestsByDateDesc(contest1, contest2);
}

const compareContestsByDateWithMatchAsFallbackDesc = (contest1, contest2) => {
  const date1 = contest1?.matchDate || contest1?.match?.started || contest1?.match?.finished;
  const date2 = contest2?.matchDate || contest2?.match?.started || contest2?.match?.finished;
  return compareContestDatesDesc(date1, date2);
}

const compareContestsByDateWithMatchAsFallbackAsc = (contest1, contest2) => {
  return -compareContestsByDateWithMatchAsFallbackDesc(contest1, contest2);
}

export default {
  //compareContestsByMatchOrContestUuidAsDatesAsc,
  //compareContestsByMatchOrContestUuidAsDatesDesc,
  //compareContestsByContestUuidAsDatesAsc,
  //compareContestsByContestUuidAsDatesDesc,
  //compareMatchesByMatchUuidAsDatesDesc,
  compareMatchesByDateDesc,
  compareMatchesByDateAsc,
  compareContestsByDateDesc,
  compareContestsByDateAsc,
  compareContestsByDateWithMatchAsFallbackDesc,
  compareContestsByDateWithMatchAsFallbackAsc,
};
