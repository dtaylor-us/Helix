export const GLOSSARY = {
  Transformation: {
    definition: 'A meaningful area of personal change you want to explore over time—not a task you must complete perfectly.',
    guidance: 'Use one to hold a direction such as becoming calmer during conflict. Helix connects your experiments, reflections, evidence, and learning back to this direction.',
    example: 'Example: “Respond to difficult feedback with curiosity.”',
  },
  Experiment: {
    definition: 'A small, time-bounded behavior you try in order to learn, rather than prove that you can succeed.',
    guidance: 'Make it specific enough to try soon. Name what you will do, what you hope to learn, and what you will notice afterward. An unexpected result is still useful.',
    example: 'Example: “In my next tense meeting, pause for one breath before answering.”',
  },
  Reflection: {
    definition: 'Your account of what happened while trying an experiment and what the experience showed you.',
    guidance: 'Describe what you did, noticed, felt, or found surprising. You do not need a polished insight; concrete details create better evidence than judging the attempt as a success or failure.',
    example: 'Example: “I paused, noticed my shoulders relax, and asked a clarifying question.”',
  },
  Evidence: {
    definition: 'A specific observation that supports or challenges a belief you are examining.',
    guidance: 'Evidence is not a verdict about you. Record what happened, identify where it came from, and say whether it strengthens or weakens the belief. Contradictory evidence is valuable.',
    example: 'Example: “I asked for clarification and the conversation stayed calm” challenges “If I pause, I will look incompetent.”',
  },
  Wisdom: {
    definition: 'A principle or lesson you deliberately choose to keep because your experience has given it meaning.',
    guidance: 'Wisdom stays connected to the reflections or retrospectives that shaped it. Keep the statement useful and revisable rather than treating it as permanent truth.',
    example: 'Example: “A curious pause can make difficult feedback easier to use.”',
  },
  'Suggested Small Action': {
    definition: 'An optional next step generated from your current experiment and recent context.',
    guidance: 'Treat it as a draft. You can accept it, dismiss it, or replace it with something smaller. Helix will not treat a suggestion as something you committed to until you choose it.',
    example: 'Example: “Ask one clarifying question in your next feedback conversation.”',
  },
  Memory: {
    definition: 'A durable fact or pattern about you that Helix may use as future context only after you review it.',
    guidance: 'AI-derived memories remain proposals. Check the statement and its source before accepting it; you can edit, reject, or delete it. A memory is context, not a diagnosis or permanent fact.',
    example: 'Example: “I prefer a moment to think before responding to complex feedback.”',
  },
} as const

export type GlossaryTerm = keyof typeof GLOSSARY
