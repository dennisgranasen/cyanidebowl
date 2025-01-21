const asUTCDate = (date) => {
  const [year, month, day, hours, minutes, seconds] = date.split(/-|\s|T|:|\./).map((c) => parseInt(c, 10));
  return new Date(Date.UTC(year, month - 1, day, hours, minutes, seconds, 0));
};

const durationInMillis = (startDate, endDate) => {
  if (!startDate) return 0;
  const endTime = endDate ? asUTCDate(endDate).getTime() : Date.now();

  return endTime - asUTCDate(startDate).getTime();
};

export default {
  durationInMillis,
  asUTCDate,
};
