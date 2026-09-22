import { plainTextToArticleHtml, hasArticleContent } from './articleContent';
import { canGenerateArticleImage } from './articleImages';

test('legacy match reports retain plain text and line breaks without interpreting tags', () => {
  expect(plainTextToArticleHtml('A < B & C\nNext')).toBe('<p>A &lt; B &amp; C<br>Next</p>');
});
test('image-only match articles count as content', () => {
  expect(hasArticleContent({ body: '', bodyHtml: '<p></p><img src="/asset.png">' })).toBe(true);
  expect(hasArticleContent({ body: '', bodyHtml: '<p></p>' })).toBe(false);
});
test('image generation remains available after deleting all body content', () => {
  expect(canGenerateArticleImage({ title: 'A headline', body: '' })).toBe(true);
  expect(canGenerateArticleImage({ prompt: 'A stadium', body: '' })).toBe(true);
  expect(canGenerateArticleImage({ matchId: '3_match', body: '' })).toBe(true);
  expect(canGenerateArticleImage({ title: '', body: ' ', prompt: '' })).toBe(false);
});
