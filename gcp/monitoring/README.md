# Support email alerts — initial DEV rollout

Cloud Build config: `gcp/cloud-build/deploy_support_alerts.yaml`.
Manual trigger `600-deploy-support-alerts` in `org-service-account-001` uses branch
`dev`, the existing deployment service account and `_ENVIRONMENT=dev`.
The YAML runs the checked-out source (no clone of a moving branch).
It rejects other branches/environments and targets only `soccer-dev-1744877837`.
DEV configuration deployed successfully on 18 October 2026 (build
`b048fe9e-a1e6-4e04-9357-bafb3f766da9`). Email delivery has not yet been tested.

The script creates/updates one labelled email channel and one log-based alert policy.
Run deployments serially: concurrent first runs could create duplicate resources.
Duplicates cause subsequent runs to fail rather than pick or delete resources arbitrarily.
No APIs are enabled and no IAM, DNS, functions, hosting or Firestore data are changed.
Monitoring and Logging APIs must already be enabled. The build service account needs
notification-channel and alert-policy list/create/update permissions in DEV, plus
`logging.notificationRules.create` for log-based alert creation. Missing access must
be reviewed separately; this build never grants itself permissions.

Recipient: `support@piotr-gorczynski.com`. The Google group must allow messages from
`alerting-noreply@google.com`; verify delivery and channel verification state after deployment.
Notifications use Google's sender, so no domain SPF/DKIM changes are needed.
Notifications are rate-limited to one per 5 minutes, with incident auto-close after
30 minutes. This is an attention alert, not a durable one-email-per-ticket queue.
Always inspect the support list for all outstanding work.

## Backend events (deploy through 580)

The support codebase now includes two first-generation Firestore creation handlers:
`onSupportTicketCreated` for `supportTickets/{ticketId}` and
`onSupportMessageCreated` for `supportTickets/{ticketId}/messages/{messageId}`.
Deploy them with trigger 580. They emit structured server logs after commit:

```json
{"event":"support_attention_required","reason":"ticket_created"}
```

New open payout tickets AND moderation reports emit `reason=ticket_created`.
Subsequent messages with `authorType=user` and the ticket owner's `authorId` emit
`reason=user_reply`. The current app has no subsequent-user-reply write path;
this handler supports future trusted message creation without changing app behavior.
Initial messages (`initial`), admin replies, closed/resolved tickets, block actions,
failed submissions and automatic status/retention updates do not emit alerts.
Only event/reason, ticket ID and Firestore event ID are logged; no message bodies,
names, wallet numbers or profile data. Firestore event delivery is at least once;
the event ID permits tracing duplicates, but no exactly-once email guarantee is made.
The filter targets the current first-generation `cloud_function` resource; review
it if support functions migrate to another generation.

## Validation before DEV rollout

Offline preview (no authentication/network/mutations):
`python gcp/monitoring/configure_support_alerts.py --environment dev --dry-run`

After backend logging is deployed, test a synthetic new ticket and
user reply in DEV, verify mail delivery, and confirm admin replies do not alert.
The new backend handlers have not yet been deployed or tested for email delivery.
TEST/PROD support must be added explicitly after the DEV test succeeds.

API reference: https://docs.cloud.google.com/logging/docs/alerting/log-based-alerts
