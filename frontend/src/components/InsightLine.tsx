type InsightLineProps = {
  text: string;
};

export function InsightLine({ text }: InsightLineProps) {
  return (
    <p className="rounded-lg border border-zinc-200 bg-zinc-50 px-3 py-2 text-sm italic text-zinc-600 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-400">
      인사이트: {text}
    </p>
  );
}
