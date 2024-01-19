function capitalize(s) {
  return s[0].toUpperCase() + s.slice(1);
}

const prettyPrint = (text, optionalPrefixDivider) => {
  if (!text || text === null) return text;
  let myText = text;
  if (optionalPrefixDivider !== null) {
    myText = myText.split(optionalPrefixDivider).pop();
  }

  return capitalize(
    myText
      .split(/([A-Z][a-z]+)/)
      .filter(Boolean)
      .join(' ')
  );
};

export default prettyPrint;
