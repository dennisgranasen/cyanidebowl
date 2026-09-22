export const IMAGE_REQUEST_TIMEOUT_MS = 5 * 60 * 1000;

export async function boundedImageRequest(request, timeoutMs = IMAGE_REQUEST_TIMEOUT_MS) {
  const controller = new AbortController();
  let timer;
  try {
    return await Promise.race([
      Promise.resolve().then(() => request(controller.signal)),
      new Promise((resolve, reject) => {
        timer = setTimeout(() => {
          const error = new Error('Image generation timed out');
          error.code = 'IMAGE_TIMEOUT';
          reject(error);
          controller.abort();
        }, timeoutMs);
      }),
    ]);
  } finally {
    clearTimeout(timer);
  }
}
