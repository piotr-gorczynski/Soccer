# Create Regulation Tool

This tool validates a regulation JSON file and imports it into the
`regulations` collection in Firestore. Firestore generates the document ID;
the tool does not implement custom IDs or regulation versioning.

The root regulation document contains machine-readable eligibility and prize
rules. Localized rule text is stored using the existing schema:

```text
regulations/{nativeId}/{language}/rules
```

## Install

```bash
cd tools/create-regulation
npm install
```

The target environment requires its existing service account file:

```text
secrets/serviceAccountKey.dev.json
secrets/serviceAccountKey.test.json
secrets/serviceAccountKey.prod.json
```

## Validate without writing

```bash
node create-regulation.js dev regulation-example.json --dry-run
```

## Create a regulation

```bash
node create-regulation.js dev regulation-example.json
```

Replace `dev` with `test` or `prod` to select another environment. After a
successful import, the command prints the native Firestore document ID. Pass
that ID as the `regulation` value when using `tools/create-tournament`.

## JSON schema

See [`regulation-example.json`](regulation-example.json) for a complete
Bangladesh example.

Required fields:

- `name`: regulation display name;
- `translations`: object keyed by language code; every translation must have a
  non-empty `rules` array.

Optional fields:

- `status`: `draft`, `active`, `inactive`, or `archived`; defaults to `draft`;
- `body`: root document text; when omitted it is generated from English rules,
  or from the first available translation;
- `market`: uppercase ISO 3166-1 alpha-2 country code;
- `minimumAge`: integer from 0 to 120;
- `prizeRules`: structured cash-prize configuration.

When `prizeRules.cashPrizesEnabled` is `true`, `market`, `minimumAge`, a
three-letter uppercase `currency`, and at least one `payoutMethods` entry are
required.

Payout methods describe destinations offered to the winner, such as `bkash`,
`nagad`, or `bank_account`. Transfer operators used administratively, such as
Remitly or Wise, do not belong in this list.

The root document and all translation documents are written in one Firestore
batch. A validation or write failure therefore does not leave a partially
created regulation.

## Backward compatibility

Existing regulations without `market`, `minimumAge`, or `prizeRules` remain
valid. This tool does not modify or migrate existing documents, and the current
`420-deploy-seed-regulations` trigger is unchanged.
