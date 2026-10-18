"""Configure DEV support alerts using the build service account, without changing IAM."""
import argparse
import json
import subprocess
import urllib.error
import urllib.parse
import urllib.request

PROJECT = "soccer-dev-1744877837"
EMAIL = "support@piotr-gorczynski.com"
OWNER = "soccer-support-alerts-v1"


def channel_spec():
    return {
        "displayName": "DEV - Soccer support email",
        "type": "email",
        "labels": {"email_address": EMAIL},
        "userLabels": {"managed_by": OWNER, "environment": "dev"},
        "enabled": True,
    }


def policy_spec(channel):
    return {
        "displayName": "DEV - Soccer support requires attention",
        "userLabels": {"managed_by": OWNER, "environment": "dev"},
        "enabled": True,
        "combiner": "OR",
        "conditions": [{
            "displayName": "New support request or user reply",
            "conditionMatchedLog": {
                "filter": (
                    f'resource.labels.project_id="{PROJECT}"\n'
                    'resource.type="cloud_function"\n'
                    'jsonPayload.event="support_attention_required"\n'
                    'jsonPayload.reason=("ticket_created" OR "user_reply")'
                ),
            },
        }],
        "notificationChannels": [channel],
        "alertStrategy": {
            "notificationRateLimit": {"period": "300s"},
            "autoClose": "1800s",
        },
        "documentation": {
            "mimeType": "text/markdown",
            "content": (
                "DEV support requires attention. Review open tickets; this alert may "
                "represent multiple requests and is not one email per ticket.\n\n"
                f"[Open DEV Firestore](https://console.firebase.google.com/project/{PROJECT}"
                "/firestore/databases/-default-/data/~2FsupportTickets)\n\n"
                "CLI: `node tools/support-tickets/support-tickets.js dev list`\n\n"
                "No message bodies or recipient details are included in this notification."
            ),
        },
    }


class Monitoring:
    def __init__(self):
        self.token = subprocess.check_output(
            ["gcloud", "auth", "print-access-token"], text=True).strip()

    def request(self, method, path, body=None):
        request = urllib.request.Request(
            "https://monitoring.googleapis.com/v3/" + path,
            data=None if body is None else json.dumps(body).encode(),
            headers={"Authorization": "Bearer " + self.token,
                     "Content-Type": "application/json"}, method=method)
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            raise RuntimeError(f"Monitoring API {error.code}: "
                               + error.read().decode()) from None

    def find(self, collection):
        items, token = [], ""
        while True:
            page = self.request("GET", f"projects/{PROJECT}/{collection}?"
                                + urllib.parse.urlencode({"pageToken": token}))
            items.extend(page.get(collection, []))
            token = page.get("nextPageToken")
            if not token:
                break
        matches = [item for item in items
                   if item.get("userLabels", {}).get("managed_by") == OWNER]
        if len(matches) > 1:
            raise RuntimeError(f"Multiple managed {collection}; inspect before retrying.")
        return matches[0] if matches else None

    def upsert(self, collection, spec, existing):
        if existing:
            # Keep the existing condition identity during policy updates.
            if "conditions" in spec and existing.get("conditions"):
                spec["conditions"][0]["name"] = existing["conditions"][0]["name"]
            mask = ",".join(spec)
            spec["name"] = existing["name"]
            result = self.request("PATCH", existing["name"] + "?updateMask=" + mask, spec)
        else:
            result = self.request("POST", f"projects/{PROJECT}/{collection}", spec)
        print(result["name"])
        return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--environment", required=True, choices=["dev"])
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if args.dry_run:
        print(json.dumps({"channel": channel_spec(), "policy": policy_spec(
            f"projects/{PROJECT}/notificationChannels/DRY_RUN")}, indent=2))
        return
    api = Monitoring()
    # Resolve both resources before any mutation; fail on duplicates or missing read access.
    channel = api.find("notificationChannels")
    policy = api.find("alertPolicies")
    channel = api.upsert("notificationChannels", channel_spec(), channel)
    api.upsert("alertPolicies", policy_spec(channel["name"]), policy)


if __name__ == "__main__":
    main()
