function capitalize(s) {
  return s[0].toUpperCase() + s.slice(1);
}

const prettyPrint = (text, optionalPrefixDivider) => {
  if (!text) return text;
  let myText = text;
  if (optionalPrefixDivider !== null) {
    myText = myText.split(optionalPrefixDivider).pop();
  }
  // snake_case to CamelCase
  myText = myText.replace(/[^a-zA-Z0-9]+(.)/g, (m, chr) => chr.toUpperCase());
  // camelCase to  Camel Case With Space
  return capitalize(
    myText
      .split(/([A-Z][a-z]+)/)
      .filter(Boolean)
      .join(' ')
  );
};

export default prettyPrint;
