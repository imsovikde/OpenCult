-- OpenCult Server-Side Auth Guard: blocks disposable / temporary email domains & malformed emails
-- Runs automatically via GoTrue `before_user_created` Postgres hook even when API clients bypass the UI.
-- Keep schema `private` excluded from Postgrest exposure.

CREATE SCHEMA IF NOT EXISTS private;

REVOKE ALL ON SCHEMA private FROM PUBLIC, anon, authenticated;
GRANT USAGE ON SCHEMA private TO supabase_auth_admin, postgres, service_role;

CREATE TABLE IF NOT EXISTS private.blocked_email_domains (
    domain text PRIMARY KEY CHECK (domain = lower(domain) AND domain !~ '\s'),
    reason text NOT NULL DEFAULT 'disposable',
    created_at timestamptz NOT NULL DEFAULT now()
);

REVOKE ALL ON TABLE private.blocked_email_domains FROM PUBLIC, anon, authenticated;
GRANT SELECT ON TABLE private.blocked_email_domains TO supabase_auth_admin, postgres, service_role;

INSERT INTO private.blocked_email_domains (domain, reason) VALUES
    ('10minutemail.com', 'disposable'),
    ('10minutemail.net', 'disposable'),
    ('10minutemail.org', 'disposable'),
    ('20minutemail.com', 'disposable'),
    ('anonbox.net', 'disposable'),
    ('binkmail.com', 'disposable'),
    ('bobmail.info', 'disposable'),
    ('byom.de', 'disposable'),
    ('chammy.info', 'disposable'),
    ('cool.fr.nf', 'disposable'),
    ('courriel.fr.nf', 'disposable'),
    ('crazymailing.com', 'disposable'),
    ('devnullmail.com', 'disposable'),
    ('discard.email', 'disposable'),
    ('discardmail.com', 'disposable'),
    ('discardmail.de', 'disposable'),
    ('dispomail.eu', 'disposable'),
    ('disposableaddress.com', 'disposable'),
    ('disposableemailaddresses.com', 'disposable'),
    ('dispostable.com', 'disposable'),
    ('dodgeit.com', 'disposable'),
    ('dodgit.com', 'disposable'),
    ('drdrb.net', 'disposable'),
    ('dropmail.me', 'disposable'),
    ('duck2.club', 'disposable'),
    ('e4ward.com', 'disposable'),
    ('emailfake.com', 'disposable'),
    ('emailondeck.com', 'disposable'),
    ('emailtemporario.com.br', 'disposable'),
    ('emeil.in', 'disposable'),
    ('emeil.ir', 'disposable'),
    ('fakemailgenerator.com', 'disposable'),
    ('fakeinbox.com', 'disposable'),
    ('fake-mail.cf', 'disposable'),
    ('fake-mail.ga', 'disposable'),
    ('fake-mail.ml', 'disposable'),
    ('filzmail.com', 'disposable'),
    ('ftml.net', 'disposable'),
    ('getairmail.com', 'disposable'),
    ('getnada.com', 'disposable'),
    ('gishpuppy.com', 'disposable'),
    ('gowikibooks.com', 'disposable'),
    ('grr.la', 'disposable'),
    ('guerrillamail.biz', 'disposable'),
    ('guerrillamail.com', 'disposable'),
    ('guerrillamail.de', 'disposable'),
    ('guerrillamail.info', 'disposable'),
    ('guerrillamail.net', 'disposable'),
    ('guerrillamail.org', 'disposable'),
    ('haltospam.com', 'disposable'),
    ('harakirimail.com', 'disposable'),
    ('hatespam.org', 'disposable'),
    ('hidemail.de', 'disposable'),
    ('ieh-mail.de', 'disposable'),
    ('imails.info', 'disposable'),
    ('inboxalias.com', 'disposable'),
    ('incognitomail.com', 'disposable'),
    ('incognitomail.org', 'disposable'),
    ('instant-mail.de', 'disposable'),
    ('ip6.li', 'disposable'),
    ('jetable.com', 'disposable'),
    ('jetable.fr.nf', 'disposable'),
    ('jetable.org', 'disposable'),
    ('junk1e.com', 'disposable'),
    ('keepmymail.com', 'disposable'),
    ('klzlk.com', 'disposable'),
    ('koszmail.pl', 'disposable'),
    ('kurzepost.de', 'disposable'),
    ('lhsdv.com', 'disposable'),
    ('lookugly.com', 'disposable'),
    ('lortemail.dk', 'disposable'),
    ('lr78.com', 'disposable'),
    ('luxusmail.org', 'disposable'),
    ('m4ilweb.info', 'disposable'),
    ('mail-temporaire.fr', 'disposable'),
    ('mail4trash.com', 'disposable'),
    ('mailbidon.com', 'disposable'),
    ('mailblocks.com', 'disposable'),
    ('mailbucket.org', 'disposable'),
    ('mailcatch.com', 'disposable'),
    ('maildrop.cc', 'disposable'),
    ('maildx.com', 'disposable'),
    ('maileater.com', 'disposable'),
    ('mailexpire.com', 'disposable'),
    ('mailforspam.com', 'disposable'),
    ('mailfreeonline.com', 'disposable'),
    ('mailin8r.com', 'disposable'),
    ('mailinator.com', 'disposable'),
    ('mailinator.net', 'disposable'),
    ('mailinator.org', 'disposable'),
    ('mailinator.us', 'disposable'),
    ('mailinator2.com', 'disposable'),
    ('mailme.lv', 'disposable'),
    ('mailmetrash.com', 'disposable'),
    ('mailmoat.com', 'disposable'),
    ('mailnesia.com', 'disposable'),
    ('mailnull.com', 'disposable'),
    ('mailscrap.com', 'disposable'),
    ('mailshell.com', 'disposable'),
    ('mailsiphon.com', 'disposable'),
    ('mailslite.com', 'disposable'),
    ('mailtemp.info', 'disposable'),
    ('mailzilla.com', 'disposable'),
    ('makemetheking.com', 'disposable'),
    ('mbx.cc', 'disposable'),
    ('mega.zik.dj', 'disposable'),
    ('meinspamschutz.de', 'disposable'),
    ('meltmail.com', 'disposable'),
    ('messagebeamer.de', 'disposable'),
    ('mierdamail.com', 'disposable'),
    ('mintemail.com', 'disposable'),
    ('moakt.com', 'disposable'),
    ('mobi.web.id', 'disposable'),
    ('moburl.com', 'disposable'),
    ('mohmal.com', 'disposable'),
    ('moncourrier.fr.nf', 'disposable'),
    ('monemail.fr.nf', 'disposable'),
    ('monmail.fr.nf', 'disposable'),
    ('mt2009.com', 'disposable'),
    ('mx0.wwwnew.eu', 'disposable'),
    ('mypartyclip.de', 'disposable'),
    ('myphantomemail.com', 'disposable'),
    ('mytemp.email', 'disposable'),
    ('nada.email', 'disposable'),
    ('nepwk.com', 'disposable'),
    ('nervmich.net', 'disposable'),
    ('nervtmich.net', 'disposable'),
    ('netzidiot.de', 'disposable'),
    ('no-spam.ws', 'disposable'),
    ('nobulk.com', 'disposable'),
    ('noclickemail.com', 'disposable'),
    ('nogmailspam.info', 'disposable'),
    ('nomail.xl.cx', 'disposable'),
    ('nomail2me.com', 'disposable'),
    ('nospam.ze.tc', 'disposable'),
    ('nospam4.us', 'disposable'),
    ('nospamfor.us', 'disposable'),
    ('nospamthanks.info', 'disposable'),
    ('notmailinator.com', 'disposable'),
    ('nowmymail.com', 'disposable'),
    ('nurfuerspam.de', 'disposable'),
    ('nwldx.com', 'disposable'),
    ('odaymail.com', 'disposable'),
    ('oneoffemail.com', 'disposable'),
    ('onewaymail.com', 'disposable'),
    ('online.ms', 'disposable'),
    ('oopi.org', 'disposable'),
    ('ordinaryamerican.net', 'disposable'),
    ('otherinbox.com', 'disposable'),
    ('ovpn.to', 'disposable'),
    ('owlpic.com', 'disposable'),
    ('pancakemail.com', 'disposable'),
    ('pjkh.com', 'disposable'),
    ('pjjkp.com', 'disposable'),
    ('plexolan.de', 'disposable'),
    ('poofy.org', 'disposable'),
    ('pookmail.com', 'disposable'),
    ('privacy.net', 'disposable'),
    ('proxymail.eu', 'disposable'),
    ('punkass.com', 'disposable'),
    ('put2.net', 'disposable'),
    ('putthisinyourspamdatabase.com', 'disposable'),
    ('quickinbox.com', 'disposable'),
    ('rcpt.at', 'disposable'),
    ('receiveee.com', 'disposable'),
    ('rmqkr.net', 'disposable'),
    ('rppkn.com', 'disposable'),
    ('safetymail.info', 'disposable'),
    ('sendspamhere.com', 'disposable'),
    ('sharklasers.com', 'disposable'),
    ('sofort-mail.de', 'disposable'),
    ('spamavert.com', 'disposable'),
    ('spambog.com', 'disposable'),
    ('spambog.de', 'disposable'),
    ('spambog.ru', 'disposable'),
    ('spambox.us', 'disposable'),
    ('spamcannon.com', 'disposable'),
    ('spamcannon.net', 'disposable'),
    ('spamcon.org', 'disposable'),
    ('spamcowboy.com', 'disposable'),
    ('spamcowboy.net', 'disposable'),
    ('spamday.com', 'disposable'),
    ('spamex.com', 'disposable'),
    ('spamfree24.org', 'disposable'),
    ('spamgourmet.com', 'disposable'),
    ('spamherelots.com', 'disposable'),
    ('spamhereplease.com', 'disposable'),
    ('spamhole.com', 'disposable'),
    ('spamify.com', 'disposable'),
    ('spaml.com', 'disposable'),
    ('spaml.de', 'disposable'),
    ('spammotel.com', 'disposable'),
    ('spamobox.com', 'disposable'),
    ('spamslicer.com', 'disposable'),
    ('spamspot.com', 'disposable'),
    ('spamthis.co.uk', 'disposable'),
    ('speed.1s.fr', 'disposable'),
    ('superrito.com', 'disposable'),
    ('suremail.info', 'disposable'),
    ('temp-mail.io', 'disposable'),
    ('temp-mail.org', 'disposable'),
    ('tempail.com', 'disposable'),
    ('tempmail.com', 'disposable'),
    ('tempmail.net', 'disposable'),
    ('tempr.email', 'disposable'),
    ('thankyou2010.com', 'disposable'),
    ('thisisnotmyrealemail.com', 'disposable'),
    ('throwawaymail.com', 'disposable'),
    ('tmailinator.com', 'disposable'),
    ('tmpmail.net', 'disposable'),
    ('tmpmail.org', 'disposable'),
    ('tradermail.info', 'disposable'),
    ('trash-mail.at', 'disposable'),
    ('trash-mail.com', 'disposable'),
    ('trash2009.com', 'disposable'),
    ('trashmail.at', 'disposable'),
    ('trashmail.com', 'disposable'),
    ('trashmail.me', 'disposable'),
    ('trashmail.net', 'disposable'),
    ('trashmail.org', 'disposable'),
    ('tyldd.com', 'disposable'),
    ('upliftnow.com', 'disposable'),
    ('venompen.com', 'disposable'),
    ('veryrealemail.com', 'disposable'),
    ('watchfull.net', 'disposable'),
    ('wegwerfmail.de', 'disposable'),
    ('wegwerfmail.net', 'disposable'),
    ('wegwerfmail.org', 'disposable'),
    ('wh4f.org', 'disposable'),
    ('willselfdestruct.com', 'disposable'),
    ('xagloo.com', 'disposable'),
    ('xemaps.com', 'disposable'),
    ('xents.com', 'disposable'),
    ('xmaily.com', 'disposable'),
    ('xoxy.net', 'disposable'),
    ('yep.it', 'disposable'),
    ('yogamaven.com', 'disposable'),
    ('yopmail.com', 'disposable'),
    ('yopmail.fr', 'disposable'),
    ('yopmail.net', 'disposable'),
    ('yuurok.com', 'disposable'),
    ('zehnminuten.de', 'disposable'),
    ('zippymail.info', 'disposable'),
    ('example.com', 'reserved_domain'),
    ('example.net', 'reserved_domain'),
    ('example.org', 'reserved_domain')
ON CONFLICT (domain) DO NOTHING;

CREATE OR REPLACE FUNCTION private.normalize_email_domain(raw_email text)
RETURNS text
LANGUAGE plpgsql
IMMUTABLE
SET search_path = private, pg_catalog
AS $$
DECLARE
    cleaned text;
    at_idx integer;
    dom text;
BEGIN
    cleaned := lower(btrim(coalesce(raw_email, '')));
    at_idx := position('@' in cleaned);
    IF at_idx <= 1 OR position('@' in substring(cleaned from at_idx + 1)) > 0 THEN
        RETURN NULL;
    END IF;
    dom := regexp_replace(substring(cleaned from at_idx + 1), '\.+$', '');
    IF char_length(dom) < 3 OR char_length(dom) > 253 THEN
        RETURN NULL;
    END IF;
    IF dom !~ '^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$' THEN
        RETURN NULL;
    END IF;
    RETURN dom;
END;
$$;

CREATE OR REPLACE FUNCTION private.before_user_created_hook(event jsonb)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = private, pg_catalog
AS $$
DECLARE
    raw_email text;
    local_part text;
    dom text;
    candidate text;
    parts text[];
    idx integer;
    is_blocked boolean := false;
BEGIN
    raw_email := lower(btrim(coalesce(event -> 'user' ->> 'email', '')));
    IF raw_email = '' THEN
        RETURN jsonb_build_object(
            'error',
            jsonb_build_object(
                'http_code', 403,
                'message', 'A verified email address is required to create an OpenCult account.'
            )
        );
    END IF;

    dom := private.normalize_email_domain(raw_email);
    IF dom IS NULL THEN
        RETURN jsonb_build_object(
            'error',
            jsonb_build_object(
                'http_code', 403,
                'message', 'Invalid email domain format.'
            )
        );
    END IF;

    local_part := split_part(raw_email, '@', 1);
    IF char_length(local_part) > 64 OR local_part ~ '\s' THEN
        RETURN jsonb_build_object(
            'error',
            jsonb_build_object(
                'http_code', 403,
                'message', 'Invalid email address format.'
            )
        );
    END IF;

    IF dom = 'localhost' OR dom ~ '\.(local|internal|invalid|test|example|localhost|arpa)$' THEN
        RETURN jsonb_build_object(
            'error',
            jsonb_build_object(
                'http_code', 403,
                'message', 'Non-routable or reserved email domains are not permitted.'
            )
        );
    END IF;

    parts := string_to_array(dom, '.');
    IF array_length(parts, 1) >= 2 THEN
        FOR idx IN 1 .. (array_length(parts, 1) - 1) LOOP
            candidate := array_to_string(parts[idx : array_length(parts, 1)], '.');
            IF EXISTS (
                SELECT 1
                FROM private.blocked_email_domains b
                WHERE b.domain = candidate
            ) THEN
                is_blocked := true;
                EXIT;
            END IF;
        END LOOP;
    END IF;

    IF is_blocked THEN
        RETURN jsonb_build_object(
            'error',
            jsonb_build_object(
                'http_code', 403,
                'message', 'Temporary or disposable email providers cannot be used to register.'
            )
        );
    END IF;

    RETURN '{}'::jsonb;
END;
$$;

REVOKE ALL ON FUNCTION private.normalize_email_domain(text) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION private.before_user_created_hook(jsonb) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION private.normalize_email_domain(text) TO supabase_auth_admin, service_role;
GRANT EXECUTE ON FUNCTION private.before_user_created_hook(jsonb) TO supabase_auth_admin, service_role;
