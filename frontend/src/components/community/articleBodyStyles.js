// Shared typography keeps the editing canvas, preview, and published article consistent.
export const articleBodyStyles = {
  lineHeight: 1.75,
  overflowWrap: 'anywhere',
  '& p': { marginBottom: '1em' },
  '& h1': { fontSize: '2em', fontWeight: 'bold', margin: '1em 0 .5em' },
  '& h2': { fontSize: '1.5em', fontWeight: 'bold', margin: '1em 0 .5em' },
  '& h3': { fontSize: '1.2em', fontWeight: 'bold', margin: '1em 0 .5em' },
  '& ul': { listStyleType: 'disc', paddingLeft: '1.8em', marginBottom: '1em' },
  '& ol': { listStyleType: 'decimal', paddingLeft: '1.8em', marginBottom: '1em' },
  '& li p': { marginBottom: '.25em' },
  '& blockquote': { borderLeft: '3px solid', borderColor: 'gray.400', paddingLeft: '1em', margin: '1em 0', fontStyle: 'italic' },
  '& a': { color: 'blue.400', textDecoration: 'underline' },
  '& img': { maxWidth: '100%', height: 'auto', borderRadius: '6px', margin: '1em 0' },
  '& hr': { margin: '1.5em 0', borderTop: '1px solid', borderColor: 'gray.400' },
  '& pre': { background: 'rgba(128,128,128,.12)', padding: '1em', whiteSpace: 'pre-wrap' },
};
