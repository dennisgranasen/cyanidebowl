import { expect, test } from '@jest/globals';
import hashCode from './hashCode';

test('hash', () => {
  expect(hashCode('Hallo')).toBe(69490486);
  expect(hashCode('Ha', 'llo')).toBe(2152298460);
  expect(hashCode('Ha,llo')).toBe(2152298460);
});
