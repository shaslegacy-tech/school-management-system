# REST API Contract

Base path: `/api/v1`. JSON uses ISO-8601 timestamps in UTC. UUIDs identify resources. List endpoints should use cursor/page pagination with stable ordering and return `data` plus `page` metadata. All protected resources enforce role and branch/student relationship checks on the server.

OpenAPI/Swagger is served at `/swagger-ui.html` and `/v3/api-docs` by the backend. Authentication, branch listing, and student search/admission/profile endpoints are implemented. The remaining routes are the planned public contract and must be added module by module.

## Authentication

### `POST /auth/login`

Request:

```json
{ "email": "admin@littlehome.example", "password": "replace-this-password" }
```

Response `200`:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "opaque-random-value",
  "tokenType": "Bearer",
  "accessTokenExpiresAt": "2026-10-07T10:00:00Z",
  "userId": "f0f6b6f9-6e7d-4b2b-ae89-d2bbd1f51c2f",
  "displayName": "Maya Chen",
  "role": "SUPER_ADMIN",
  "branchId": null
}
```

`400` validation error; `401` invalid credentials. Login is `POST /auth/login`. Refresh is `POST /auth/refresh` with `{ "refreshToken": "..." }`, and returns a newly rotated pair. Logout is `POST /auth/logout` with the same body and returns `204`.

### Password recovery (planned)

- `POST /auth/password/forgot`: `{ "email": "parent@example.com" }`; always return `202` with the same generic message.
- `POST /auth/password/reset`: `{ "token": "one-time-token", "newPassword": "..." }`; enforce single use, expiry, breach policy, and session revocation.

## Module routes

| Method and route | Purpose | Roles |
|---|---|---|
| `GET/POST /branches` | List/create branches | Super Admin |
| `GET/PATCH /branches/{branchId}` | Read/update branch | Super Admin; Branch Admin own branch read |
| `GET/POST /students` | Search/admit students | Super Admin, Branch Admin |
| `GET/PATCH /students/{studentId}` | Student profile | Admins; Teacher assigned read; Parent linked read |
| `POST /students/{studentId}/transfers` | Transfer workflow | Super Admin, Branch Admin |
| `GET/PUT /attendance?date=&sectionId=` | Read/upsert daily register | Admins, assigned Teacher |
| `GET /students/{studentId}/attendance?from=&to=` | Child attendance history | Admins; linked Parent |
| `GET/POST /syllabus` | List/create syllabus units | Admins, Teacher assigned |
| `PATCH /syllabus/{unitId}/progress` | Update progress | Admins, Teacher assigned |
| `GET/POST /daily-activities` | Read/create daily report | Admins, Teacher assigned; Parent read linked |
| `GET/POST /homework` | Read/create homework | Admins, Teacher assigned; Parent read linked |
| `GET/POST /announcements` | Read/publish announcements | All read; Admin publish; Teacher policy-scoped create |
| `GET /notifications` | Current user's notifications | Authenticated user, own records only |
| `PATCH /notifications/{id}/read` | Mark own notification read | Authenticated user, own record only |
| `GET/POST /messages/threads` | Parent-teacher threads | Parent linked, Teacher assigned, Admin oversight |
| `GET/POST /reports/attendance` | Attendance reports | Admins; Teacher assigned scope |
| `POST /documents/upload-url` | Issue constrained signed upload | Role and owner scope checked |
| `GET /audit-logs` | Search audit events | Super Admin; Branch Admin own branch |

Currently implemented branch endpoint: `GET /branches` returns all branches for Super Admin and only the assigned branch for Branch Admin. Branch creation and branch updates remain planned.

Attendance write body:

```json
{
  "sectionId": "c5e26715-b5ab-4b16-a363-1c67cb1e5eb9",
  "date": "2026-10-07",
  "records": [
    { "studentId": "f0f6b6f9-6e7d-4b2b-ae89-d2bbd1f51c2f", "status": "PRESENT" },
    { "studentId": "9f56f49a-8df6-4564-bbba-12ef9d00dc9b", "status": "LEAVE", "remarks": "Medical leave" }
  ]
}
```

### Student search and admission (implemented)

`GET /students?branchId=<uuid>&search=aarav&page=0&size=20` returns paginated student response fields. `branchId` is optional for a Branch Admin (the assigned branch is always used) and optional for a Super Admin (omitted means all branches). Page size is limited to 100.

`POST /students` requires `SUPER_ADMIN` or `BRANCH_ADMIN`. Branch Admin scope comes from the authenticated user's account; a conflicting request branch is rejected. A supplied section must belong to that branch, and admission numbers are unique inside a branch.

```json
{
  "branchId": "f0f6b6f9-6e7d-4b2b-ae89-d2bbd1f51c2f",
  "sectionId": "c5e26715-b5ab-4b16-a363-1c67cb1e5eb9",
  "admissionNumber": "LHS-26124",
  "firstName": "Aarav",
  "lastName": "Sharma",
  "dateOfBirth": "2022-04-18",
  "gender": "UNSPECIFIED"
}
```

Admission currently creates the student row only. Guardian/contact linking and documents belong to the next student-module increment.

`GET /students/{studentId}` and `PATCH /students/{studentId}` are implemented for basic profile fields. A Branch Admin may access only records assigned to that branch; a Super Admin may access any branch. Guardian, medical, document, and transfer workflows remain pending.

Validation rules: email syntax and normalized uniqueness; password policy at least 12 characters and breached-password screening; student DOB must be in a configured age range; admission number unique per branch; attendance date must be within an open academic calendar; exactly one primary guardian per student; branch/section/teacher/student IDs must resolve within the caller's authorized scope; file type and size are allowlisted; date ranges are bounded; paging sizes are capped.

## Error envelope

```json
{
  "type": "https://api.littlehome.example/problems/validation",
  "title": "Request validation failed",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/students",
  "requestId": "req_01J9...",
  "fieldErrors": [{ "field": "admissionNumber", "message": "already exists in this branch" }]
}
```

Use RFC 9457 problem details. Never expose SQL, stack traces, token material, or whether an account exists. Map validation to `400`, authentication to `401`, authorization to `403`, missing resources to `404`, version conflicts to `409`, and unexpected errors to `500`; log details with a request ID.