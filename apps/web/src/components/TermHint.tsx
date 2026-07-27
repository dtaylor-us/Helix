import { GLOSSARY, type GlossaryTerm } from '../content/glossary'

export function TermHint({ term }: { term: GlossaryTerm }) {
  return (
    <details className="term-hint">
      <summary>What&rsquo;s {startsWithVowel(term) ? 'an' : 'a'} {term.toLowerCase()}?</summary>
      <p className="muted">{GLOSSARY[term]}</p>
    </details>
  )
}

function startsWithVowel(term: string): boolean {
  return /^[aeiou]/i.test(term)
}
