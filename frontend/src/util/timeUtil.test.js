import { expect, test } from '@jest/globals';
import timeUtil from './timeUtil';

test('duration works for start and end date', () => {
  expect(timeUtil.durationInMillis('2024-12-12 00:00:00', '2024-12-12 00:00:02')).toBe(2000);
});

test('duration works for start date only', () => {
  const now = new Date();
  const before = new Date(now.getTime() - 2000);
  expect(timeUtil.durationInMillis(before.toISOString())).toBeGreaterThanOrEqual(2000);
});

test('duration returns 0 for no date', () => {
  expect(timeUtil.durationInMillis()).toBe(0);
});
