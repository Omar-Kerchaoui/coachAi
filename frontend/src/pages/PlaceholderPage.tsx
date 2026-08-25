interface PlaceholderPageProps {
  title: string;
  epic?: string;
}

// Marks a route that exists per the site map (docs product spec §55) but
// whose real implementation lands in a later epic — swapped out page by
// page as each epic's frontend stories complete.
export default function PlaceholderPage({ title, epic }: PlaceholderPageProps) {
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center gap-2 px-4 text-center">
      <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
      {epic && <p className="text-sm text-slate-500">Coming in {epic}.</p>}
    </div>
  );
}
