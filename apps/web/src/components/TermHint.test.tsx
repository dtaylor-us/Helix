import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TermHint } from './TermHint'

describe('TermHint', () => {
  it('progressively reveals definition, guidance, and an example', () => {
    render(<TermHint term="Evidence" />)

    const disclosure = screen.getByText('What is evidence?')
    expect(disclosure.closest('details')).not.toHaveAttribute('open')

    fireEvent.click(disclosure)

    expect(disclosure.closest('details')).toHaveAttribute('open')
    expect(screen.getByText(/Evidence is not a verdict about you/i)).toBeInTheDocument()
    expect(screen.getByText(/challenges “If I pause/i)).toBeInTheDocument()
  })
})
