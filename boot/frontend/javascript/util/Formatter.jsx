const numberFormat = new Intl.NumberFormat("en-GB");
const dateFormat = new Intl.DateTimeFormat("en-GB", {dateStyle: 'short', timeStyle: 'short'});
const formatAsNumber = (value) => {
    return value !== null ? numberFormat.format(value) : "-";
}

const formatAsDate = (date) => {
    return date && date !== null ? dateFormat.format(new Date(date)) : "";
}


export default {
    formatAsNumber: formatAsNumber,
    formatAsDate: formatAsDate,
};
