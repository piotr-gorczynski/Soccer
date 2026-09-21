# Support ticket administration

Install dependencies once:

```powershell
cd tools/support-tickets
npm install
```

List open tickets, reply, or resolve a ticket:

```powershell
node support-tickets.js dev list --status open
node support-tickets.js dev reply TICKET_ID "Please try again with an 11-digit number."
node support-tickets.js dev resolve TICKET_ID "The issue has been resolved."
```

Replies are stored under `supportTickets/{ticketId}/messages` and mirrored in
`latestSupportReply` for the current mobile UI. Updating a ticket triggers an
FCM notification when the support function is deployed.
