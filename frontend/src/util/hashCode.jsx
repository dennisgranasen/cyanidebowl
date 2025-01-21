const hashCode = (...args) => {
  const concatenatedString = [].concat(args).join(',');
  let hash = 0;
  let i;
  for (i = 0; i < concatenatedString.length; i += 1) {
    const chr = concatenatedString.charCodeAt(i);
    hash = hash * 32 - hash + chr;
  }
  return hash;
};

export default hashCode;
