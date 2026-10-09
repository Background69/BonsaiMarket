// Show selected entries from the existing FAQ, which is explicitly an MVP reference.
import faqSource from '../../../src/main/resources/ai/bonsai-faq.md?raw';
const entries = faqSource.split(/^## /m).slice(1).map(section => {
  const [heading, ...body] = section.trim().split('\n');
  const match = heading.match(/^(\d+)\.\s*(.+)/);
  return {id: Number(match?.[1]), title: match?.[2] || heading, text: body.join('\n').trim()};
});
export const homeFaqs = [1, 4, 8, 14, 25].map(id => entries.find(entry => entry.id === id)).filter(Boolean);
export const homeKnowledge = [2, 5, 22].map(id => entries.find(entry => entry.id === id)).filter(Boolean);
