const images = transfer => Array.from(transfer?.files || []).filter(file => file.type.startsWith('image/'));

export const canGenerateArticleImage = ({ title, body, prompt, matchId }) =>
  [title, body, prompt, matchId].some(value => Boolean(value?.trim()));

export const validArticleImage = file => ['image/png', 'image/jpeg'].includes(file.type)
  && file.size > 0 && file.size <= 10 * 1024 * 1024;

export function pasteArticleImages(event, upload) {
  const files = images(event.clipboardData);
  if (!files.length) return false;
  event.preventDefault();
  upload(files);
  return true;
}

export function dropArticleImages(view, event, moved, upload) {
  const files = images(event.dataTransfer);
  // Let Tiptap handle moving images already in the document.
  if (moved || !files.length) return false;
  event.preventDefault();
  const position = view.posAtCoords({ left: event.clientX, top: event.clientY })?.pos;
  upload(files, position);
  return true;
}
