import { expect, test } from '@jest/globals';
import prettyPrint from './prettyPrint';

test('Pretty print works with camelCase', () => {
  expect(prettyPrint('helloThere')).toBe('Hello There');
});

test('Pretty print removes prefix', () => {
  expect(prettyPrint('prefix_helloThere', '_')).toBe('Hello There');
});

test('Pretty print works with snake case', () => {
  expect(prettyPrint('hello_there')).toBe('Hello There');
});

test('Pretty print removes leading space', () => {
  expect(prettyPrint(' hello_there')).toBe('Hello There');
});

test('Pretty print works with mixed snake and camel case', () => {
  expect(prettyPrint(' hello_thereYouAre')).toBe('Hello There You Are');
});

test('Pretty print works with "null" or "undefined"', () => {
  expect(prettyPrint(null)).toBeNull();
  expect(prettyPrint(undefined)).toBeUndefined();
});

test('Pretty print ignores "null" and "undefined" as optionalPrefixSeperator', () => {
  expect(prettyPrint('hello_thereYouAre', null)).toBe('Hello There You Are');
  expect(prettyPrint('hello_thereYouAre', undefined)).toBe('Hello There You Are');
});
