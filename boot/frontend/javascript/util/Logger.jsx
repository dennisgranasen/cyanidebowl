/* eslint-disable no-console */
/* eslint-disable class-methods-use-this */

function getStack(message) {
  try {
    throw new Error(message);
  } catch (e) {
    return e.stack;
  }
}

const LOG_LEVELS = ["TRACE", "DEBUG", "DATA", "INFO", "WARN", "ERROR"];

export class Logger {
  constructor(name) {
    this.name = name;
  }

  trace(fmt, ...args) {
    this.log(console.trace, "TRACE", fmt, ...args);
  }

  debug(fmt, ...args) {
    this.log(console.debug, "DEBUG", fmt, ...args);
  }

  info(fmt, ...args) {
    this.log(console.info, "INFO", fmt, ...args);
  }

  warn(fmt, ...args) {
    this.log(console.warn, "WARN", fmt, ...args);
  }

  error(fmt, ...args) {
    this.log(console.error, "ERROR", fmt, ...args);
  }

  data(obj) {
    this.log(console.info, "DATA", "%o", obj);
  }

  getStacktraceIfLevelAbove(level, message) {
    if (LOG_LEVELS.indexOf(level) >= LOG_LEVELS.indexOf("WARN"))
      return getStack(message);
    return undefined;
  }

  log(consoleLogFunc, level, fmt, ...args) {
    if (typeof fmt !== "string") {
      fmt = "%o";
    }

    consoleLogFunc.call(global.console, fmt, ...args);
  }
}

const logger = new Logger("")
export default logger;
