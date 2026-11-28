// Supabase Auth HTTPS `before-user-created` Edge Function
// Enforces disposable domain filtering, reserved-TLD rejection, constant-time HMAC webhook verification,
// and DNS MX/A verification before GoTrue inserts any new user record.

export const DISPOSABLE_EMAIL_DOMAINS = new Set<string>([
  "10minutemail.com",
  "10minutemail.net",
  "10minutemail.org",
  "20minutemail.com",
  "anonbox.net",
  "binkmail.com",
  "bobmail.info",
  "byom.de",
  "chammy.info",
  "cool.fr.nf",
  "courriel.fr.nf",
  "crazymailing.com",
  "devnullmail.com",
  "discard.email",
  "discardmail.com",
  "discardmail.de",
  "dispomail.eu",
  "disposableaddress.com",
  "disposableemailaddresses.com",
  "dispostable.com",
  "dodgeit.com",
  "dodgit.com",
  "drdrb.net",
  "dropmail.me",
  "duck2.club",
  "e4ward.com",
  "emailfake.com",
  "emailondeck.com",
  "emailtemporario.com.br",
  "emeil.in",
  "emeil.ir",
  "example.com",
  "example.net",
  "example.org",
  "fakemailgenerator.com",
  "fakeinbox.com",
  "fake-mail.cf",
  "fake-mail.ga",
  "fake-mail.ml",
  "filzmail.com",
  "ftml.net",
  "getairmail.com",
  "getnada.com",
  "gishpuppy.com",
  "gowikibooks.com",
  "grr.la",
  "guerrillamail.biz",
  "guerrillamail.com",
  "guerrillamail.de",
  "guerrillamail.info",
  "guerrillamail.net",
  "guerrillamail.org",
  "haltospam.com",
  "harakirimail.com",
  "hatespam.org",
  "hidemail.de",
  "ieh-mail.de",
  "imails.info",
  "inboxalias.com",
  "incognitomail.com",
  "incognitomail.org",
  "instant-mail.de",
  "ip6.li",
  "jetable.com",
  "jetable.fr.nf",
  "jetable.org",
  "junk1e.com",
  "keepmymail.com",
  "klzlk.com",
  "koszmail.pl",
  "kurzepost.de",
  "lhsdv.com",
  "lookugly.com",
  "lortemail.dk",
  "lr78.com",
  "luxusmail.org",
  "m4ilweb.info",
  "mail-temporaire.fr",
  "mail4trash.com",
  "mailbidon.com",
  "mailblocks.com",
  "mailbucket.org",
  "mailcatch.com",
  "maildrop.cc",
  "maildx.com",
  "maileater.com",
  "mailexpire.com",
  "mailforspam.com",
  "mailfreeonline.com",
  "mailin8r.com",
  "mailinator.com",
  "mailinator.net",
  "mailinator.org",
  "mailinator.us",
  "mailinator2.com",
  "mailme.lv",
  "mailmetrash.com",
  "mailmoat.com",
  "mailnesia.com",
  "mailnull.com",
  "mailscrap.com",
  "mailshell.com",
  "mailsiphon.com",
  "mailslite.com",
  "mailtemp.info",
  "mailzilla.com",
  "makemetheking.com",
  "mbx.cc",
  "mega.zik.dj",
  "meinspamschutz.de",
  "meltmail.com",
  "messagebeamer.de",
  "mierdamail.com",
  "mintemail.com",
  "moakt.com",
  "mobi.web.id",
  "moburl.com",
  "mohmal.com",
  "moncourrier.fr.nf",
  "monemail.fr.nf",
  "monmail.fr.nf",
  "mt2009.com",
  "mx0.wwwnew.eu",
  "mypartyclip.de",
  "myphantomemail.com",
  "mytemp.email",
  "nada.email",
  "nepwk.com",
  "nervmich.net",
  "nervtmich.net",
  "netzidiot.de",
  "no-spam.ws",
  "nobulk.com",
  "noclickemail.com",
  "nogmailspam.info",
  "nomail.xl.cx",
  "nomail2me.com",
  "nospam.ze.tc",
  "nospam4.us",
  "nospamfor.us",
  "nospamthanks.info",
  "notmailinator.com",
  "nowmymail.com",
  "nurfuerspam.de",
  "nwldx.com",
  "odaymail.com",
  "oneoffemail.com",
  "onewaymail.com",
  "online.ms",
  "oopi.org",
  "ordinaryamerican.net",
  "otherinbox.com",
  "ovpn.to",
  "owlpic.com",
  "pancakemail.com",
  "pjkh.com",
  "pjjkp.com",
  "plexolan.de",
  "poofy.org",
  "pookmail.com",
  "privacy.net",
  "proxymail.eu",
  "punkass.com",
  "put2.net",
  "putthisinyourspamdatabase.com",
  "quickinbox.com",
  "rcpt.at",
  "receiveee.com",
  "rmqkr.net",
  "rppkn.com",
  "safetymail.info",
  "sendspamhere.com",
  "sharklasers.com",
  "sofort-mail.de",
  "spamavert.com",
  "spambog.com",
  "spambog.de",
  "spambog.ru",
  "spambox.us",
  "spamcannon.com",
  "spamcannon.net",
  "spamcon.org",
  "spamcowboy.com",
  "spamcowboy.net",
  "spamday.com",
  "spamex.com",
  "spamfree24.org",
  "spamgourmet.com",
  "spamherelots.com",
  "spamhereplease.com",
  "spamhole.com",
  "spamify.com",
  "spaml.com",
  "spaml.de",
  "spammotel.com",
  "spamobox.com",
  "spamslicer.com",
  "spamspot.com",
  "spamthis.co.uk",
  "speed.1s.fr",
  "superrito.com",
  "suremail.info",
  "temp-mail.io",
  "temp-mail.org",
  "tempail.com",
  "tempmail.com",
  "tempmail.net",
  "tempr.email",
  "thankyou2010.com",
  "thisisnotmyrealemail.com",
  "throwawaymail.com",
  "tmailinator.com",
  "tmpmail.net",
  "tmpmail.org",
  "tradermail.info",
  "trash-mail.at",
  "trash-mail.com",
  "trash2009.com",
  "trashmail.at",
  "trashmail.com",
  "trashmail.me",
  "trashmail.net",
  "trashmail.org",
  "tyldd.com",
  "upliftnow.com",
  "venompen.com",
  "veryrealemail.com",
  "watchfull.net",
  "wegwerfmail.de",
  "wegwerfmail.net",
  "wegwerfmail.org",
  "wh4f.org",
  "willselfdestruct.com",
  "xagloo.com",
  "xemaps.com",
  "xents.com",
  "xmaily.com",
  "xoxy.net",
  "yep.it",
  "yogamaven.com",
  "yopmail.com",
  "yopmail.fr",
  "yopmail.net",
  "yuurok.com",
  "zehnminuten.de",
  "zippymail.info",
]);

const RESERVED_SUFFIXES = [
  "localhost",
  ".local",
  ".internal",
  ".invalid",
  ".test",
  ".example",
  ".localhost",
  ".arpa",
];

export type DnsChecker = (domain: string) => Promise<boolean>;

export interface EmailCheckOutcome {
  allowed: boolean;
  reason?: string;
}

export function extractDomainCandidates(domain: string): string[] {
  const parts = domain.split(".");
  const out: string[] = [];
  for (let i = 0; i <= parts.length - 2; i++) {
    out.push(parts.slice(i).join("."));
  }
  return out;
}

export function validateEmailSyntaxAndDomain(rawEmail: string): EmailCheckOutcome & { domain?: string } {
  const email = rawEmail.trim().toLowerCase();
  if (!email) {
    return { allowed: false, reason: "A verified email address is required." };
  }
  const atIndex = email.indexOf("@");
  if (atIndex <= 0 || atIndex !== email.lastIndexOf("@")) {
    return { allowed: false, reason: "Invalid email format." };
  }
  const local = email.slice(0, atIndex);
  const domain = email.slice(atIndex + 1).replace(/\.+$/, "");
  if (local.length === 0 || local.length > 64 || /\s/.test(local)) {
    return { allowed: false, reason: "Invalid email username." };
  }
  if (
    domain.length < 3 ||
    domain.length > 253 ||
    !/^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$/.test(domain)
  ) {
    return { allowed: false, reason: "Invalid email domain format." };
  }
  if (domain === "localhost" || RESERVED_SUFFIXES.some((s) => domain.endsWith(s))) {
    return { allowed: false, reason: "Reserved or non-routable email domains are not allowed." };
  }
  const tld = domain.split(".").pop() ?? "";
  if (tld.length < 2 || /^\d+$/.test(tld)) {
    return { allowed: false, reason: "Email top-level domain is invalid." };
  }
  const candidates = extractDomainCandidates(domain);
  if (candidates.some((c) => DISPOSABLE_EMAIL_DOMAINS.has(c))) {
    return {
      allowed: false,
      reason: "Temporary or disposable email providers cannot be used to register.",
    };
  }
  return { allowed: true, domain };
}

export async function defaultMxDnsCheck(domain: string): Promise<boolean> {
  try {
    const mxRecords = await Deno.resolveDns(domain, "MX");
    const usableMx = mxRecords.some((r) => r.exchange && r.exchange !== "." && r.exchange.length > 1);
    if (usableMx) return true;
  } catch (err) {
    if (err instanceof Deno.errors.NotFound) {
      return false;
    }
    // Fail open only on transient resolver timeout/permission errors, never on NotFound.
  }
  try {
    const aRecords = await Deno.resolveDns(domain, "A");
    return aRecords.length > 0;
  } catch (err) {
    return !(err instanceof Deno.errors.NotFound);
  }
}

export function timingSafeEqualBytes(a: Uint8Array, b: Uint8Array): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a[i] ^ b[i];
  }
  return diff === 0;
}

function decodeBase64(b64: string): Uint8Array {
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

export async function verifyWebhookHmac(
  rawBody: string,
  headers: Headers,
  hookSecret: string,
): Promise<boolean> {
  if (!hookSecret) return true;
  const cleanSecret = hookSecret.replace(/^v1,whsec_/, "");
  const keyBytes = hookSecret.startsWith("v1,whsec_")
    ? decodeBase64(cleanSecret)
    : new TextEncoder().encode(hookSecret);

  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    keyBytes,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const stdSig = headers.get("webhook-signature");
  const webhookId = headers.get("webhook-id") ?? "";
  const webhookTs = headers.get("webhook-timestamp") ?? "";
  if (stdSig && webhookId && webhookTs) {
    const signedPayload = `${webhookId}.${webhookTs}.${rawBody}`;
    const mac = new Uint8Array(
      await crypto.subtle.sign("HMAC", cryptoKey, new TextEncoder().encode(signedPayload)),
    );
    const sigTokens = stdSig.split(" ");
    return sigTokens.some((tok) => {
      if (!tok.startsWith("v1,")) return false;
      try {
        return timingSafeEqualBytes(mac, decodeBase64(tok.slice(3)));
      } catch {
        return false;
      }
    });
  }

  const supabaseSig = headers.get("x-supabase-signature");
  if (supabaseSig) {
    const mac = new Uint8Array(
      await crypto.subtle.sign("HMAC", cryptoKey, new TextEncoder().encode(rawBody)),
    );
    const hex = Array.from(mac).map((b) => b.toString(16).padStart(2, "0")).join("");
    const encoder = new TextEncoder();
    return timingSafeEqualBytes(encoder.encode(hex), encoder.encode(supabaseSig.toLowerCase()));
  }

  return false;
}

export async function handleBeforeUserCreated(
  req: Request,
  options: { hookSecret?: string; checkDns?: DnsChecker } = {},
): Promise<Response> {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: { http_code: 405, message: "Method not allowed" } }), {
      status: 405,
      headers: { "Content-Type": "application/json" },
    });
  }

  const rawBody = await req.text();
  const secret = options.hookSecret ?? Deno.env.get("BEFORE_USER_CREATED_HOOK_SECRET") ?? "";
  const sigValid = await verifyWebhookHmac(rawBody, req.headers, secret);
  if (!sigValid) {
    return new Response(JSON.stringify({ error: { http_code: 401, message: "Invalid hook signature" } }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  }

  let email = "";
  try {
    const parsed = JSON.parse(rawBody);
    email = typeof parsed?.user?.email === "string" ? parsed.user.email : "";
  } catch {
    return new Response(JSON.stringify({ error: { http_code: 400, message: "Malformed JSON payload" } }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    });
  }

  const check = validateEmailSyntaxAndDomain(email);
  if (!check.allowed || !check.domain) {
    return new Response(
      JSON.stringify({
        error: {
          http_code: 403,
          message: check.reason ?? "Email domain not permitted.",
        },
      }),
      { status: 403, headers: { "Content-Type": "application/json" } },
    );
  }

  const dnsChecker = options.checkDns ?? defaultMxDnsCheck;
  const hasMx = await dnsChecker(check.domain);
  if (!hasMx) {
    return new Response(
      JSON.stringify({
        error: {
          http_code: 403,
          message: "Email domain does not accept mail (no valid MX/A record).",
        },
      }),
      { status: 403, headers: { "Content-Type": "application/json" } },
    );
  }

  return new Response(JSON.stringify({}), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

if (import.meta.main) {
  Deno.serve((req) => handleBeforeUserCreated(req));
}
