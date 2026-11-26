-- OpenCult initial PostgreSQL schema, 10-profile limit, strict RLS, and Realtime publication
-- Apply via: supabase db push

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- 1. Shared trigger helpers
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------------------
-- 2. household_profiles (max 10 active profiles per account)
-- ---------------------------------------------------------------------------

CREATE TABLE public.household_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name text NOT NULL CHECK (char_length(btrim(display_name)) BETWEEN 1 AND 60),
    avatar_color text NOT NULL DEFAULT '#2185F7' CHECK (avatar_color ~ '^#[0-9A-Fa-f]{6}$'),
    biological_sex text NOT NULL CHECK (biological_sex IN ('male', 'female')),
    birth_date date NOT NULL,
    height_cm numeric(5,2) NOT NULL CHECK (height_cm BETWEEN 50.00 AND 260.00),
    activity_level text NOT NULL DEFAULT 'moderate' CHECK (activity_level IN ('sedentary', 'mild', 'moderate', 'heavy', 'extreme')),
    athlete_mode boolean NOT NULL DEFAULT false,
    auto_match_window_kg numeric(4,2) NOT NULL DEFAULT 2.50 CHECK (auto_match_window_kg BETWEEN 0.20 AND 15.00),
    target_weight_kg numeric(5,2) CHECK (target_weight_kg IS NULL OR target_weight_kg BETWEEN 20.00 AND 300.00),
    target_body_fat_pct numeric(4,2) CHECK (target_body_fat_pct IS NULL OR target_body_fat_pct BETWEEN 3.00 AND 65.00),
    is_primary_hc_user boolean NOT NULL DEFAULT false,
    sort_order smallint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE OR REPLACE FUNCTION public.enforce_max_10_profiles()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
    active_count integer;
BEGIN
    IF NEW.deleted_at IS NULL THEN
        SELECT count(*)
        INTO active_count
        FROM public.household_profiles
        WHERE account_id = NEW.account_id
          AND deleted_at IS NULL
          AND id <> NEW.id;

        IF active_count >= 10 THEN
            RAISE EXCEPTION 'Maximum of 10 active household profiles reached for account %', NEW.account_id
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_household_profiles_max10
    BEFORE INSERT OR UPDATE ON public.household_profiles
    FOR EACH ROW EXECUTE FUNCTION public.enforce_max_10_profiles();

CREATE TRIGGER trg_household_profiles_updated_at
    BEFORE UPDATE ON public.household_profiles
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE INDEX idx_household_profiles_account_updated
    ON public.household_profiles (account_id, updated_at DESC);

-- ---------------------------------------------------------------------------
-- 3. scale_devices (4-electrode CS-BF01 and 8-electrode Smart Scale Pro)
-- ---------------------------------------------------------------------------

CREATE TABLE public.scale_devices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    ble_mac_or_id text NOT NULL CHECK (char_length(btrim(ble_mac_or_id)) BETWEEN 1 AND 128),
    advertised_name text NOT NULL CHECK (char_length(btrim(advertised_name)) BETWEEN 1 AND 120),
    model_tier text NOT NULL CHECK (model_tier IN ('cult_pro_8e', 'cult_bf01_4e', 'generic_ble')),
    electrode_count smallint NOT NULL CHECK (electrode_count IN (4, 8)),
    firmware_notes text,
    last_battery_pct smallint CHECK (last_battery_pct IS NULL OR last_battery_pct BETWEEN 0 AND 100),
    last_seen_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT uq_scale_devices_account_mac UNIQUE (account_id, ble_mac_or_id)
);

CREATE TRIGGER trg_scale_devices_updated_at
    BEFORE UPDATE ON public.scale_devices
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE INDEX idx_scale_devices_account_updated
    ON public.scale_devices (account_id, updated_at DESC);

-- ---------------------------------------------------------------------------
-- 4. measurements (56-parameter payload + provenance + soft-delete tombstone)
-- ---------------------------------------------------------------------------

CREATE TABLE public.measurements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    profile_id uuid NOT NULL REFERENCES public.household_profiles(id) ON DELETE CASCADE,
    scale_device_id uuid REFERENCES public.scale_devices(id) ON DELETE SET NULL,
    measured_at timestamptz NOT NULL,
    source text NOT NULL CHECK (source IN ('ble_8e_handles', 'ble_4e_feet', 'manual', 'health_connect_import', 'csv_import')),
    assignment_state text NOT NULL DEFAULT 'confirmed' CHECK (assignment_state IN ('confirmed', 'pending_review')),
    weight_kg numeric(6,2) NOT NULL CHECK (weight_kg BETWEEN 2.00 AND 400.00),
    heart_rate_bpm smallint CHECK (heart_rate_bpm IS NULL OR heart_rate_bpm BETWEEN 25 AND 240),
    impedance_50khz_ohm numeric(7,2) CHECK (impedance_50khz_ohm IS NULL OR impedance_50khz_ohm > 0),
    impedance_high_ohm numeric(7,2) CHECK (impedance_high_ohm IS NULL OR impedance_high_ohm > 0),
    metrics_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    provenance_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    notes text,
    mutation_id uuid NOT NULL DEFAULT gen_random_uuid(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);

CREATE TRIGGER trg_measurements_updated_at
    BEFORE UPDATE ON public.measurements
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE INDEX idx_measurements_account_updated
    ON public.measurements (account_id, updated_at DESC);

CREATE INDEX idx_measurements_profile_measured
    ON public.measurements (profile_id, measured_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 5. raw_ble_packets (immutable raw 0xCF / 0xBE / 0xDF frames, INV-01)
-- ---------------------------------------------------------------------------

CREATE TABLE public.raw_ble_packets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    measurement_id uuid NOT NULL REFERENCES public.measurements(id) ON DELETE CASCADE,
    seq_no smallint NOT NULL CHECK (seq_no >= 0),
    characteristic_uuid text NOT NULL CHECK (char_length(btrim(characteristic_uuid)) BETWEEN 4 AND 64),
    packet_type_hex text NOT NULL CHECK (packet_type_hex ~ '^0x[0-9A-Fa-f]{2}$'),
    payload_hex text NOT NULL CHECK (payload_hex ~ '^[0-9A-Fa-f]+$'),
    checksum_valid boolean NOT NULL,
    decoded_impedances_ohm numeric(7,2)[],
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_raw_ble_packets_measurement
    ON public.raw_ble_packets (measurement_id, seq_no);

CREATE INDEX idx_raw_ble_packets_account
    ON public.raw_ble_packets (account_id, captured_at DESC);

-- ---------------------------------------------------------------------------
-- 6. health_connect_sync_ledger (deterministic client_record_id, INV-04)
-- ---------------------------------------------------------------------------

CREATE TABLE public.health_connect_sync_ledger (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    measurement_id uuid NOT NULL REFERENCES public.measurements(id) ON DELETE CASCADE,
    hc_record_type text NOT NULL CHECK (char_length(btrim(hc_record_type)) BETWEEN 1 AND 80),
    client_record_id text NOT NULL CHECK (char_length(btrim(client_record_id)) BETWEEN 1 AND 160),
    client_record_version bigint NOT NULL DEFAULT 1 CHECK (client_record_version >= 1),
    hc_record_uid text,
    sync_status text NOT NULL CHECK (sync_status IN ('pending', 'synced', 'deleted', 'failed')),
    last_error text,
    synced_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_hc_ledger_account_client_id UNIQUE (account_id, client_record_id)
);

CREATE TRIGGER trg_hc_ledger_updated_at
    BEFORE UPDATE ON public.health_connect_sync_ledger
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE INDEX idx_hc_ledger_account_updated
    ON public.health_connect_sync_ledger (account_id, updated_at DESC);

CREATE INDEX idx_hc_ledger_measurement
    ON public.health_connect_sync_ledger (measurement_id);

-- ---------------------------------------------------------------------------
-- 7. Strict Row-Level Security (ENABLE + FORCE on every public table, INV-08)
-- ---------------------------------------------------------------------------

ALTER TABLE public.household_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.household_profiles FORCE ROW LEVEL SECURITY;

ALTER TABLE public.scale_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scale_devices FORCE ROW LEVEL SECURITY;

ALTER TABLE public.measurements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.measurements FORCE ROW LEVEL SECURITY;

ALTER TABLE public.raw_ble_packets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.raw_ble_packets FORCE ROW LEVEL SECURITY;

ALTER TABLE public.health_connect_sync_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.health_connect_sync_ledger FORCE ROW LEVEL SECURITY;

-- household_profiles policies
CREATE POLICY household_profiles_select_own ON public.household_profiles
    FOR SELECT TO authenticated USING ((SELECT auth.uid()) = account_id);
CREATE POLICY household_profiles_insert_own ON public.household_profiles
    FOR INSERT TO authenticated WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY household_profiles_update_own ON public.household_profiles
    FOR UPDATE TO authenticated USING ((SELECT auth.uid()) = account_id) WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY household_profiles_delete_own ON public.household_profiles
    FOR DELETE TO authenticated USING ((SELECT auth.uid()) = account_id);

-- scale_devices policies
CREATE POLICY scale_devices_select_own ON public.scale_devices
    FOR SELECT TO authenticated USING ((SELECT auth.uid()) = account_id);
CREATE POLICY scale_devices_insert_own ON public.scale_devices
    FOR INSERT TO authenticated WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY scale_devices_update_own ON public.scale_devices
    FOR UPDATE TO authenticated USING ((SELECT auth.uid()) = account_id) WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY scale_devices_delete_own ON public.scale_devices
    FOR DELETE TO authenticated USING ((SELECT auth.uid()) = account_id);

-- measurements policies
CREATE POLICY measurements_select_own ON public.measurements
    FOR SELECT TO authenticated USING ((SELECT auth.uid()) = account_id);
CREATE POLICY measurements_insert_own ON public.measurements
    FOR INSERT TO authenticated WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY measurements_update_own ON public.measurements
    FOR UPDATE TO authenticated USING ((SELECT auth.uid()) = account_id) WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY measurements_delete_own ON public.measurements
    FOR DELETE TO authenticated USING ((SELECT auth.uid()) = account_id);

-- raw_ble_packets policies (immutable payload: SELECT, INSERT, DELETE only; no UPDATE)
CREATE POLICY raw_ble_packets_select_own ON public.raw_ble_packets
    FOR SELECT TO authenticated USING ((SELECT auth.uid()) = account_id);
CREATE POLICY raw_ble_packets_insert_own ON public.raw_ble_packets
    FOR INSERT TO authenticated WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY raw_ble_packets_delete_own ON public.raw_ble_packets
    FOR DELETE TO authenticated USING ((SELECT auth.uid()) = account_id);

-- health_connect_sync_ledger policies
CREATE POLICY hc_ledger_select_own ON public.health_connect_sync_ledger
    FOR SELECT TO authenticated USING ((SELECT auth.uid()) = account_id);
CREATE POLICY hc_ledger_insert_own ON public.health_connect_sync_ledger
    FOR INSERT TO authenticated WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY hc_ledger_update_own ON public.health_connect_sync_ledger
    FOR UPDATE TO authenticated USING ((SELECT auth.uid()) = account_id) WITH CHECK ((SELECT auth.uid()) = account_id);
CREATE POLICY hc_ledger_delete_own ON public.health_connect_sync_ledger
    FOR DELETE TO authenticated USING ((SELECT auth.uid()) = account_id);

-- ---------------------------------------------------------------------------
-- 8. Zero-Refresh Realtime Publication (INV-07)
-- ---------------------------------------------------------------------------

ALTER TABLE public.household_profiles REPLICA IDENTITY FULL;
ALTER TABLE public.scale_devices REPLICA IDENTITY FULL;
ALTER TABLE public.measurements REPLICA IDENTITY FULL;
ALTER TABLE public.health_connect_sync_ledger REPLICA IDENTITY FULL;

ALTER PUBLICATION supabase_realtime ADD TABLE
    public.household_profiles,
    public.scale_devices,
    public.measurements,
    public.health_connect_sync_ledger;
