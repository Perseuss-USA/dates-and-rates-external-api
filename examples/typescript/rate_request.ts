import crypto from "node:crypto";

const baseUrl = process.env.DNR_BASE_URL ?? "https://app.datesnrates.com/api";
const signingPath = process.env.DNR_SIGNING_PATH ?? "/external/v1/rates";

function env(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`${name} is required`);
  }
  return value;
}

function stableStringify(value: unknown): string {
  if (value === null || typeof value !== "object") {
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(",")}]`;
  }
  const record = value as Record<string, unknown>;
  return `{${Object.keys(record)
    .sort()
    .map((key) => `${JSON.stringify(key)}:${stableStringify(record[key])}`)
    .join(",")}}`;
}

function signRequest(
  method: string,
  path: string,
  body: string,
  timestamp: string,
  nonce: string,
): string {
  const bodyHash = crypto.createHash("sha256").update(body).digest("hex");
  const message = ["v1", method.toUpperCase(), path, timestamp, nonce, bodyHash].join("\n");
  const digest = crypto
    .createHmac("sha256", env("DNR_API_SECRET"))
    .update(message)
    .digest("hex");
  return `v1=${digest}`;
}

async function main() {
  const payload = {
    brand: { domain: process.env.DNR_BRAND_DOMAIN ?? "https://nologo.com" },
    origin: { provider_id: Number(process.env.DNR_3PL_ID ?? "6") },
    destination: {
      address1: "548 Market St",
      city: "San Francisco",
      province: "CA",
      postal_code: "94104",
      country: "US",
      name: "Customer Name",
      phone: "5555555555",
      email: "customer@example.com",
    },
    parcel: {
      length: 10,
      width: 8,
      height: 4,
      dimension_unit: "in",
      weight: 2,
      weight_unit: "lb",
    },
    currency: "USD",
    subtotal_price_cents: 5000,
    items: [{ sku: "external-parcel", quantity: 1 }],
  };

  const body = stableStringify(payload);
  const timestamp = Math.floor(Date.now() / 1000).toString();
  const nonce = crypto.randomUUID();

  const response = await fetch(`${baseUrl}/external/v1/rates`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-DNR-3PL-ID": env("DNR_3PL_ID"),
      "X-DNR-API-KEY": env("DNR_API_KEY"),
      "X-DNR-TIMESTAMP": timestamp,
      "X-DNR-NONCE": nonce,
      "X-DNR-SIGNATURE": signRequest("POST", signingPath, body, timestamp, nonce),
    },
    body,
  });

  console.log(response.status);
  console.log(await response.text());
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
