import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import Underline from '@tiptap/extension-underline';
import TextAlign from '@tiptap/extension-text-align';

export const articleEditorExtensions = () => [
  StarterKit.configure({ heading: { levels: [1, 2, 3] } }), Image,
  Link.configure({ openOnClick: false }), Underline,
  TextAlign.configure({ types: ['heading', 'paragraph'] }),
];
