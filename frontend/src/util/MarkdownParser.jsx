const parseMarkdownPrefixingLinks = (text, prefix) => {
    const markdownLinkRegex = new RegExp(/(\[[^\]]*\])\((?:https?|mailto\:\/\/){0}([^\:)]*)\)/, 'g');
    return text.replace(markdownLinkRegex, `$1(${prefix}$2)`);
}

export default parseMarkdownPrefixingLinks;
