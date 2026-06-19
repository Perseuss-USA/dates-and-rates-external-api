# Dates & Rates External API

External carrier-service API for non-Shopify brands that use the Dates & Rates platform through a 3PL.

The API accepts an already-packed parcel, origin provider, and destination address. Dates & Rates returns carrier rates from the configured 3PL carrier integrations, then applies the configured 3PL and brand rate rules.

## Base URL

```text
https://app.datesnrates.com/api
```

## Endpoint

```http
POST /external/v1/rates
```

Public URL:

```text
https://app.datesnrates.com/api/external/v1/rates
```

## Authentication

Each request is authenticated with a 3PL API key and HMAC-SHA256 signature.

Required headers:

| Header | Description |
| --- | --- |
| `X-DNR-3PL-ID` | 3PL provider identifier. For ShipBots, use `6` unless Dates & Rates gives you a different value. |
| `X-DNR-API-KEY` | API key issued by Dates & Rates. |
| `X-DNR-TIMESTAMP` | Unix timestamp in seconds. Must be within 5 minutes of server time. |
| `X-DNR-NONCE` | Unique request nonce. Reusing a nonce within 5 minutes is rejected. |
| `X-DNR-SIGNATURE` | `v1=` followed by the HMAC-SHA256 hex digest. |

### Signature String

Build the string to sign exactly:

```text
v1
METHOD
PATH_WITH_QUERY
TIMESTAMP
NONCE
SHA256_HEX_RAW_BODY
```

For the production public URL, sign this path:

```text
/external/v1/rates
```

The public request URL includes `/api`, but the Dates & Rates gateway strips `/api` before signature verification.

Example:

```text
v1
POST
/external/v1/rates
1781899589
09a34c36-d2c7-4588-b3e6-a1709783d524
8d7e0f0c...
```

Then compute:

```text
hex(hmac_sha256(DNR_API_SECRET, string_to_sign))
```

Send it as:

```text
X-DNR-SIGNATURE: v1=<hex_digest>
```

## Request Body

```json
{
  "brand": {
    "domain": "https://nologo.com"
  },
  "origin": {
    "provider_id": 6
  },
  "destination": {
    "address1": "548 Market St",
    "city": "San Francisco",
    "province": "CA",
    "postal_code": "94104",
    "country": "US",
    "name": "Customer Name",
    "phone": "5555555555",
    "email": "customer@example.com"
  },
  "parcel": {
    "length": 10,
    "width": 8,
    "height": 4,
    "dimension_unit": "in",
    "weight": 2,
    "weight_unit": "lb"
  },
  "currency": "USD",
  "subtotal_price_cents": 5000,
  "items": [
    {
      "sku": "external-parcel",
      "quantity": 1
    }
  ]
}
```

### Request Fields

| Field | Required | Notes |
| --- | --- | --- |
| `brand.domain` | Yes | Brand domain configured in Dates & Rates, for example `https://nologo.com`. |
| `origin.provider_id` | Optional | Provider id. If present, it must match the authenticated 3PL. |
| `destination.address1` | Recommended | Street address. |
| `destination.city` | Recommended | Destination city. |
| `destination.province` | Recommended | State/province code, for example `CA`. |
| `destination.postal_code` | Required for countries that use postal codes | Required for US destinations. |
| `destination.country` | Yes | Two-letter country code, for example `US`. |
| `parcel.length` | Yes | Positive number. |
| `parcel.width` | Yes | Positive number. |
| `parcel.height` | Yes | Positive number. |
| `parcel.dimension_unit` | Yes | Must be `in`. |
| `parcel.weight` | Yes | Positive number. |
| `parcel.weight_unit` | Yes | Must be `lb`. |
| `currency` | Yes | Must be `USD`. |
| `subtotal_price_cents` | Optional | Used for free-shipping thresholds and markup rules. Defaults to `0`. |
| `items` | Optional | SKU/quantity metadata. The API does not cartonize. |

## Response Body

```json
{
  "rates": [
    {
      "service_name": "Express Shipping with Expedited Handling",
      "service_code": "express_shipping_with_expedited_handling",
      "total_price": "3724",
      "currency": "USD",
      "min_delivery_date": "2026-06-22",
      "max_delivery_date": "2026-06-22",
      "external_code": "fedex-2day"
    }
  ],
  "request_id": "6e968ed0-4b11-4fb3-b50d-343a6d07ab6b"
}
```

Notes:

- `total_price` is in cents.
- `external_code` is the underlying carrier/method code before display-name mapping.
- Returned rates already include configured 3PL markups, brand markups, delivery-date padding, and brand padding.
- The API does not perform cartonization. Send the final parcel dimensions and weight.

## Errors

| Status | Meaning |
| --- | --- |
| `400` | Invalid request, unsupported currency, missing destination postal code, or provider origin is not configured. |
| `401` | Missing/invalid API key, signature, timestamp, or reused nonce. |
| `403` | Brand is not available for the authenticated provider. |
| `422` | Request body failed schema validation. |
| `503` | External API credentials or provider configuration are missing. |

## Examples

See:

- [Python](examples/python/rate_request.py)
- [TypeScript](examples/typescript/rate_request.ts)
- [Java](examples/java/RateRequest.java)
- [Example env](.env.example)

## Minimal cURL Shape

Do not hard-code this signature. Generate it for every request.

```bash
curl -X POST "https://app.datesnrates.com/api/external/v1/rates" \
  -H "Content-Type: application/json" \
  -H "X-DNR-3PL-ID: $DNR_3PL_ID" \
  -H "X-DNR-API-KEY: $DNR_API_KEY" \
  -H "X-DNR-TIMESTAMP: $DNR_TIMESTAMP" \
  -H "X-DNR-NONCE: $DNR_NONCE" \
  -H "X-DNR-SIGNATURE: v1=$DNR_SIGNATURE" \
  --data-binary @payload.json
```

## Security

- Keep `DNR_API_SECRET` server-side only.
- Use a fresh nonce for every request.
- Do not retry a signed request with the same nonce. If retrying, regenerate timestamp, nonce, and signature.
- Do not log API secrets or raw signatures in application logs.
