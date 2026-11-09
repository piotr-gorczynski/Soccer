import unittest

from register_bd_play_certificates import APP_ID, CERTIFICATES, PACKAGE, PROJECT, reconcile


class RegisterCertificatesTest(unittest.TestCase):
    def fake_api(self, existing=None, package=PACKAGE):
        certs = set(existing or [])
        calls = []

        def request(method, path, payload=None):
            calls.append((method, path, payload))
            if not path.endswith("/sha"):
                return {"appId": APP_ID, "packageName": package}
            if method == "POST":
                certs.add((payload["certType"], payload["shaHash"]))
                return payload
            return {"certificates": [
                {"certType": kind, "shaHash": value} for kind, value in certs
            ]}

        return request, calls, certs

    def test_adds_both_and_preserves_existing(self):
        existing = {("SHA_1", "a" * 40)}
        api, calls, certs = self.fake_api(existing)
        reconcile(PROJECT, api)
        self.assertEqual(certs, existing | CERTIFICATES)
        self.assertEqual(sum(call[0] == "POST" for call in calls), 2)

    def test_repeat_does_not_duplicate(self):
        api, calls, _ = self.fake_api(CERTIFICATES)
        reconcile(PROJECT, api)
        self.assertFalse(any(call[0] == "POST" for call in calls))

    def test_other_environment_does_not_call_api(self):
        reconcile("soccer-dev", lambda *args: self.fail("Unexpected API call"))

    def test_wrong_package_fails_before_write(self):
        api, calls, _ = self.fake_api(package="piotr_gorczynski.soccer2")
        with self.assertRaises(ValueError):
            reconcile(PROJECT, api)
        self.assertFalse(any(call[0] == "POST" for call in calls))


if __name__ == "__main__":
    unittest.main()
