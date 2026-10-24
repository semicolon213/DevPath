import { EvidenceItem, type EvidenceItemValue } from "../../../shared/ui/EvidenceItem";
import { evidenceTypeLabel } from "../../../shared/ui/labels";

export type ResolvedEvidence = EvidenceItemValue[];

// scope keeps footnote ids unique when several panels on one page cite the same evidence.
export function EvidenceMarkers({ references, allReferences, evidence, scope, label = "근거" }: {
  references: string[];
  allReferences: string[];
  evidence: ResolvedEvidence;
  scope: string;
  label?: string;
}) {
  return <>{references.map(reference => {
    const number = allReferences.indexOf(reference) + 1;
    const resolved = resolve(reference, evidence);
    const name = resolved?.evidenceType ? `${evidenceTypeLabel(resolved.evidenceType)} ${resolved.sourceReference}` : `근거 ID ${resolved?.sourceReference ?? reference}`;
    return <a className="evidence-marker" key={reference} href={`#footnote-${scope}-${encodeURIComponent(reference)}`} aria-label={`${label} ${number}: ${name}`}>[{number}]</a>;
  })}</>;
}

export function EvidenceFootnotes({ references, evidence, scope, analysisId, label = "근거" }: {
  references: string[];
  evidence: ResolvedEvidence;
  scope: string;
  analysisId?: string | null;
  label?: string;
}) {
  if (!references.length) return null;
  return <section className="ai-generated-panel__evidence" aria-label={label}><h3>{label}</h3><ol>
    {references.map((reference, index) => {
      const resolved = resolve(reference, evidence);
      const footnoteId = `${scope}-${encodeURIComponent(reference)}`;
      return <EvidenceItem key={reference} variant="footnote" number={index + 1} evidence={resolved
        ? { ...resolved, evidenceId: footnoteId }
        : { evidenceId: footnoteId, sourceReference: reference }}
        linkTo={analysisId && resolved?.evidenceId ? `/analyses/${analysisId}#evidence-${encodeURIComponent(resolved.evidenceId)}` : undefined} />;
    })}
  </ol></section>;
}

function resolve(reference: string, evidence: ResolvedEvidence) {
  return evidence.find(item => item.evidenceId === reference || item.sourceReference === reference);
}
