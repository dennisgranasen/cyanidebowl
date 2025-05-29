import { expect, test } from '@jest/globals';
import formatter from './formatter';

test('formatAsPercentage', () => {
  expect(formatter.formatAsPercentage(0.9, ['de'])).toBe('90 %');
  expect(formatter.formatAsPercentage(0.012, ['en'])).toBe('1.2%');
  expect(formatter.formatAsPercentage(0.7999, ['en'])).toBe('80%');
  expect(formatter.formatAsPercentage(null)).toBe('-');
  expect(formatter.formatAsPercentage(undefined)).toBe('-');
});

test('formatAsNumber', () => {
  expect(formatter.formatAsNumber(2, ['en'])).toBe('2');
  expect(formatter.formatAsNumber(2000, ['en'])).toBe('2,000');
  expect(formatter.formatAsNumber(0.7, ['en'])).toBe('0.7');
  expect(formatter.formatAsNumber(0.7544, ['en'])).toBe('0.754');
  expect(formatter.formatAsNumber(0.7545, ['en'])).toBe('0.755');
  expect(formatter.formatAsNumber(null, ['en'])).toBe('-');
  expect(formatter.formatAsNumber(undefined, ['en'])).toBe('-');
  expect(formatter.formatAsNumber(0, ['en'])).toBe('0');
});

test('formatAsDate', () => {
  const date = new Date(2024, 11, 20, 17, 15, 21);
  expect(formatter.formatAsDate(date.toISOString(), '', ['en-GB'])).toBe('20/12/24, 17:15');
  expect(formatter.formatAsDate(date.toISOString(), '', ['en-US'])).toBe('12/20/24, 17:15');
  expect(formatter.formatAsDate(date.toISOString(), '', ['de-DE'])).toBe('20.12.24, 17:15');
  expect(formatter.formatAsDate(null, '##')).toBe('##');
  expect(formatter.formatAsDate(null)).toBe(undefined);
  expect(formatter.formatAsDate(undefined, '##')).toBe('##');
  expect(formatter.formatAsDate(undefined)).toBe(undefined);
});

test('formatAsRomanNumber', () => {
  expect(formatter.formatAsRomanNumber(1)).toBe('I');
  expect(formatter.formatAsRomanNumber(2)).toBe('II');
  expect(formatter.formatAsRomanNumber(3)).toBe('III');
  expect(formatter.formatAsRomanNumber(4)).toBe('IV');
  expect(formatter.formatAsRomanNumber(5)).toBe('V');
  expect(formatter.formatAsRomanNumber(6)).toBe('VI');
  expect(formatter.formatAsRomanNumber(7)).toBe('VII');
  expect(formatter.formatAsRomanNumber(8)).toBe('VIII');
  expect(formatter.formatAsRomanNumber(9)).toBe('IX');
  expect(formatter.formatAsRomanNumber(10)).toBe('X');
  expect(formatter.formatAsRomanNumber(11)).toBe('XI');
  expect(formatter.formatAsRomanNumber(14)).toBe('XIV');
  expect(formatter.formatAsRomanNumber(16)).toBe('XVI');
  expect(formatter.formatAsRomanNumber(19)).toBe('XIX');
  expect(formatter.formatAsRomanNumber(20)).toBe('XX');
  expect(formatter.formatAsRomanNumber(39)).toBe('XXXIX');
  expect(formatter.formatAsRomanNumber(42)).toBe('XXXXII');
  expect(formatter.formatAsRomanNumber(null)).toBe('');
  expect(formatter.formatAsRomanNumber(undefined)).toBe('');
});

test('formatAsDuration', () => {
  const start = new Date(2024, 11, 20, 17, 15, 21).toISOString();
  const oneHourLater = new Date(2024, 11, 20, 18, 15, 21).toISOString();
  const twoHoursLater = new Date(2024, 11, 20, 19, 15, 21).toISOString();
  const threeHoursOneMinuteTwelveSecondsLater = new Date(2024, 11, 20, 20, 16, 33).toISOString();
  const twoMinutesAgo = new Date(new Date().getTime() - 120_000).toISOString();
  expect(formatter.formatAsDuration(null)).toBe('');
  expect(formatter.formatAsDuration(undefined)).toBe('');
  expect(formatter.formatAsDuration(null, new Date())).toBe('');
  expect(formatter.formatAsDuration(undefined, new Date())).toBe('');
  expect(formatter.formatAsDuration(start, oneHourLater)).toBe('1 hour');
  expect(formatter.formatAsDuration(start, twoHoursLater)).toBe('2 hours');
  expect(formatter.formatAsDuration(start, threeHoursOneMinuteTwelveSecondsLater)).toBe('3 hours, 1 minute');
  expect(formatter.formatAsDuration(twoMinutesAgo, null)).toBe('2 minutes');
  expect(formatter.formatAsDuration(twoMinutesAgo, undefined)).toBe('2 minutes');
});
