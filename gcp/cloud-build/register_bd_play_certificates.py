"""Register public Play signing certificates for the production Bangladesh app."""

import json
from pathlib import Path
import subprocess
import urllib.request


PROJECT = "soccer-prod-1754048346"
APP_ID = "1:249008569891:android:76386af0099ba9ba91bb96"
PACKAGE = "piotr_gorczynski.soccer2.bd"
# Read from this app's Play Console signing page on 2026-11-09.
CERTIFICATES = {
    ("SHA_1", "94a9a24a56ccc3e50b295fcff2d44093cddddfdcf"),
    ("SHA_256", "451ad58133f68d4c51e2c37e8ee2d3952ca08160c258023e8dd11c513eda5002"),
}


def reconcile(project, request):
    if project != PROJECT:
        print("Skipping production Bangladesh Play certificates for another project.")
        return
    path = f"projects/{PROJECT}/androidApps/{APP_ID}"
    app = request("GET", path)
    if app.get("packageName") != PACKAGE or app.get("appId") != APP_ID:
        raise ValueError("Unexpected Firebase app; no certificates added")

    def read_certificates():
        result = request("GET", f"{path}/sha")
        return {
            (cert["certType"], cert["shaHash"].replace(":", "").lower())
            for cert in result.get("certificates", [])
        }

    existing = read_certificates()
    for cert_type, fingerprint in sorted(CERTIFICATES - existing):
        request("POST", f"{path}/sha", {
            "certType": cert_type, "shaHash": fingerprint,
        })
        print(f"Registered Bangladesh Play {cert_type}: {fingerprint}")
    if not CERTIFICATES.issubset(read_certificates()):
        raise RuntimeError("Bangladesh Play certificate verification failed")
    print("Verified both production Bangladesh Play certificates.")


def main():
    project = Path("/workspace/SOCCER_PROJECT_ID.txt").read_text().strip()
    if project != PROJECT:
        reconcile(project, None)
        return
    token = subprocess.check_output(
        ["gcloud", "auth", "print-access-token"], text=True
    ).strip()

    def request(method, path, payload=None):
        data = None if payload is None else json.dumps(payload).encode()
        req = urllib.request.Request(
            f"https://firebase.googleapis.com/v1beta1/{path}",
            data=data, method=method,
            headers={"Authorization": f"Bearer {token}",
                     "Content-Type": "application/json",
                     "x-goog-user-project": PROJECT},
        )
        with urllib.request.urlopen(req, timeout=30) as response:
            return json.load(response)

    reconcile(project, request)


if __name__ == "__main__":
    main()
