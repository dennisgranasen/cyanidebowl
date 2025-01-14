function capitalizeEachWord(text) {
  text = text.replace(/([(])(.)/g, (m, char, first) => `${char}${first.toUpperCase()}`);
  return text
    .split(' ')
    .map((str) => str.replace(/(^.)(.*)/, (m, first, rest) => `${first.toUpperCase()}${rest}`))
    .join(' ');
}

function removeUnderscores(myText) {
  return myText.replace(/_/g, ' ');
}

function removeDots(myText) {
  return myText.replace(/\./g, ' ');
}

function removeLeadingSpaces(myText) {
  return myText.replace(/^ +/, '');
}

function splitCamelCase(myText) {
  return myText.replace(/([a-z])([A-Z])/g, (m, first, second) => `${first} ${second}`);
}

function removeFirstElementSeparatedBy(myText, optionalPrefixSeparator) {
  if (optionalPrefixSeparator !== null) {
    myText = myText.split(optionalPrefixSeparator).pop();
  }
  return myText;
}

const prettyPrint = (text, optionalPrefixSeparator) => {
  if (!text) return text;
  let myText = removeFirstElementSeparatedBy(text, optionalPrefixSeparator);
  myText = removeUnderscores(myText);
  myText = removeDots(myText);
  myText = capitalizeEachWord(myText);
  myText = removeLeadingSpaces(myText);
  return splitCamelCase(myText);
};

export default prettyPrint;
