import { GLOSSARY, type GlossaryTerm } from '../content/glossary'

export function TermHint({ term }: { term: GlossaryTerm }) {
  return (
    <details className="term-hint">
      <summary>What is {term.toLowerCase()}?</summary>
      <div className="term-hint-content">
        <p>{GLOSSARY[term].definition}</p>
        <p className="muted">{GLOSSARY[term].guidance}</p>
        <p className="term-example">{GLOSSARY[term].example}</p>
      </div>
    </details>
  )
}
