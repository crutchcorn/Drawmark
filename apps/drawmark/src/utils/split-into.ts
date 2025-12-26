export function splitInto<T>(items: T[], size: number): T[][] {
  if (size <= 0) {
    throw new Error('Size must be positive');
  }

  const result: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    result.push(items.slice(i, i + size));
  }
  return result;
}
