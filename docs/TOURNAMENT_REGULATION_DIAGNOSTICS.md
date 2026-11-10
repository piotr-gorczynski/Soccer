# Tournament regulation diagnostics

Both app variants send `tournament_regulation_step` through the existing Analytics
manager, with existing consent settings and pseudonymous Analytics identity.
This adds no authentication, eligibility, joining or payout changes.

| stage | outcome | Meaning |
| --- | --- | --- |
| load | success / failure | Rule content loaded, fallback used, or controlled failure |
| retry | submit | Explicit Retry after a failed load; concurrent taps are ignored |
| confirmations_visible | shown | Each of the three checkboxes has intersected the visible screen while the window had focus |
| accept_ready | ready | Accept was enabled for the first time |
| exit | abandoned / declined | Screen finished without any submission; Decline is explicit |

Parameters: public `tournament_id` when valid; `stage`, `outcome`, `reason`;
numeric flags `content_loaded`, `configuration_valid`, `confirmations_required`,
`confirmations_seen`, `accept_ready`; `checked_count` (0–3); and `load_attempt` (starts
at 1 and increments only for explicit retries). No checkbox-specific
personal answers, nickname, email, raw exception message, stack trace or raw UID
is added. Failure classification uses SDK exception codes, not message parsing.

The load reasons are `none`, `fallback`, `not_found`, `missing_content`, `network`,
`permission`, `invalid_configuration`, `missing_user`, `missing_regulation`, `unknown`.
Success with `fallback` means public fallback body was displayed instead of localized rules.
The content flag describes actual content, independently of the existing UI-ready flag.

Milestones are deduplicated for the screen visit, with state preserved across
Activity recreation. Scrolling and checkbox toggles do not create per-interaction
events. Visibility accumulates across the three checkboxes so large fonts do not
require all of them to fit simultaneously. Partial visibility is not proof of reading.

Backgrounding alone does not mean abandonment. An explicit finish produces an exit;
after a submission the existing submission/success/error events describe the result.
Process kill, crash, or OS termination cannot reliably emit an exit. Absence of an
exit event is not evidence of a network error or deliberate refusal.

Explicit retries produce a new load result, allowing a network failure to be
followed by successful recovery. Identical results after Activity recreation are
deduplicated, but a changed load result is reported. The persistent error panel
keeps Accept disabled until actual non-empty content and a valid eligibility
configuration are available; all required confirmations still apply after retry.

Use BigQuery for detailed numeric parameters. Existing GA4 definitions for stage,
outcome and reason suffice; no new custom definitions or key events are required.
