import { Link } from "react-router-dom";

export function EvidenceReferences({ references, analysisId, label = "분석 근거", target = "evidence", showReference = false }: {
  references: string[];
  analysisId?: string | null;
  label?: string;
  target?: "evidence" | "rules";
  showReference?: boolean;
}) {
  if (!references.length) return null;
  return <ul className="ai-evidence-links" aria-label={label}>{references.map((reference, index) => <li key={`${reference}:${index}`}>
    {analysisId
      ? <Link to={`/analyses/${analysisId}#${target === "evidence" ? `evidence-${encodeURIComponent(reference)}` : "category-results-title"}`}>{label} {index + 1}</Link>
      : <span>{showReference ? `${label}: ${reference}` : `${label} ${index + 1}`}</span>}
  </li>)}</ul>;
}
