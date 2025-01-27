import timeUtil from './timeUtil';

const numberFormat = (locales = []) => new Intl.NumberFormat(locales);
const numberFormatPercentage = (locales = []) =>
  new Intl.NumberFormat(locales, {
    style: 'percent',
    maximumSignificantDigits: 3,
  });
const dateFormatOptions = {
  year: '2-digit',
  month: '2-digit',
  day: '2-digit',
  hour12: false,
  hour: 'numeric',
  minute: 'numeric',
};

const formatAsNumber = (value, locales = []) => {
  return value !== null && value !== undefined ? numberFormat(locales).format(value) : '-';
};

const formatAsPercentage = (value, locales = []) => {
  if (value === null || value === undefined) return '-';
  return `${numberFormatPercentage(locales).format(value)}`;
};

const formatAsDate = (date, nullRepresentation, locales = []) => {
  if (!date) return nullRepresentation;

  const utcDate = timeUtil.asUTCDate(date);
  return utcDate.toLocaleString(locales, dateFormatOptions);
};

const formatAsDuration = (startDate, endDate) => {
  if (!startDate) return '';
  const millis = timeUtil.durationInMillis(startDate, endDate);
  const minutes = Math.floor(millis / 1000 / 60);
  const hours = Math.floor(minutes / 60);
  const restMinutes = minutes % 60;
  const hoursText = hours > 0 ? `${hours} hour${hours > 1 ? 's' : ''}` : '';
  const minutesText = restMinutes > 0 ? `${restMinutes} minute${restMinutes > 1 ? 's' : ''}` : '';
  return `${hoursText}${hoursText && minutesText ? ', ' : ''}${minutesText}`;
};

const formatAsRomanNumber = (num) => {
  const lookup = { X: 10, IX: 9, V: 5, IV: 4, I: 1 };
  let roman = '';
  Object.keys(lookup).forEach((key) => {
    const value = lookup[key];
    while (num >= value) {
      roman += key;
      num -= value;
    }
  });
  return roman;
};

export default {
  formatAsNumber,
  formatAsPercentage,
  formatAsDate,
  formatAsDuration,
  formatAsRomanNumber,
};
