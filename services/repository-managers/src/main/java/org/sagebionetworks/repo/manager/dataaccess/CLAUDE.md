# manager/dataaccess

Managers for the data-access request/approval flow — access requirements, research-project submissions, and their approval state.

## Bean-name collision — qualify the submission manager

`SubmissionManagerImpl` is registered as `@Service("dataAccessSubmissionManager")`. **Inject it with `@Qualifier("dataAccessSubmissionManager")`** — the unqualified bean name `submissionManager` collides with the evaluations `SubmissionManager`, so an unqualified inject is ambiguous / resolves to the wrong bean (evidence: `SubmissionManagerImpl.java:76`).

## Submission state machine

A submission transitions only `SUBMITTED → APPROVED` or `SUBMITTED → REJECTED`, enforced via `ValidateArgument.requirement(...)` in the manager (not a DB constraint). Validation also checks etag match and that no conflicting submission state already exists. When adding a transition, add the guard here — the state machine is code-enforced, so a missing check silently allows an illegal transition.
