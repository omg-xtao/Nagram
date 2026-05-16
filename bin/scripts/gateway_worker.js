// SPDX-License-Identifier: GPL-2.0-or-later
//
// WebPush/UnifiedPush gateway — Cloudflare Snippets edition.
//
// Routes (mirror of main.py, minus the 200 ms correlation suppression which
// requires setTimeout + module-level state — both unavailable in Snippets):
//
//   POST /aesgcm?e=<url>  WebPush (token_type=10): embeds Encryption /
//                         Crypto-Key headers into the body, forwards to the
//                         UnifiedPush endpoint.
//   PUT  /<url>           Simple Push (token_type=4): forwards the body
//                         directly. The client may briefly see both a POST
//                         and a PUT wake-up; the extra PUT only triggers a
//                         no-op aesgcm decryption failure on the device.
//
// Snippets budget: ≤ 1 subrequest, ≤ 5 ms CPU, ≤ 32 KiB.

const UPSTREAM_HEADERS = {
  "TTL": "2592000",
  "Urgency": "high",
  "Content-Encoding": "aes128gcm",
};

// Minimal IPv4 SSRF check — covers the common SSRF entry points without the
// CPU cost of full IPv6 parsing. Hostname → IP safety is enforced by the
// Cloudflare edge runtime (Snippets cannot reach RFC1918 from outbound fetch).
function ipv4Unsafe(host) {
  const m = host.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
  if (!m) return false;
  const a = +m[1], b = +m[2];
  if (a > 255 || b > 255 || +m[3] > 255 || +m[4] > 255) return true;
  return (
    a === 0 || a === 127 || a === 10 ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 168) ||
    (a === 100 && b >= 64 && b <= 127) ||
    (a === 169 && b === 254) ||
    a >= 224
  );
}

// Returns parsed URL on success, null on rejection.
function parseSafeEndpoint(raw) {
  let u;
  try { u = new URL(raw); } catch { return null; }
  if (u.protocol !== "http:" && u.protocol !== "https:") return null;
  if (u.username || u.password) return null;
  const h = u.hostname;
  if (!h) return null;
  if (ipv4Unsafe(h)) return null;
  // Bracketed IPv6 literals: cheap reject of obvious unsafe forms.
  if (h.startsWith("[")) {
    const inner = h.slice(1, -1).toLowerCase();
    if (
      inner === "::1" || inner === "::" ||
      inner.startsWith("fe8") || inner.startsWith("fe9") ||
      inner.startsWith("fea") || inner.startsWith("feb") ||
      inner.startsWith("fc") || inner.startsWith("fd") ||
      inner.startsWith("ff")
    ) return null;
  }
  return u;
}

// Zero-copy body builder: prepends the aesgcm header block to the request
// stream without buffering the ciphertext in memory.
function prefixedStream(prefix, source) {
  return new ReadableStream({
    start(controller) { controller.enqueue(prefix); },
    pull: source
      ? async (controller) => {
          const reader = source.getReader();
          for (;;) {
            const { value, done } = await reader.read();
            if (done) { controller.close(); return; }
            controller.enqueue(value);
          }
        }
      : (controller) => controller.close(),
  });
}

async function handleAesgcm(request, url) {
  const endpoint = url.searchParams.get("e");
  if (!endpoint) return new Response(null, { status: 400 });
  const target = parseSafeEndpoint(endpoint);
  if (!target) return new Response(null, { status: 403 });

  const encryption = request.headers.get("encryption") || "";
  const cryptoKey = request.headers.get("crypto-key") || "";

  const prefix = new TextEncoder().encode(
    "aesgcm\nEncryption: " + encryption + "\nCrypto-Key: " + cryptoKey + "\n"
  );

  const resp = await fetch(target, {
    method: "POST",
    headers: UPSTREAM_HEADERS,
    body: prefixedStream(prefix, request.body),
    redirect: "manual",
  });

  if (resp.status >= 200 && resp.status < 300) {
    // Normalize 2xx → 201 Created per WebPush spec to avoid Telegram backoff.
    return new Response(null, {
      status: 201,
      headers: { "location": resp.headers.get("Location") || endpoint },
    });
  }
  return new Response(resp.body, { status: resp.status });
}

async function handleSimplePush(request, url) {
  const target = parseSafeEndpoint(url.pathname.slice(1) + url.search);
  if (!target) return new Response(null, { status: 403 });

  const resp = await fetch(target, {
    method: "POST",
    headers: UPSTREAM_HEADERS,
    body: request.body,
    redirect: "manual",
  });
  return new Response(resp.body, { status: resp.status });
}

export default {
  fetch(request) {
    const url = new URL(request.url);
    const m = request.method;
    if (m === "POST" && url.pathname === "/aesgcm") return handleAesgcm(request, url);
    if (m === "PUT" && url.pathname.length > 1) return handleSimplePush(request, url);
    return new Response(null, { status: 404 });
  },
};
