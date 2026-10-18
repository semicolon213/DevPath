import { fireEvent, render, screen } from "@testing-library/react";
import { useRef, useState } from "react";
import { expect, it, vi } from "vitest";
import { ConfirmDialog } from "./ConfirmDialog";

function Harness({ onConfirm = () => undefined }: { onConfirm?: () => void }) {
  const [open, setOpen] = useState(false);
  return <><button type="button" onClick={() => setOpen(true)}>보관 시작</button><ConfirmDialog open={open} title="보관할까요?" description="기존 이력은 유지됩니다." confirmLabel="로드맵 보관" destructive onCancel={() => setOpen(false)} onConfirm={onConfirm} /></>;
}

function PendingHarness() {
  const fallback = useRef<HTMLParagraphElement>(null);
  return <><button type="button" disabled>보관 시작</button><p ref={fallback} tabIndex={-1}>완료 상태</p><ConfirmDialog open title="보관할까요?" description="기존 이력은 유지됩니다." confirmLabel="로드맵 보관" pending returnFocusFallback={fallback} onCancel={() => undefined} onConfirm={() => undefined} /></>;
}

function FallbackHarness() {
  const [open, setOpen] = useState(false);
  const [disabled, setDisabled] = useState(false);
  const fallback = useRef<HTMLParagraphElement>(null);
  return <><button type="button" disabled={disabled} onClick={() => setOpen(true)}>보관 시작</button><p ref={fallback} tabIndex={-1}>완료 상태</p><ConfirmDialog open={open} title="보관할까요?" description="기존 이력은 유지됩니다." confirmLabel="로드맵 보관" returnFocusFallback={fallback} onCancel={() => { setDisabled(true); setOpen(false); }} onConfirm={() => undefined} /></>;
}

function BodyFallbackHarness() {
  const [open, setOpen] = useState(false);
  const fallback = useRef<HTMLParagraphElement>(null);
  return <><button type="button" onClick={() => setOpen(true)}>보관 시작</button><p ref={fallback} tabIndex={-1}>완료 상태</p><ConfirmDialog open={open} title="보관할까요?" description="기존 이력은 유지됩니다." confirmLabel="로드맵 보관" returnFocusFallback={fallback} onCancel={() => setOpen(false)} onConfirm={() => undefined} /></>;
}

it("moves focus into the dialog and traps Tab navigation", () => {
  render(<Harness />);
  fireEvent.click(screen.getByRole("button", { name: "보관 시작" }));
  const cancel = screen.getByRole("button", { name: "취소" });
  const confirm = screen.getByRole("button", { name: "로드맵 보관" });
  expect(cancel).toHaveFocus();
  fireEvent.keyDown(document, { key: "Tab", shiftKey: true });
  expect(confirm).toHaveFocus();
  fireEvent.keyDown(document, { key: "Tab" });
  expect(cancel).toHaveFocus();
});

it("cancels on Escape and returns focus to the trigger", () => {
  render(<Harness />);
  const trigger = screen.getByRole("button", { name: "보관 시작" });
  trigger.focus();
  fireEvent.click(trigger);
  fireEvent.keyDown(document, { key: "Escape" });
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(trigger).toHaveFocus();
});

it("cancels from the safe default button", () => {
  render(<Harness />);
  fireEvent.click(screen.getByRole("button", { name: "보관 시작" }));
  fireEvent.click(screen.getByRole("button", { name: "취소" }));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});

it("keeps focus trapped and ignores Escape while pending", () => {
  render(<PendingHarness />);
  const confirm = screen.getByRole("button", { name: "처리 중…" });
  document.body.focus();
  fireEvent.keyDown(document, { key: "Tab" });
  expect(confirm).toHaveFocus();
  fireEvent.keyDown(document, { key: "Escape" });
  expect(screen.getByRole("dialog")).toBeInTheDocument();
});

it("uses the fallback when the return target is disabled", () => {
  render(<FallbackHarness />);
  const trigger = screen.getByRole("button", { name: "보관 시작" });
  trigger.focus();
  fireEvent.click(trigger);
  fireEvent.click(screen.getByRole("button", { name: "취소" }));
  expect(screen.getByText("완료 상태")).toHaveFocus();
});

it("uses the fallback when the trigger did not receive focus", () => {
  render(<BodyFallbackHarness />);
  expect(document.body).toHaveFocus();
  fireEvent.click(screen.getByRole("button", { name: "보관 시작" }));
  fireEvent.click(screen.getByRole("button", { name: "취소" }));
  expect(screen.getByText("완료 상태")).toHaveFocus();
});

it("fires confirmation only once", () => {
  const onConfirm = vi.fn();
  render(<Harness onConfirm={onConfirm} />);
  fireEvent.click(screen.getByRole("button", { name: "보관 시작" }));
  const confirm = screen.getByRole("button", { name: "로드맵 보관" });
  fireEvent.click(confirm);
  fireEvent.click(confirm);
  expect(onConfirm).toHaveBeenCalledTimes(1);
});

it("allows a retry after a pending confirmation fails", () => {
  const onConfirm = vi.fn();
  const { rerender } = render(<ConfirmDialog open title="보관할까요?" description="기존 이력은 유지됩니다." confirmLabel="로드맵 보관" pending={false} onCancel={() => undefined} onConfirm={onConfirm} />);
  fireEvent.click(screen.getByRole("button", { name: "로드맵 보관" }));
  rerender(<ConfirmDialog open title="보관할까요?" description="기존 이력은 유지됩니다." confirmLabel="로드맵 보관" pending onCancel={() => undefined} onConfirm={onConfirm} />);
  rerender(<ConfirmDialog open title="보관할까요?" description="기존 이력은 유지됩니다." confirmLabel="로드맵 보관" pending={false} onCancel={() => undefined} onConfirm={onConfirm} />);
  fireEvent.click(screen.getByRole("button", { name: "로드맵 보관" }));
  expect(onConfirm).toHaveBeenCalledTimes(2);
});

it("has no native confirmation calls in product components", () => {
  const sources = import.meta.glob("../../**/*.tsx", { eager: true, query: "?raw", import: "default" });
  const productSources = Object.entries(sources).filter(([path]) => !path.includes(".test.")).map(([, source]) => source);
  expect(productSources.join("\n")).not.toContain("window.confirm");
});
