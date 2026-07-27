const MESSAGES: Record<string, string> = {
  Settings: "General settings aren't available in this build yet. AI provider, privacy, and export settings are listed below.",
  Privacy: "Privacy controls (what's stored, what's shared with AI, and how to delete it) aren't available in this build yet.",
  'AI Settings': "AI provider setup isn't available in this build yet. Helix works fully without AI in the meantime.",
  'Export Settings': "Exporting your data isn't available in this build yet.",
}

export function PlaceholderPage({ title }: { title: string }) {
  const message = MESSAGES[title] ?? `${title} isn't available in this build yet.`

  return (
    <section className="card">
      <h2>{title}</h2>
      <p>{message}</p>
    </section>
  )
}
