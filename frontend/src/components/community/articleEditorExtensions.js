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

export const articleEditorExtensions = () => [
  StarterKit.configure({ heading: { levels: [1, 2, 3] } }), EditorialImage,
  Link.configure({ openOnClick: false }), Underline,
  TextAlign.configure({ types: ['heading', 'paragraph'] }),
];
