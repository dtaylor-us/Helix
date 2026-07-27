// Progressive reflection questions shown on Today, in order.
// Keeping these as data (rather than inline JSX strings) means the question
// set has one place to change, and keeps TodayPage focused on rendering
// rather than authoring copy.
export interface ReflectionFollowUpQuestion {
  id: 'noticed' | 'evidenceNoted' | 'surprise'
  prompt: string
  placeholder: string
}

export const REFLECTION_FOLLOW_UP_QUESTIONS: ReflectionFollowUpQuestion[] = [
  {
    id: 'noticed',
    prompt: 'What did you notice internally?',
    placeholder: 'e.g. My shoulders were tense before I paused',
  },
  {
    id: 'evidenceNoted',
    prompt: 'What evidence did this give you?',
    placeholder: 'e.g. The conversation stayed calmer than usual',
  },
  {
    id: 'surprise',
    prompt: 'Did anything surprise you?',
    placeholder: "e.g. It felt more natural than I expected by the second try",
  },
]
