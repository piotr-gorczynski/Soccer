# Support email alerts — initial DEV rollout

Cloud Build config: `gcp/cloud-build/deploy_support_alerts.yaml`.
Configure a manual trigger in `org-service-account-001` against branch `dev`,
using the existing deployment service account and `_ENVIRONMENT=dev`.
The YAML runs the checked-out source (no clone of a moving branch).
It rejects other branches/environments and targets only `soccer-dev-1744877837`.
The trigger itself has not been created by this change.

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

## Required backend follow-up (not implemented by this configuration)

Current support functions do not emit the matching event yet. Deploying this YAML
alone will NOT enable end-to-end notifications. Add structured server logs after a
successful ticket/report creation or user reply, then deploy with trigger 580:

```json
{"event":"support_attention_required","reason":"ticket_created"}
```

Use `reason=user_reply` for a new user reply. Do not emit for admin replies, block
actions, failed submissions, or automatic status/retention updates. Cover payout
tickets AND moderation reports. Emit after transaction commit, not inside a retried
transaction. Do not log message bodies, names, wallet numbers or profile data.
The filter targets the current first-generation `cloud_function` resource; review
it if support functions migrate to another generation.

## Validation before DEV rollout

Offline preview (no authentication/network/mutations):
`python gcp/monitoring/configure_support_alerts.py --environment dev --dry-run`

After backend logging is implemented and deployed, test a synthetic new ticket and
user reply in DEV, verify mail delivery, and confirm admin replies do not alert.
No end-to-end delivery test or deployment has been performed for this change.
TEST/PROD support must be added explicitly after the DEV test succeeds.

API reference: https://docs.cloud.google.com/logging/docs/alerting/log-based-alerts
