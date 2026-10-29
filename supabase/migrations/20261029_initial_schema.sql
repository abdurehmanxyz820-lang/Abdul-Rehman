-- MediSync AI - Production PostgreSQL Database Schema & Migrations
-- Designed for Supabase PostgreSQL with Row Level Security (RLS)

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. ENUMS & DOMAINS
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('patient', 'caregiver', 'agency_admin', 'system_admin');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE dose_status AS ENUM ('pending', 'taken', 'skipped', 'missed', 'snoozed');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE alert_severity AS ENUM ('info', 'warning', 'critical');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE subscription_tier AS ENUM ('family_care', 'agency_pro', 'enterprise');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 3. ORGANIZATIONS (Agencies & Healthcare Facilities)
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    license_number VARCHAR(100),
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    tier subscription_tier DEFAULT 'agency_pro',
    max_patients INTEGER DEFAULT 50,
    max_caregivers INTEGER DEFAULT 20,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. PROFILES (Extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'patient',
    org_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    phone_number VARCHAR(50),
    avatar_url TEXT,
    timezone VARCHAR(50) DEFAULT 'UTC',
    biometric_enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. ORGANIZATION MEMBERS (Role assignments in agencies)
CREATE TABLE IF NOT EXISTS organization_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    role user_role NOT NULL DEFAULT 'caregiver',
    is_active BOOLEAN DEFAULT true,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(org_id, user_id)
);

-- 6. PATIENTS
CREATE TABLE IF NOT EXISTS patients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    full_name VARCHAR(255) NOT NULL,
    dob DATE,
    gender VARCHAR(50),
    room_number VARCHAR(50),
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    allergies TEXT[] DEFAULT ARRAY[]::TEXT[],
    medical_conditions TEXT[] DEFAULT ARRAY[]::TEXT[],
    adherence_rate NUMERIC(5,2) DEFAULT 100.0,
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. CAREGIVER-PATIENT ASSIGNMENTS
CREATE TABLE IF NOT EXISTS caregiver_patient_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caregiver_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ DEFAULT NOW(),
    is_primary BOOLEAN DEFAULT false,
    UNIQUE(caregiver_id, patient_id)
);

-- 8. MEDICATIONS
CREATE TABLE IF NOT EXISTS medications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    dosage VARCHAR(50) NOT NULL,
    unit VARCHAR(50) DEFAULT 'mg',
    route VARCHAR(50) DEFAULT 'Oral', -- Oral, Sublingual, Injection, Inhaler, Topical, Drops
    instructions TEXT, -- e.g. "Take with a full glass of water after meals"
    prescriber_name VARCHAR(255),
    rx_number VARCHAR(100),
    requires_food BOOLEAN DEFAULT false,
    color_hex VARCHAR(20) DEFAULT '#006699',
    shape VARCHAR(50) DEFAULT 'capsule', -- capsule, round, oval, liquid
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. MEDICATION SCHEDULES
CREATE TABLE IF NOT EXISTS medication_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_id UUID NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
    frequency VARCHAR(50) DEFAULT 'daily', -- daily, specific_days, interval, prn
    times_of_day TIME[] NOT NULL, -- e.g. ARRAY['08:00:00'::TIME, '20:00:00'::TIME]
    days_of_week INTEGER[] DEFAULT ARRAY[1,2,3,4,5,6,7], -- 1=Mon, 7=Sun
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. DOSE EVENTS (Every scheduled dose instance)
CREATE TABLE IF NOT EXISTS dose_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_id UUID NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    scheduled_time TIMESTAMPTZ NOT NULL,
    status dose_status NOT NULL DEFAULT 'pending',
    recorded_at TIMESTAMPTZ,
    recorded_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    snooze_until TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(medication_id, scheduled_time)
);

-- 11. NOTIFICATIONS & ALERTS
CREATE TABLE IF NOT EXISTS alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    patient_id UUID REFERENCES patients(id) ON DELETE CASCADE,
    severity alert_severity NOT NULL DEFAULT 'warning',
    type VARCHAR(100) NOT NULL, -- 'missed_dose', 'low_adherence', 'critical_warning', 'interaction'
    message TEXT NOT NULL,
    is_resolved BOOLEAN DEFAULT false,
    resolved_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 12. CARE NOTES
CREATE TABLE IF NOT EXISTS care_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    note_type VARCHAR(50) DEFAULT 'general', -- 'general', 'vital', 'incident', 'med_reaction'
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 13. AUDIT LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL, -- 'DOSE_LOGGED', 'MEDICATION_ADDED', 'PATIENT_EXPORTED'
    resource_type VARCHAR(100) NOT NULL,
    resource_id UUID,
    metadata JSONB DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 14. REGISTERED DEVICES
CREATE TABLE IF NOT EXISTS registered_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    device_token TEXT NOT NULL,
    platform VARCHAR(50) NOT NULL, -- 'android', 'ios', 'web'
    app_version VARCHAR(50),
    last_active TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, device_token)
);

-- 15. SUBSCRIPTIONS & BILLING
CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_tier subscription_tier UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    monthly_price_cents INTEGER NOT NULL,
    features TEXT[] DEFAULT ARRAY[]::TEXT[],
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS billing_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    user_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    plan_tier subscription_tier NOT NULL,
    amount_paid_cents INTEGER NOT NULL,
    currency VARCHAR(10) DEFAULT 'usd',
    payment_status VARCHAR(50) DEFAULT 'active',
    current_period_end TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 16. INDEXES FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_profiles_role ON profiles(role);
CREATE INDEX IF NOT EXISTS idx_patients_org ON patients(org_id);
CREATE INDEX IF NOT EXISTS idx_medications_patient ON medications(patient_id);
CREATE INDEX IF NOT EXISTS idx_dose_events_patient_scheduled ON dose_events(patient_id, scheduled_time);
CREATE INDEX IF NOT EXISTS idx_dose_events_status ON dose_events(status);
CREATE INDEX IF NOT EXISTS idx_alerts_patient ON alerts(patient_id, is_resolved);
CREATE INDEX IF NOT EXISTS idx_care_notes_patient ON care_notes(patient_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_org ON audit_logs(org_id, created_at DESC);

-- 17. ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE caregiver_patient_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE medications ENABLE ROW LEVEL SECURITY;
ALTER TABLE medication_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE dose_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE care_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE registered_devices ENABLE ROW LEVEL SECURITY;

-- Helper functions for RLS
CREATE OR REPLACE FUNCTION auth_user_role() RETURNS user_role AS $$
    SELECT role FROM profiles WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

CREATE OR REPLACE FUNCTION auth_user_org_id() RETURNS UUID AS $$
    SELECT org_id FROM profiles WHERE id = auth.uid();
$$ LANGUAGE sql STABLE SECURITY DEFINER;

-- Profiles: Users can view their own profile, agency admins see org profiles
CREATE POLICY "Users view own profile" ON profiles
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Agency admins view org profiles" ON profiles
    FOR SELECT USING (
        auth_user_role() = 'agency_admin' AND org_id = auth_user_org_id()
    );

CREATE POLICY "Users update own profile" ON profiles
    FOR UPDATE USING (auth.uid() = id);

-- Patients: Patient views self; assigned caregiver views patient; agency admin views org patients
CREATE POLICY "Patient views own record" ON patients
    FOR SELECT USING (profile_id = auth.uid());

CREATE POLICY "Caregiver views assigned patients" ON patients
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM caregiver_patient_assignments
            WHERE caregiver_id = auth.uid() AND patient_id = patients.id
        )
    );

CREATE POLICY "Agency admin views org patients" ON patients
    FOR ALL USING (
        auth_user_role() = 'agency_admin' AND org_id = auth_user_org_id()
    );

-- Medications & Doses: Accessible if authorized for the patient
CREATE POLICY "Patient views own medications" ON medications
    FOR SELECT USING (
        patient_id IN (SELECT id FROM patients WHERE profile_id = auth.uid())
    );

CREATE POLICY "Caregiver manages assigned patient medications" ON medications
    FOR ALL USING (
        patient_id IN (
            SELECT patient_id FROM caregiver_patient_assignments WHERE caregiver_id = auth.uid()
        )
    );

CREATE POLICY "Dose events patient or caregiver access" ON dose_events
    FOR ALL USING (
        patient_id IN (
            SELECT id FROM patients WHERE profile_id = auth.uid()
            UNION
            SELECT patient_id FROM caregiver_patient_assignments WHERE caregiver_id = auth.uid()
        )
    );

-- Insert Default Subscription Tiers
INSERT INTO subscription_plans (plan_tier, name, monthly_price_cents, features)
VALUES 
    ('family_care', 'Family Care', 999, ARRAY['Up to 2 Patients', 'Caregiver Notifications', 'AI Medication Assistant', 'Adherence Reports']),
    ('agency_pro', 'Home-Care Agency Pro', 4999, ARRAY['Up to 50 Patients', '20 Caregivers', 'Agency Dashboard', 'Incident Management', 'Audit Trail', 'CSV/PDF Export']),
    ('enterprise', 'Healthcare Enterprise', 19999, ARRAY['Unlimited Patients', 'Unlimited Caregivers', 'EHR/FHIR Integration', 'Dedicated Account Manager', 'Custom RLS SLA'])
ON CONFLICT (plan_tier) DO NOTHING;
