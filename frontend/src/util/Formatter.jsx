const numberFormat = new Intl.NumberFormat('en-GB');
const dateFormat = new Intl.DateTimeFormat('en-GB', {
  year: '2-digit',
  month: '2-digit',
  day: '2-digit',
  hour12: false,
  hour: 'numeric',
  minute: 'numeric',
  timeZone: 'UTC',
});
const formatAsNumber = (value) => {
  return value !== null ? numberFormat.format(value) : '-';
};

const formatAsDate = (date) => {
  return date && date !== null ? dateFormat.format(new Date(date)) : '';
};

const formatAsDuration = (startDate, endDate) => {
  if (!startDate || !endDate) return '';

  const millis = new Date(endDate).getTime() - new Date(startDate).getTime();
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
