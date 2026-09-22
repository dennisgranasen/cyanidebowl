export const plainTextToArticleHtml = text => `<p>${String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')}</p>`;
export const hasArticleContent = ({ body, bodyHtml }) => Boolean(body?.trim() || /<img\s/i.test(bodyHtml || ''));
