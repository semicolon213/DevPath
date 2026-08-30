import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  archiveKnowledgeDocument, getKnowledgeChunks, getKnowledgeDocument, getKnowledgeDocuments,
  getKnowledgeIngestionJob, importNotionKnowledge, reindexKnowledgeDocument, searchKnowledge
} from "../api/knowledgeApi";

export const knowledgeDocumentsKey = ["knowledge", "documents"] as const;

export function useKnowledgeDocuments() {
  return useQuery({ queryKey: knowledgeDocumentsKey, queryFn: getKnowledgeDocuments });
}

export function useKnowledgeDocument(documentId: string) {
  return useQuery({ queryKey: ["knowledge", "documents", documentId], queryFn: () => getKnowledgeDocument(documentId) });
}

export function useKnowledgeChunks(documentId: string) {
  return useQuery({ queryKey: ["knowledge", "documents", documentId, "chunks"], queryFn: () => getKnowledgeChunks(documentId) });
}

export function useKnowledgeJob(jobId: string | null) {
  return useQuery({
    queryKey: ["knowledge", "jobs", jobId],
    queryFn: () => getKnowledgeIngestionJob(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: query => {
      const status = query.state.data?.status;
      return status === "queued" || status === "running" ? 1000 : false;
    }
  });
}

export function useImportNotionKnowledge() {
  return useMutation({ mutationFn: ({ connectionId, providerPageId }: { connectionId: string; providerPageId: string }) =>
    importNotionKnowledge(connectionId, providerPageId) });
}

export function useArchiveKnowledgeDocument() {
  const queryClient = useQueryClient();
  return useMutation({ mutationFn: archiveKnowledgeDocument, onSuccess: async document => {
    queryClient.setQueryData(["knowledge", "documents", document.documentId], document);
    await queryClient.invalidateQueries({ queryKey: knowledgeDocumentsKey });
  }});
}

export function useReindexKnowledgeDocument() {
  return useMutation({ mutationFn: reindexKnowledgeDocument });
}

export function useKnowledgeSearch() {
  return useMutation({ mutationFn: ({ query, documentIds }: { query: string; documentIds: string[] }) =>
    searchKnowledge(query, documentIds) });
}
