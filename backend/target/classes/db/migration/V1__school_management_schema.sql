CREATE TABLE branches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(32) NOT NULL UNIQUE,
    name varchar(160) NOT NULL,
    phone varchar(32),
    email varchar(254),
    address_line1 varchar(180),
    address_line2 varchar(180),
    city varchar(100),
    state varchar(100),
    postal_code varchar(20),
    timezone varchar(64) NOT NULL DEFAULT 'Asia/Kolkata',
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE roles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(32) NOT NULL UNIQUE CHECK (code IN ('SUPER_ADMIN', 'BRANCH_ADMIN', 'TEACHER', 'PARENT')),
    name varchar(80) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE academic_years (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    name varchar(32) NOT NULL,
    starts_on date NOT NULL,
    ends_on date NOT NULL,
    is_current boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_academic_year_dates CHECK (ends_on > starts_on),
    UNIQUE (branch_id, name)
);
CREATE UNIQUE INDEX uq_academic_year_current ON academic_years(branch_id) WHERE is_current;

CREATE TABLE school_classes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(32) NOT NULL UNIQUE,
    name varchar(80) NOT NULL UNIQUE,
    display_order smallint NOT NULL UNIQUE CHECK (display_order BETWEEN 1 AND 5),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid REFERENCES branches(id) ON DELETE RESTRICT,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    email varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    first_name varchar(80) NOT NULL,
    last_name varchar(80) NOT NULL,
    phone varchar(32),
    is_active boolean NOT NULL DEFAULT true,
    email_verified_at timestamptz,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_users_email_lower ON users(lower(email));
CREATE INDEX ix_users_branch_role ON users(branch_id, role_id) WHERE is_active;

CREATE FUNCTION enforce_user_branch_scope() RETURNS trigger AS $$
BEGIN
    IF NEW.branch_id IS NULL AND NOT EXISTS (
        SELECT 1 FROM roles WHERE id = NEW.role_id AND code = 'SUPER_ADMIN'
    ) THEN
        RAISE EXCEPTION 'Only super administrators may have no branch';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_user_branch_scope
    BEFORE INSERT OR UPDATE OF branch_id, role_id ON users
    FOR EACH ROW EXECUTE FUNCTION enforce_user_branch_scope();

CREATE TABLE user_branch_memberships (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, branch_id)
);
CREATE UNIQUE INDEX uq_user_default_branch ON user_branch_memberships(user_id) WHERE is_default;
CREATE INDEX ix_memberships_branch ON user_branch_memberships(branch_id, user_id);

CREATE TABLE parents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    first_name varchar(80) NOT NULL,
    last_name varchar(80) NOT NULL,
    email varchar(254),
    phone varchar(32) NOT NULL,
    alternate_phone varchar(32),
    address jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_parents_phone ON parents(phone);

CREATE TABLE teachers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    employee_number varchar(40) NOT NULL UNIQUE,
    qualification varchar(160),
    hired_on date,
    emergency_contact jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0
);

CREATE TABLE class_sections (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    academic_year_id uuid NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    class_id uuid NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    section_name varchar(16) NOT NULL,
    homeroom_teacher_id uuid REFERENCES teachers(id) ON DELETE SET NULL,
    capacity smallint NOT NULL DEFAULT 30 CHECK (capacity > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0,
    UNIQUE (branch_id, academic_year_id, class_id, section_name)
);
CREATE INDEX ix_sections_branch_year ON class_sections(branch_id, academic_year_id);

CREATE TABLE teacher_section_assignments (
    teacher_id uuid NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    section_id uuid NOT NULL REFERENCES class_sections(id) ON DELETE CASCADE,
    subject varchar(80),
    starts_on date NOT NULL,
    ends_on date,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid REFERENCES users(id) ON DELETE SET NULL,
    PRIMARY KEY (teacher_id, section_id, starts_on),
    CONSTRAINT ck_teacher_assignment_dates CHECK (ends_on IS NULL OR ends_on >= starts_on)
);
CREATE INDEX ix_teacher_assignments_section ON teacher_section_assignments(section_id, teacher_id);

CREATE TABLE students (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    section_id uuid REFERENCES class_sections(id) ON DELETE SET NULL,
    admission_number varchar(40) NOT NULL,
    first_name varchar(80) NOT NULL,
    last_name varchar(80) NOT NULL,
    preferred_name varchar(80),
    date_of_birth date NOT NULL,
    gender varchar(24),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'TRANSFERRED', 'GRADUATED')),
    admitted_on date NOT NULL DEFAULT current_date,
    medical_information jsonb NOT NULL DEFAULT '{}'::jsonb,
    emergency_contacts jsonb NOT NULL DEFAULT '[]'::jsonb,
    photo_document_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0,
    UNIQUE (branch_id, admission_number)
);
CREATE INDEX ix_students_branch_status ON students(branch_id, status);
CREATE INDEX ix_students_section ON students(section_id) WHERE status = 'ACTIVE';
CREATE INDEX ix_students_name ON students(branch_id, last_name, first_name);

CREATE TABLE student_parents (
    student_id uuid NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    parent_id uuid NOT NULL REFERENCES parents(id) ON DELETE RESTRICT,
    relationship varchar(32) NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    can_pick_up boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (student_id, parent_id)
);
CREATE UNIQUE INDEX uq_student_primary_parent ON student_parents(student_id) WHERE is_primary;
CREATE INDEX ix_student_parents_parent ON student_parents(parent_id);

CREATE TABLE student_transfers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id uuid NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    from_branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    to_branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    requested_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    approved_by uuid REFERENCES users(id) ON DELETE RESTRICT,
    status varchar(16) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')),
    effective_on date NOT NULL,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_transfer_branch CHECK (from_branch_id <> to_branch_id)
);

CREATE TABLE attendance (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id uuid NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    section_id uuid REFERENCES class_sections(id) ON DELETE SET NULL,
    attendance_date date NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LEAVE')),
    marked_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    marked_at timestamptz NOT NULL DEFAULT now(),
    remarks varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0,
    UNIQUE (student_id, attendance_date)
);
CREATE INDEX ix_attendance_branch_date ON attendance(branch_id, attendance_date, status);
CREATE INDEX ix_attendance_student_date ON attendance(student_id, attendance_date DESC);

CREATE TABLE syllabus_units (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    academic_year_id uuid NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    class_id uuid NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    subject varchar(80) NOT NULL,
    unit_title varchar(180) NOT NULL,
    month smallint CHECK (month BETWEEN 1 AND 12),
    sequence_number smallint NOT NULL DEFAULT 0,
    starts_on date,
    ends_on date,
    progress_percent smallint NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
    status varchar(16) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED')),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_syllabus_dates CHECK (ends_on IS NULL OR starts_on IS NULL OR ends_on >= starts_on)
);
CREATE INDEX ix_syllabus_scope ON syllabus_units(branch_id, academic_year_id, class_id, subject);

CREATE TABLE daily_activities (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    section_id uuid NOT NULL REFERENCES class_sections(id) ON DELETE RESTRICT,
    teacher_id uuid NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    activity_date date NOT NULL,
    title varchar(180) NOT NULL,
    learning_topics text,
    rhymes text,
    story_time text,
    games text,
    arts_and_crafts text,
    important_notes text,
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0,
    UNIQUE (section_id, activity_date, title)
);
CREATE INDEX ix_activity_section_date ON daily_activities(section_id, activity_date DESC);

CREATE TABLE homework (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id uuid NOT NULL REFERENCES class_sections(id) ON DELETE RESTRICT,
    teacher_id uuid NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    title varchar(180) NOT NULL,
    instructions text NOT NULL,
    assigned_on date NOT NULL DEFAULT current_date,
    due_on date,
    attachment_document_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_homework_due CHECK (due_on IS NULL OR due_on >= assigned_on)
);
CREATE INDEX ix_homework_section_due ON homework(section_id, due_on);

CREATE TABLE announcements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid REFERENCES branches(id) ON DELETE RESTRICT,
    author_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title varchar(180) NOT NULL,
    body text NOT NULL,
    audience varchar(20) NOT NULL DEFAULT 'ALL' CHECK (audience IN ('ALL', 'STAFF', 'PARENTS', 'TEACHERS')),
    published_at timestamptz,
    expires_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_announcements_audience_date ON announcements(audience, published_at DESC) WHERE published_at IS NOT NULL;
CREATE INDEX ix_announcements_branch_date ON announcements(branch_id, published_at DESC);

CREATE TABLE notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title varchar(180) NOT NULL,
    body varchar(1000) NOT NULL,
    channel varchar(16) NOT NULL DEFAULT 'IN_APP' CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS', 'PUSH')),
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    read_at timestamptz,
    delivered_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_recipient_unread ON notifications(recipient_id, created_at DESC) WHERE read_at IS NULL;

CREATE TABLE documents (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    owner_type varchar(32) NOT NULL,
    owner_id uuid NOT NULL,
    storage_key varchar(512) NOT NULL UNIQUE,
    original_name varchar(255) NOT NULL,
    media_type varchar(120) NOT NULL,
    byte_size bigint NOT NULL CHECK (byte_size >= 0),
    checksum_sha256 char(64),
    uploaded_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_documents_owner ON documents(owner_type, owner_id);
ALTER TABLE students ADD CONSTRAINT fk_students_photo_document FOREIGN KEY (photo_document_id) REFERENCES documents(id) ON DELETE SET NULL;
ALTER TABLE homework ADD CONSTRAINT fk_homework_attachment FOREIGN KEY (attachment_document_id) REFERENCES documents(id) ON DELETE SET NULL;

CREATE TABLE communication_threads (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id uuid NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    subject varchar(180) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_by uuid,
    version bigint NOT NULL DEFAULT 0
);
CREATE TABLE communication_messages (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id uuid NOT NULL REFERENCES communication_threads(id) ON DELETE CASCADE,
    sender_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    body text NOT NULL,
    sent_at timestamptz NOT NULL DEFAULT now(),
    read_at timestamptz
);
CREATE INDEX ix_messages_thread_sent ON communication_messages(thread_id, sent_at);

CREATE TABLE school_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid REFERENCES branches(id) ON DELETE RESTRICT,
    title varchar(180) NOT NULL,
    description text,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz,
    audience varchar(20) NOT NULL DEFAULT 'ALL' CHECK (audience IN ('ALL', 'STAFF', 'PARENTS')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_event_dates CHECK (ends_at IS NULL OR ends_at > starts_at)
);
CREATE INDEX ix_events_branch_start ON school_events(branch_id, starts_at);

CREATE TABLE gallery_albums (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    title varchar(180) NOT NULL,
    event_id uuid REFERENCES school_events(id) ON DELETE SET NULL,
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    version bigint NOT NULL DEFAULT 0
);
CREATE TABLE gallery_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    album_id uuid NOT NULL REFERENCES gallery_albums(id) ON DELETE CASCADE,
    document_id uuid NOT NULL REFERENCES documents(id) ON DELETE RESTRICT,
    caption varchar(500),
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (album_id, document_id)
);

CREATE TABLE fee_structures (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    academic_year_id uuid NOT NULL REFERENCES academic_years(id) ON DELETE RESTRICT,
    class_id uuid NOT NULL REFERENCES school_classes(id) ON DELETE RESTRICT,
    fee_name varchar(100) NOT NULL,
    amount numeric(12,2) NOT NULL CHECK (amount >= 0),
    currency char(3) NOT NULL DEFAULT 'INR',
    due_day smallint CHECK (due_day BETWEEN 1 AND 28),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    version bigint NOT NULL DEFAULT 0,
    UNIQUE (branch_id, academic_year_id, class_id, fee_name)
);

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    replaced_by_id uuid,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user_active ON refresh_tokens(user_id, expires_at) WHERE revoked_at IS NULL;

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid REFERENCES branches(id) ON DELETE SET NULL,
    actor_id uuid REFERENCES users(id) ON DELETE SET NULL,
    action varchar(80) NOT NULL,
    entity_type varchar(80) NOT NULL,
    entity_id uuid,
    request_id varchar(80),
    before_state jsonb,
    after_state jsonb,
    ip_address inet,
    occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_branch_time ON audit_logs(branch_id, occurred_at DESC);
CREATE INDEX ix_audit_entity ON audit_logs(entity_type, entity_id, occurred_at DESC);

INSERT INTO roles (code, name) VALUES
    ('SUPER_ADMIN', 'Super Admin'), ('BRANCH_ADMIN', 'Branch Admin'), ('TEACHER', 'Teacher'), ('PARENT', 'Parent');
INSERT INTO school_classes (code, name, display_order) VALUES
    ('DAY_CARE', 'Day Care', 1), ('PLAY_GROUP', 'Play Group', 2), ('NURSERY', 'Nursery', 3),
    ('JUNIOR_KG', 'Junior KG', 4), ('SENIOR_KG', 'Senior KG', 5);
INSERT INTO branches (code, name) VALUES
    ('LHS-CENTRAL', 'Little Home · Central'), ('LHS-NORTH', 'Little Home · North'),
    ('LHS-RIVERSIDE', 'Little Home · Riverside'), ('LHS-GARDEN', 'Little Home · Garden'),
    ('LHS-HILLVIEW', 'Little Home · Hillview'), ('LHS-LAKESIDE', 'Little Home · Lakeside'),
    ('LHS-WEST-END', 'Little Home · West End');