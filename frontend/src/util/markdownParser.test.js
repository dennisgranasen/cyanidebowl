import { expect, test } from '@jest/globals';
import parseMarkdownPrefixingLinks from './MarkdownParser';

test('parseMarkdownAdjustingLinks adjusts simple markdown links', () => {
  expect(parseMarkdownPrefixingLinks('Hello [markdown](link)', '/prefix/')).toBe('Hello [markdown](/prefix/link)');
});

test('parseMarkdownAdjustingLinks does not change http links', () => {
  expect(parseMarkdownPrefixingLinks('Hello [markdown](http://link)', '/prefix/')).toBe(
    'Hello [markdown](http://link)'
  );
});

test('parseMarkdownAdjustingLinks does not change https links', () => {
  expect(parseMarkdownPrefixingLinks('Hello [markdown](https://link)', '/prefix/')).toBe(
    'Hello [markdown](https://link)'
  );
});

test('parseMarkdownAdjustingLinks does not change mailto links', () => {
  expect(parseMarkdownPrefixingLinks('Hello [markdown](mailto:mail)', '/prefix/')).toBe(
    'Hello [markdown](mailto:mail)'
  );
});
