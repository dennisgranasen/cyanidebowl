import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import Underline from '@tiptap/extension-underline';
import TextAlign from '@tiptap/extension-text-align';

const EditorialImage = Image.extend({
  addAttributes() {
    return { ...this.parent?.(), editorialImageId: {
      default: null,
      parseHTML: element => element.getAttribute('data-editorial-image'),
      renderHTML: attributes => attributes.editorialImageId ? { 'data-editorial-image': attributes.editorialImageId } : {},
    } };
  },
});

const EditorialLink = Link.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      editorialMentionType: {
        default: null,
        parseHTML: element => element.getAttribute('data-editorial-mention'),
        renderHTML: attributes => attributes.editorialMentionType
          ? { 'data-editorial-mention': attributes.editorialMentionType } : {},
      },
      editorialMentionId: {
        default: null,
        parseHTML: element => element.getAttribute('data-editorial-id'),
        renderHTML: attributes => attributes.editorialMentionId
          ? { 'data-editorial-id': attributes.editorialMentionId } : {},
      },
    };
  },
  parseHTML() {
    return [
      { tag: 'a[href]' },
      { tag: 'span[data-editorial-mention][data-editorial-id]' },
    ];
  },
  renderHTML({ HTMLAttributes }) {
    if (HTMLAttributes['data-editorial-mention']) {
      const { href, target, rel, ...mentionAttributes } = HTMLAttributes;
      return ['span', { ...mentionAttributes, class: 'editorial-mention' }, 0];
    }
    return ['a', { ...this.options.HTMLAttributes, ...HTMLAttributes }, 0];
  },
});

export const articleEditorExtensions = () => [
  StarterKit.configure({ heading: { levels: [1, 2, 3] } }), EditorialImage,
  EditorialLink.configure({ openOnClick: false }), Underline,
  TextAlign.configure({ types: ['heading', 'paragraph'] }),
];
