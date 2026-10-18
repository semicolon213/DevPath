import { createContext, useContext } from "react";

type UnsavedChangesState = {
  hasUnsavedChanges: boolean;
  ignoreUnsavedChanges: boolean;
  setHasUnsavedChanges: (value: boolean) => void;
};

export const UnsavedChangesContext = createContext<UnsavedChangesState>({
  hasUnsavedChanges: false,
  ignoreUnsavedChanges: false,
  setHasUnsavedChanges: () => undefined
});

export function useUnsavedChanges() {
  return useContext(UnsavedChangesContext);
}
