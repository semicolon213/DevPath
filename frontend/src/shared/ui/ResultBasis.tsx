import { Fragment, type ReactNode } from "react";

export type BasisItem = { label: string; value: ReactNode };

// One page-end basis line for official-result pages (DESIGN §4.3, D3). Shows returned values only.
export function ResultBasis({ items }: { items: BasisItem[] }) {
  return <footer className="result-basis">{items.map((item, index) => <Fragment key={item.label}>{index > 0 ? " · " : null}{item.label} {item.value}</Fragment>)}</footer>;
}
