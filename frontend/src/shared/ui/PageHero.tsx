import type { HTMLAttributes, ReactNode } from "react";

type PageHeroProps = Omit<HTMLAttributes<HTMLElement>, "title"> & {
  title?: ReactNode;
  eyebrow?: ReactNode;
  description?: ReactNode;
  status?: ReactNode;
  actions?: ReactNode;
};

// Shared full-bleed page band (DESIGN §6.3, D8). The dashboard keeps its own large variant.
export function PageHero({ title, eyebrow, description, status, actions, className, children, ...rest }: PageHeroProps) {
  if (children) return <header {...rest} className={className ? `page-hero ${className}` : "page-hero"}>{children}</header>;
  return <header {...rest} className={className ? `page-hero ${className}` : "page-hero"}>
    <div className="page-hero__intro">
      {eyebrow ? <p className="page-hero__eyebrow">{eyebrow}</p> : null}
      {title ? <h1>{title}</h1> : null}
      {description ? <p>{description}</p> : null}
      {status ? <div className="page-hero__status">{status}</div> : null}
    </div>
    {actions ? <div className="page-hero__actions">{actions}</div> : null}
  </header>;
}
