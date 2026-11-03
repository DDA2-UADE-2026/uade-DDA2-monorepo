import unittest
from server import response


class ContractTests(unittest.TestCase):
    def test_token_requires_matching_identity_and_secret(self):
        self.assertEqual(401, response("POST", "/api/v1/auth/module-token", {}, {"module": "other", "secret": "test"}, "test")[0])
        self.assertEqual(401, response("POST", "/api/v1/auth/module-token", {}, {"module": "desarrollo-social", "secret": "wrong"}, "test")[0])
        status, token = response("POST", "/api/v1/auth/module-token", {}, {"module": "desarrollo-social", "secret": "test"}, "test")
        self.assertEqual(200, status)
        self.assertEqual("module", token["kind"])
        headers = {"Authorization": "Bearer " + token["accessToken"]}
        self.assertEqual(401, response("GET", "/api/v1/dashboard", {}, None, "test")[0])
        status, dashboard = response("GET", "/api/v1/dashboard?windowHours=48", headers, None, "test")
        self.assertEqual(200, status)
        self.assertEqual(48, dashboard["windowHours"])
        self.assertEqual("desarrollo-social", dashboard["module"])
        self.assertEqual(400, response("GET", "/api/v1/dashboard?windowHours=0", headers, None, "test")[0])
        self.assertEqual((200, []), response("GET", "/api/v1/publications", headers, None, "test"))
        self.assertEqual((200, []), response("GET", "/api/v1/subscriptions", headers, None, "test"))
        self.assertEqual(400, response("GET", "/api/v1/events?page=-1", headers, None, "test")[0])
        self.assertEqual({"items": [], "total": 0, "page": 2, "size": 10, "pages": 0}, response("GET", "/api/v1/events?page=2&size=10", headers, None, "test")[1])
        self.assertEqual(404, response("GET", "/api/v1/events/00000000-0000-0000-0000-000000000000", headers, None, "test")[0])


if __name__ == "__main__":
    unittest.main()
