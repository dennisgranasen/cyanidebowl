const compareAsDates = (dateStr1, dateStr2) => {
  if ((!dateStr1 || dateStr1 === null) && (!dateStr2 || dateStr2 === null)) return 0;
  if (!dateStr1 || dateStr1 === null) return 1;
  if (!dateStr2 || dateStr2 === null) return -1;

  return new Date(dateStr1).getTime() - new Date(dateStr2).getTime();
};

export default {
  compareAsDates,
};
