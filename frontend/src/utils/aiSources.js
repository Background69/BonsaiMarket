const references = new Set([
  'https://www.rhs.org.uk/plants/types/trees/bonsai',
  'https://www.extension.umd.edu/resource/diagnose-indoor-plant-problems',
  'https://extension.umn.edu/garden-and-home/yard-and-garden/yard-and-garden-insects/insects-on-indoor-plants',
]);

export function referenceUrl(source) {
  return source?.type === 'FAQ' && Number.isSafeInteger(source.id) && source.id > 0
    && references.has(source.url) ? source.url : null;
}
