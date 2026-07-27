// Definitions are sourced verbatim from docs/product/glossary.md.
// Keep this file in sync with that document; do not invent new wording here.
export const GLOSSARY = {
  Transformation: 'A meaningful personal change area over time.',
  Experiment: 'A time-bounded behavior attempt with learning criteria.',
  Reflection: 'Your account of what happened and what you learned.',
  Evidence: 'An observation supporting or challenging a belief.',
  Wisdom: 'A user-accepted principle linked to the records that support it.',
  'Suggested Small Action': 'An optional practical next step, offered by deterministic rules or AI.',
} as const

export type GlossaryTerm = keyof typeof GLOSSARY
