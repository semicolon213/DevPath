import { useEffect, useId, useRef, type RefObject } from "react";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  destructive?: boolean;
  pending?: boolean;
  error?: string;
  returnFocusFallback?: RefObject<HTMLElement | null>;
  onConfirm: () => void;
  onCancel: () => void;
};

const focusableSelector = "button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])";

function canReceiveReturnFocus(element: HTMLElement | null | undefined): element is HTMLElement {
  return Boolean(element && element !== document.body && element.isConnected && !element.matches(":disabled, [aria-disabled='true']"));
}

export function ConfirmDialog({ open, title, description, confirmLabel, destructive = false, pending = false, error, returnFocusFallback, onConfirm, onCancel }: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);
  const returnFocusRef = useRef<HTMLElement | null>(null);
  const confirmedRef = useRef(false);
  const onCancelRef = useRef(onCancel);
  const pendingRef = useRef(pending);
  const id = useId();
  onCancelRef.current = onCancel;
  pendingRef.current = pending;

  useEffect(() => {
    if (!pending) confirmedRef.current = false;
  }, [pending]);

  useEffect(() => {
    if (!open) return;
    returnFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    confirmedRef.current = false;
    cancelRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        if (pendingRef.current) return;
        onCancelRef.current();
        return;
      }
      if (event.key !== "Tab" || !dialogRef.current) return;
      const focusable = Array.from(dialogRef.current.querySelectorAll<HTMLElement>(focusableSelector));
      const first = focusable[0];
      const last = focusable.at(-1);
      if (!dialogRef.current.contains(document.activeElement)) {
        event.preventDefault();
        (event.shiftKey ? last : first)?.focus();
      } else if (!first) {
        event.preventDefault();
        dialogRef.current.focus();
      } else if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last?.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first?.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      const returnTarget = returnFocusRef.current;
      const target = canReceiveReturnFocus(returnTarget)
        ? returnTarget
        : returnFocusFallback?.current;
      if (canReceiveReturnFocus(target)) target.focus();
    };
  }, [open, returnFocusFallback]);

  if (!open) return null;
  const titleId = `${id}-title`;
  const descriptionId = `${id}-description`;
  const errorId = `${id}-error`;
  return <div className="dialog-backdrop">
    <div ref={dialogRef} className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby={titleId} aria-describedby={`${descriptionId}${error ? ` ${errorId}` : ""}`} tabIndex={-1}>
      <h2 id={titleId}>{title}</h2>
      <p id={descriptionId}>{description}</p>
      {error ? <p id={errorId} className="form-error" role="alert">{error}</p> : null}
      <div className="confirm-dialog__actions">
        <button ref={cancelRef} type="button" disabled={pending} onClick={onCancel}>취소</button>
        <button type="button" className={destructive ? "button-danger" : undefined} aria-disabled={pending} onClick={() => {
          if (pending || confirmedRef.current) return;
          confirmedRef.current = true;
          onConfirm();
        }}>{pending ? "처리 중…" : confirmLabel}</button>
      </div>
    </div>
  </div>;
}
