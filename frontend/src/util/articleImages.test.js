import { pasteArticleImages, dropArticleImages, validArticleImage } from './articleImages';

const screenshot = { type: 'image/png', size: 1200 };
test('clipboard screenshot is uploaded while ordinary text paste stays with Tiptap', () => {
  const upload = jest.fn();
  const event = { clipboardData: { files: [screenshot] }, preventDefault: jest.fn() };
  expect(pasteArticleImages(event, upload)).toBe(true);
  expect(upload).toHaveBeenCalledWith([screenshot]);
  expect(event.preventDefault).toHaveBeenCalledTimes(1);
  expect(pasteArticleImages({ clipboardData: { files: [] } }, upload)).toBe(false);
});
test('dropping multiple files inserts images at the drop position', () => {
  const jpeg = { type: 'image/jpeg', size: 400 };
  const upload = jest.fn();
  const event = { dataTransfer: { files: [screenshot, { type: 'text/plain' }, jpeg] }, clientX: 20, clientY: 30, preventDefault: jest.fn() };
  const view = { posAtCoords: jest.fn(() => ({ pos: 12 })) };
  expect(dropArticleImages(view, event, false, upload)).toBe(true);
  expect(view.posAtCoords).toHaveBeenCalledWith({ left: 20, top: 30 });
  expect(upload).toHaveBeenCalledWith([screenshot, jpeg], 12);
});
test('moving an existing image does not upload a duplicate', () => {
  const upload = jest.fn();
  expect(dropArticleImages({}, { dataTransfer: { files: [screenshot] } }, true, upload)).toBe(false);
  expect(upload).not.toHaveBeenCalled();
});
test('unsupported, empty and oversized images fail before upload', () => {
  expect(validArticleImage(screenshot)).toBe(true);
  expect(validArticleImage({ type: 'image/svg+xml', size: 200 })).toBe(false);
  expect(validArticleImage({ type: 'image/png', size: 0 })).toBe(false);
  expect(validArticleImage({ type: 'image/jpeg', size: 10 * 1024 * 1024 + 1 })).toBe(false);
});
