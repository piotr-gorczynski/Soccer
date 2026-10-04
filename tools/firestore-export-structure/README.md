# Firestore structure sample

Read-only CLI. Prints document paths and field types, including nested maps and distinct array element shapes. Field values are not printed; document IDs remain visible.

From the repository root (PowerShell), export one document reference per collection to a file:

```powershell
node tools/firestore-export-structure/index.js dev 1 | Out-File -Encoding utf8 firestore-structure-dev.txt
```

Replace `dev` with `test` or `prod` as needed. Credentials are loaded from `secrets/serviceAccountKey.<env>.json`.

The limit applies independently to each visited collection, including subcollections. References are sorted by path. Missing parent documents with surviving subcollections are included and count towards the limit. The tool lists all references in each visited collection, then reads only the selected sample.

This is a sample, not a complete schema: subcollections beneath unselected documents and fields found only in unselected documents are omitted. Increase the limit to inspect more documents. Empty collections, maps and arrays cannot reveal absent fields or element types. Listing references and reading documents can incur Firestore charges.

No data is modified. Errors produce a nonzero exit code. Run local tests with `npm test --prefix tools/firestore-export-structure`.
