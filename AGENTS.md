<!-- BEGIN CURRENT MODEL COMMIT ATTRIBUTION -->
## Commit attribution

- For every AI-assisted commit, resolve the model actually used in the current session at commit time from trusted runtime/session metadata.
- Add `Co-Authored-By: Codex (<active-model-id>) <noreply@openai.com>`, replacing `<active-model-id>` with that verified model identifier.
- Recheck after a model change. Never hardcode a model version, copy an earlier commit's model, or assume the configured default is the active model.
- If the exact active model cannot be verified, use `Co-Authored-By: Codex <noreply@openai.com>` and disclose that limitation instead of guessing a version.
<!-- END CURRENT MODEL COMMIT ATTRIBUTION -->

<!-- BEGIN GLOBAL FINANCIAL SAFETY POLICY -->
## Mandatory financial policy: NEVER CANCEL OR REFUND

- NEVER cancel subscriptions, recurring charges, billing contracts, paid accounts or services, customer orders, or bookings. Never schedule their cancellation, disable renewal, or replace a subscription in a way that cancels its existing billing contract.
- NEVER issue full or partial refunds, prorated refunds or credits, reimbursements, or payment reversals. Do not use APIs, dashboards, scripts, bulk operations, or automated workflows to perform these actions.
- Billing exemptions, free access, provider assignments, plan changes, plan removal, migrations, tests, and cleanup are NEVER permission to cancel existing billing or refund money. App access and billing contracts are separate concerns.
- If requested work appears to require cancellation or a refund, stop that action, explain this prohibition and its financial impact, and leave the action to Tomas to perform personally. Do not infer an exception or execute it as a side effect.
- Creating charges and running automatic customer billing are permitted when otherwise authorized by the task and applicable payment rules. This does not authorize arbitrary charges, cancellation of an existing subscription, or refunds to correct a charge; Tomas handles any reversal personally.
- Cancellations and refunds can cause unrecoverable revenue loss. Preserve existing paid contracts and payment history. This is a standing instruction across all projects and tools, recorded October 18, 2026.
<!-- END GLOBAL FINANCIAL SAFETY POLICY -->

