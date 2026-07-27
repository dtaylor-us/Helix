// Definitions are sourced verbatim from docs/product/glossary.md.
// Keep this file in sync with that document; do not invent new wording here.
export const GLOSSARY = {
  Transformation: 'meaningful personal change area over time.',
  Experiment: 'time-bounded behavior attempt with learning criteria.',
  Reflection: 'user account of what happened and what was learned.',
  Evidence: 'observation supporting or challenging a belief.',
  Wisdom: 'user-accepted principle linked to source records.',
  'Suggested Small Action': 'optional practical next step from deterministic rules or AI.',
} as const

export type GlossaryTerm = keyof typeof GLOSSARY
