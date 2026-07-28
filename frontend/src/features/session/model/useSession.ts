import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { getCurrentUser, logout } from "../api/sessionApi";

export const sessionQueryKey = ["session", "current-user"] as const;

export function useSession() {
  return useQuery({
    queryKey: sessionQueryKey,
    queryFn: getCurrentUser
  });
}

export function useLogout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.setQueryData(sessionQueryKey, null);
    }
  });
}
