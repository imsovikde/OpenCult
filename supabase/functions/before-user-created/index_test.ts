import { handleBeforeUserCreated } from "./index.ts";

function assert(cond: unknown, msg: string): asserts cond {
  if (!cond) throw new Error(msg);
}

Deno.test("blocks direct disposable email domain with HTTP 403", async () => {
  const req = new Request("http://localhost/before-user-created", {
    method: "POST",
    body: JSON.stringify({ user: { email: "bot@mailinator.com" } }),
  });
  const res = await handleBeforeUserCreated(req, { checkDns: async () => true });
  assert(res.status === 403, `Expected 403, got ${res.status}`);
});

Deno.test("blocks subdomain of disposable email domain", async () => {
  const req = new Request("http://localhost/before-user-created", {
    method: "POST",
    body: JSON.stringify({ user: { email: "test@sub.yopmail.com" } }),
  });
  const res = await handleBeforeUserCreated(req, { checkDns: async () => true });
  assert(res.status === 403, `Expected 403, got ${res.status}`);
});

Deno.test("blocks domain when MX check fails", async () => {
  const req = new Request("http://localhost/before-user-created", {
    method: "POST",
    body: JSON.stringify({ user: { email: "user@nonexistent-domain-xyz.com" } }),
  });
  const res = await handleBeforeUserCreated(req, { checkDns: async () => false });
  assert(res.status === 403, `Expected 403, got ${res.status}`);
});

Deno.test("allows valid domain passing MX check", async () => {
  const req = new Request("http://localhost/before-user-created", {
    method: "POST",
    body: JSON.stringify({ user: { email: "alex@proton.me" } }),
  });
  const res = await handleBeforeUserCreated(req, { checkDns: async () => true });
  assert(res.status === 200, `Expected 200, got ${res.status}`);
});

Deno.test("rejects unsigned webhook request when hookSecret configured", async () => {
  const req = new Request("http://localhost/before-user-created", {
    method: "POST",
    body: JSON.stringify({ user: { email: "alex@proton.me" } }),
  });
  const res = await handleBeforeUserCreated(req, {
    hookSecret: "test-secret",
    checkDns: async () => true,
  });
  assert(res.status === 401, `Expected 401, got ${res.status}`);
});
