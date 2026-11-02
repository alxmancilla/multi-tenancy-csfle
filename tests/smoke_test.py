"""Read-only API smoke tests for a running, seeded demo (Python standard library)."""

import argparse
import itertools
import json
import unittest
import urllib.parse
import urllib.request


BASE_URL = "http://localhost:8080"
TENANTS = {"tenant_alpha", "tenant_beta", "tenant_gamma"}


def api(path, payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    request = urllib.request.Request(
        BASE_URL + "/api/v1" + path,
        data=data,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


class DemoSmokeTest(unittest.TestCase):
    def test_tenant_reads(self):
        self.assertEqual({tenant["id"] for tenant in api("/tenants")}, TENANTS)
        for tenant in sorted(TENANTS):
            with self.subTest(tenant=tenant):
                customers = api(f"/tenants/{tenant}/customers")
                orders = api(f"/tenants/{tenant}/orders")
                self.assertGreaterEqual(len(customers), 2)
                self.assertGreaterEqual(len(orders), 2)
                for customer in customers:
                    self.assertTrue(customer["tenantId"] == tenant)
                    for field in ("name", "email", "phone", "address"):
                        self.assertIsInstance(customer[field], str)
                for order in orders:
                    self.assertTrue(order["tenantId"] == tenant)
                    self.assertIsInstance(order["product"], str)
                    self.assertIsInstance(order["status"], str)
                    self.assertIsInstance(order["amount"], (int, float))

    def test_email_equality_search(self):
        for tenant in sorted(TENANTS):
            with self.subTest(tenant=tenant):
                customer = api(f"/tenants/{tenant}/customers")[0]
                query = urllib.parse.urlencode({"email": customer["email"]})
                found = api(f"/tenants/{tenant}/customers/search?{query}")
                # Avoid including plaintext customer data in assertion output.
                self.assertTrue(found == customer, "Email lookup did not match")

    def test_customer_order_lookup(self):
        for tenant in sorted(TENANTS):
            customers = api(f"/tenants/{tenant}/customers")
            orders = api(f"/tenants/{tenant}/orders")
            for customer in customers:
                with self.subTest(tenant=tenant, customer=customer["customerId"]):
                    customer_id = urllib.parse.quote(customer["customerId"], safe="")
                    found = api(f"/tenants/{tenant}/orders/customer/{customer_id}")
                    expected = [o for o in orders if o["customerId"] == customer["customerId"]]
                    self.assertTrue(
                        sorted(found, key=lambda o: o["orderId"])
                        == sorted(expected, key=lambda o: o["orderId"]),
                        "Customer-order lookup did not match tenant orders",
                    )

    def test_cross_tenant_decryption_is_rejected(self):
        for attacker, victim in itertools.permutations(sorted(TENANTS), 2):
            with self.subTest(attacker=attacker, victim=victim):
                result = api("/demo/cross-tenant-attempt", {
                    "attackerTenant": attacker,
                    "victimTenant": victim,
                })
                self.assertIs(result.get("success"), False)
                self.assertIs(result.get("isolationEnforced"), True)
                self.assertTrue("HMAC validation failure" in result.get("error", ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=BASE_URL, help="Running demo API origin")
    args = parser.parse_args()
    BASE_URL = args.base_url.rstrip("/")
    unittest.main(argv=["smoke_test"], verbosity=2)