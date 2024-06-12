const numberFormat = new Intl.NumberFormat('en-GB');
const dateFormatOptions = {
  year: '2-digit',
  month: '2-digit',
  day: '2-digit',
  hour12: false,
  hour: 'numeric',
  minute: 'numeric',
};

const formatAsNumber = (value) => {
  return value !== null ? numberFormat.format(value) : '-';
};

const asUTCDate = (date) => {
  const [year, month, day, hours, minutes, seconds] = date.split(/-|\s|:/).map((c) => parseInt(c, 10));
  return new Date(Date.UTC(year, month - 1, day, hours, minutes, seconds, 0));
};

const formatAsDate = (date) => {
  if (!date) return '';

  const utcDate = asUTCDate(date);
  return utcDate.toLocaleString([], dateFormatOptions);
};

const formatAsDuration = (startDate, endDate) => {
  if (!startDate) return '';
  const endTime = endDate ? asUTCDate(endDate).getTime() : Date.now();

  const millis = endTime - asUTCDate(startDate).getTime();
  const minutes = Math.floor(millis / 1000 / 60);
  const hours = Math.floor(minutes / 60);
  const restMinutes = minutes % 60;
  const hoursText = hours > 0 ? `${hours} hour${hours > 1 ? 's' : ''}` : '';
  const minutesText = restMinutes > 0 ? `${restMinutes} minute${minutes > 1 ? 's' : ''}` : '';
  return `${hoursText}${hoursText && minutesText ? ', ' : ''}${minutesText}`;
};

export default {
  formatAsNumber,
  formatAsDate,
  formatAsDuration,
};
