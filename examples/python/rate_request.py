import hashlib
import hmac
import json
import os
import time
import urllib.request
from uuid import uuid4


BASE_URL = os.getenv("DNR_BASE_URL", "https://app.datesnrates.com/api")
SIGNING_PATH = os.getenv("DNR_SIGNING_PATH", "/external/v1/rates")


def env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"{name} is required")
    return value


def sign_request(method: str, path: str, body: bytes, timestamp: str, nonce: str) -> str:
    body_hash = hashlib.sha256(body).hexdigest()
    message = "\n".join([method.upper(), path, timestamp, nonce, body_hash])
    versioned_message = "v1\n" + message
    digest = hmac.new(
        env("DNR_API_SECRET").encode("utf-8"),
        versioned_message.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
    return f"v1={digest}"


payload = {
    "brand": {"domain": os.getenv("DNR_BRAND_DOMAIN", "https://nologo.com")},
    "origin": {"provider_id": int(os.getenv("DNR_3PL_ID", "6"))},
    "destination": {
        "address1": "548 Market St",
        "city": "San Francisco",
        "province": "CA",
        "postal_code": "94104",
        "country": "US",
        "name": "Customer Name",
        "phone": "5555555555",
        "email": "customer@example.com",
    },
    "parcel": {
        "length": 10,
        "width": 8,
        "height": 4,
        "dimension_unit": "in",
        "weight": 2,
        "weight_unit": "lb",
    },
    "currency": "USD",
    "subtotal_price_cents": 5000,
    "items": [{"sku": "external-parcel", "quantity": 1}],
}

body = json.dumps(payload, separators=(",", ":"), sort_keys=True).encode("utf-8")
timestamp = str(int(time.time()))
nonce = str(uuid4())
url = f"{BASE_URL}/external/v1/rates"

request = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={
        "Content-Type": "application/json",
        "X-DNR-3PL-ID": env("DNR_3PL_ID"),
        "X-DNR-API-KEY": env("DNR_API_KEY"),
        "X-DNR-TIMESTAMP": timestamp,
        "X-DNR-NONCE": nonce,
        "X-DNR-SIGNATURE": sign_request("POST", SIGNING_PATH, body, timestamp, nonce),
    },
)

with urllib.request.urlopen(request, timeout=30) as response:
    print(response.status)
    print(response.read().decode("utf-8"))
