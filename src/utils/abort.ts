/**
 * React Native doesn't support AbortSignal.timeout, so we need to implement it ourselves.
 *
 * @see https://github.com/mo/abortcontroller-polyfill/blob/master/src/abortsignal-ponyfill.js#L40-L46
 */
export function getAbortTimeout(milliseconds: number) {
  const controller = new AbortController();

  setTimeout(() => controller.abort(), milliseconds);

  return controller.signal;
}

/**
 * AbortSignal.any polyfill for React Native
 *
 * @see https://github.com/mo/abortcontroller-polyfill/blob/master/src/abortsignal-ponyfill.js#L58-L78
 */
export function anyAbort(signals: AbortSignal[]) {
  const controller = new AbortController();

  function abort() {
    controller.abort();
    clean();
  }
  function clean() {
    for (const signal of signals) signal.removeEventListener('abort', abort);
  }

  for (const signal of signals) {
    if (signal.aborted) {
      controller.abort();
      break;
    } else {
      signal.addEventListener('abort', abort);
    }
  }

  return controller.signal;
}
