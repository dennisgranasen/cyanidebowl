const formatAsNumber = (value) => {
    return value !== null ? value.toLocaleString("en-UK") : "-";
}

export default formatAsNumber;
