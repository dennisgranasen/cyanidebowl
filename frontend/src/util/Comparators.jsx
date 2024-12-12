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

export default {
  compareAsDatesAsc,
  compareAsDatesDesc,
};
