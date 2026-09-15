import { boundedImageRequest } from './boundedImageRequest';

afterEach(() => jest.useRealTimers());

test('stops waiting and aborts even when the request never settles', async () => {
  jest.useFakeTimers();
  let signal;
  const result = boundedImageRequest(value => { signal = value; return new Promise(() => {}); }, 100);
  const check = expect(result).rejects.toMatchObject({ code: 'IMAGE_TIMEOUT' });
  await Promise.resolve();
  jest.advanceTimersByTime(100);
  await check;
  expect(signal.aborted).toBe(true);
});

test('returns successful results and clears its timer', async () => {
  jest.useFakeTimers();
  await expect(boundedImageRequest(async () => ({ url: '/image.png' }), 100)).resolves.toEqual({ url: '/image.png' });
  expect(jest.getTimerCount()).toBe(0);
});
