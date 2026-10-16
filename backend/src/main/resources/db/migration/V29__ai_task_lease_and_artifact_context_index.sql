ALTER TABLE ai_tasks ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE;

UPDATE ai_tasks SET next_attempt_at = requested_at;

ALTER TABLE ai_tasks ALTER COLUMN next_attempt_at SET NOT NULL;

DROP INDEX ai_tasks_claim_idx;
CREATE INDEX ai_tasks_claim_idx ON ai_tasks (status, next_attempt_at);

CREATE INDEX generated_artifacts_context_owner_idx
    ON generated_artifacts (prompt_context_id, user_id);
