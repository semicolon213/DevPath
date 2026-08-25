import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getPreferences, getProfile, setCareer, setCompany, updateProfile } from "../api/profileApi";
import { sessionQueryKey } from "../../session/model/useSession";

export const profileKey = ["identity", "profile"] as const;
export const preferencesKey = ["identity", "preferences"] as const;

export function useProfile() { return useQuery({ queryKey: profileKey, queryFn: getProfile }); }
export function usePreferences() { return useQuery({ queryKey: preferencesKey, queryFn: getPreferences }); }
export function useUpdateProfile() {
  const client = useQueryClient();
  return useMutation({ mutationFn: updateProfile, onSuccess: data => {
    client.setQueryData(profileKey, data);
    void client.invalidateQueries({ queryKey: sessionQueryKey });
  } });
}
export function useSetCareer() {
  const client = useQueryClient();
  return useMutation({ mutationFn: setCareer, onSuccess: data => client.setQueryData(preferencesKey, data) });
}
export function useSetCompany() {
  const client = useQueryClient();
  return useMutation({ mutationFn: setCompany, onSuccess: data => client.setQueryData(preferencesKey, data) });
}
