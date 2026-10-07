"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarCheck, Plus, RefreshCw } from "lucide-react";
import { useState, type FormEvent } from "react";
import { apiClient } from "@/lib/api-client";
import type { SessionUser } from "@/stores/session-store";

type Branch = { id: string; code: string; name: string; phone: string | null; email: string | null; city: string | null; status: string };
type SchoolClass = { id: string; code: string; name: string; displayOrder: number };
type AcademicYear = { id: string; branchId: string; name: string; startsOn: string; endsOn: string; current: boolean };
type Section = { id: string; branchId: string; academicYearId: string; classId: string; sectionName: string; capacity: number; homeroomTeacherId: string | null };
type StaffUser = { id: string; email: string; firstName: string; lastName: string; role: string; branchId: string | null; branchName: string | null; active: boolean; createdAt: string };
type Student = { id: string; branchId: string; sectionId: string | null; admissionNumber: string; firstName: string; lastName: string; dateOfBirth: string; status: string };
type PageResult<T> = { content: T[]; totalElements: number };
type FeeStructure = { id: string; branchId: string; academicYearId: string; classId: string; feeName: string; amount: number; currency: string; dueDay: number | null };
type Announcement = { id: string; branchId: string | null; title: string; body: string; audience: string; publishedAt: string };
type AttendanceRecord = { id: string; studentId: string; sectionId: string; date: string; status: "PRESENT" | "ABSENT" | "LEAVE"; markedAt: string; remarks: string | null };
type AttendanceStatus = AttendanceRecord["status"];
const inputClass = "admin-input";

export default function AdminWorkspace({ page, user, branchId, onToast }: { page: string; user: SessionUser; branchId: string; onToast: (message: string) => void }) {
  if (page === "Branches") return <BranchesWorkspace onToast={onToast} />;
  if (page === "Users") return <UsersWorkspace onToast={onToast} />;
  if (page === "Classes" || page === "Classes & staff") return <ClassesWorkspace user={user} branchId={branchId} onToast={onToast} />;
  if (page === "Academic years") return <AcademicYearsWorkspace user={user} branchId={branchId} onToast={onToast} />;
  if (page === "Attendance") return <AttendanceWorkspace branchId={branchId} onToast={onToast} />;
  if (page === "Fees") return <FeesWorkspace onToast={onToast} />;
  if (page === "Announcements") return <AnnouncementsWorkspace user={user} branchId={branchId} onToast={onToast} />;
  return null;
}

function BranchesWorkspace({ onToast }: { onToast: (message: string) => void }) {
  const client = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const branches = useQuery({ queryKey: ["branches", "admin"], queryFn: async () => (await apiClient.get<Branch[]>("/branches")).data });
  const create = useMutation({
    mutationFn: async (body: object) => apiClient.post("/branches", body),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ["branches"] }); setFormOpen(false); onToast("Branch created"); },
    onError: () => onToast("Branch could not be created. Check the code and required fields."),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    create.mutate({ code: data.get("code"), name: data.get("name"), city: data.get("city"), phone: data.get("phone"), email: data.get("email") });
  }
  return <section className="panel"><PanelHeader title="Branch directory" note={`${branches.data?.length ?? 0} school locations`} action={<button className="primary-action" onClick={() => setFormOpen((open) => !open)} type="button"><Plus size={14} /> Add branch</button>} />
    {formOpen && <form className="admin-form-grid" onSubmit={submit}><label className="form-field">Branch code<input className={inputClass} name="code" pattern="[A-Za-z0-9-]+" placeholder="LHS-EAST" required /></label><label className="form-field">Branch name<input className={inputClass} name="name" placeholder="Little Home · East" required /></label><label className="form-field">City<input className={inputClass} name="city" /></label><label className="form-field">Phone<input className={inputClass} name="phone" /></label><label className="form-field">Email<input className={inputClass} name="email" type="email" /></label><button className="primary-action admin-form-submit" disabled={create.isPending} type="submit">{create.isPending ? "Creating..." : "Create branch"}</button></form>}
    <div className="table-wrap"><table className="student-table"><thead><tr><th>BRANCH</th><th>CODE</th><th>CITY</th><th>CONTACT</th><th>STATUS</th></tr></thead><tbody>{branches.data?.map((branch) => <tr key={branch.id}><td className="student-name">{branch.name}</td><td>{branch.code}</td><td>{branch.city ?? "—"}</td><td>{branch.email ?? branch.phone ?? "—"}</td><td><span className="status-pill">{branch.status}</span></td></tr>)}<TableState loading={branches.isLoading} error={branches.isError} empty={!branches.isLoading && !branches.isError && !branches.data?.length} colSpan={5} noun="branches" /></tbody></table></div>
  </section>;
}

function UsersWorkspace({ onToast }: { onToast: (message: string) => void }) {
  const client = useQueryClient();
  const branches = useQuery({ queryKey: ["branches", "user-admin"], queryFn: async () => (await apiClient.get<Branch[]>("/branches")).data });
  const users = useQuery({ queryKey: ["admin-users"], queryFn: async () => (await apiClient.get<PageResult<StaffUser>>("/admin/users", { params: { page: 0, size: 100 } })).data });
  const [formOpen, setFormOpen] = useState(false);
  const create = useMutation({
    mutationFn: async (body: object) => apiClient.post("/admin/users", body),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ["admin-users"] }); setFormOpen(false); onToast("Account created with a temporary password"); },
    onError: () => onToast("Account could not be created. Check for duplicate email and password length."),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    create.mutate({ email: data.get("email"), firstName: data.get("firstName"), lastName: data.get("lastName"), role: data.get("role"), branchId: data.get("branchId"), temporaryPassword: data.get("temporaryPassword") });
  }
  return <section className="panel"><PanelHeader title="User access" note={`${users.data?.totalElements ?? 0} accounts · rotate temporary passwords`} action={<button className="primary-action" onClick={() => setFormOpen((open) => !open)} type="button"><Plus size={14} /> Add user</button>} />
    {formOpen && <form className="admin-form-grid" onSubmit={submit}><label className="form-field">First name<input className={inputClass} name="firstName" required /></label><label className="form-field">Last name<input className={inputClass} name="lastName" required /></label><label className="form-field">Email<input className={inputClass} name="email" type="email" required /></label><label className="form-field">Role<select className={inputClass} name="role" defaultValue="BRANCH_ADMIN"><option value="BRANCH_ADMIN">Branch Admin</option><option value="TEACHER">Teacher</option><option value="PARENT">Parent</option></select></label><label className="form-field">Branch<select className={inputClass} name="branchId" required>{branches.data?.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select></label><label className="form-field">Temporary password<input className={inputClass} minLength={16} name="temporaryPassword" type="password" required /></label><button className="primary-action admin-form-submit" disabled={create.isPending} type="submit">{create.isPending ? "Creating..." : "Create account"}</button></form>}
    <div className="table-wrap"><table className="student-table"><thead><tr><th>NAME</th><th>EMAIL</th><th>ROLE</th><th>BRANCH</th><th>STATUS</th></tr></thead><tbody>{users.data?.content.map((account) => <tr key={account.id}><td className="student-name">{account.firstName} {account.lastName}</td><td>{account.email}</td><td>{account.role.replaceAll("_", " ")}</td><td>{account.branchName ?? "All branches"}</td><td><span className={`status-pill ${account.active ? "" : "pending"}`}>{account.active ? "Active" : "Inactive"}</span></td></tr>)}<TableState loading={users.isLoading} error={users.isError} empty={!users.isLoading && !users.isError && !users.data?.content.length} colSpan={5} noun="accounts" /></tbody></table></div>
  </section>;
}

function AcademicYearsWorkspace({ user, branchId, onToast }: { user: SessionUser; branchId: string; onToast: (message: string) => void }) {
  const client = useQueryClient();
  const branches = useQuery({ queryKey: ["branches", "academic-years"], queryFn: async () => (await apiClient.get<Branch[]>("/branches")).data });
  const years = useQuery({ queryKey: ["academic-years", branchId], queryFn: async () => (await apiClient.get<AcademicYear[]>("/academic-years", { params: { branchId: branchId || undefined } })).data });
  const [formOpen, setFormOpen] = useState(false);
  const create = useMutation({
    mutationFn: async (body: object) => apiClient.post("/academic-years", body),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ["academic-years"] }); setFormOpen(false); onToast("Academic year created"); },
    onError: () => onToast("Academic year could not be created. Check branch and date range."),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    create.mutate({ branchId: user.role === "SUPER_ADMIN" ? data.get("branchId") : user.branchId, name: data.get("name"), startsOn: data.get("startsOn"), endsOn: data.get("endsOn"), current: data.get("current") === "on" });
  }
  return <section className="panel"><PanelHeader title="Academic calendar" note="Year names and date ranges by branch" action={<button className="primary-action" onClick={() => setFormOpen((open) => !open)} type="button"><Plus size={14} /> Add academic year</button>} />
    {formOpen && <form className="admin-form-grid" onSubmit={submit}>{user.role === "SUPER_ADMIN" && <label className="form-field">Branch<select className={inputClass} name="branchId" defaultValue={branchId} required><option value="">Select branch</option>{branches.data?.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select></label>}<label className="form-field">Year name<input className={inputClass} name="name" placeholder="2027–28" required /></label><label className="form-field">Starts on<input className={inputClass} name="startsOn" type="date" required /></label><label className="form-field">Ends on<input className={inputClass} name="endsOn" type="date" required /></label><label className="form-field admin-checkbox"><input name="current" type="checkbox" /> Current academic year</label><button className="primary-action admin-form-submit" disabled={create.isPending} type="submit">Create year</button></form>}
    <div className="table-wrap"><table className="student-table"><thead><tr><th>ACADEMIC YEAR</th><th>START</th><th>END</th><th>STATUS</th></tr></thead><tbody>{years.data?.map((year) => <tr key={year.id}><td className="student-name">{year.name}</td><td>{year.startsOn}</td><td>{year.endsOn}</td><td><span className={`status-pill ${year.current ? "" : "leave"}`}>{year.current ? "Current" : "Planned"}</span></td></tr>)}<TableState loading={years.isLoading} error={years.isError} empty={!years.isLoading && !years.isError && !years.data?.length} colSpan={4} noun="academic years" /></tbody></table></div>
  </section>;
}

function ClassesWorkspace({ user, branchId, onToast }: { user: SessionUser; branchId: string; onToast: (message: string) => void }) {
  const client = useQueryClient();
  const branches = useQuery({ queryKey: ["branches", "sections"], queryFn: async () => (await apiClient.get<Branch[]>("/branches")).data });
  const classes = useQuery({ queryKey: ["classes"], queryFn: async () => (await apiClient.get<SchoolClass[]>("/classes")).data });
  const years = useQuery({ queryKey: ["academic-years", branchId], queryFn: async () => (await apiClient.get<AcademicYear[]>("/academic-years", { params: { branchId: branchId || undefined } })).data });
  const sections = useQuery({ queryKey: ["sections", branchId], queryFn: async () => (await apiClient.get<Section[]>("/sections", { params: { branchId: branchId || undefined } })).data });
  const [formOpen, setFormOpen] = useState(false);
  const create = useMutation({
    mutationFn: async (body: object) => apiClient.post("/sections", body),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ["sections"] }); setFormOpen(false); onToast("Class section created"); },
    onError: () => onToast("Section could not be created. Choose a valid branch, year, and class."),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    create.mutate({ branchId: user.role === "SUPER_ADMIN" ? data.get("branchId") : user.branchId, academicYearId: data.get("academicYearId"), classId: data.get("classId"), sectionName: data.get("sectionName"), capacity: Number(data.get("capacity")) });
  }
  const classNames = new Map(classes.data?.map((schoolClass) => [schoolClass.id, schoolClass.name]) ?? []);
  return <div className="admin-two-column"><section className="panel"><PanelHeader title="Early years classes" note="School-wide class catalogue" /><div className="class-list">{classes.data?.map((schoolClass) => <div className="class-row" key={schoolClass.id}><div><div className="class-title">{schoolClass.name}</div><div className="class-subtitle">{schoolClass.code}</div></div><div className="class-count">{schoolClass.displayOrder}</div><span className="status-pill">Active</span></div>)}{classes.isLoading && <InfoRow message="Loading classes..." />}{classes.isError && <InfoRow message="Class catalogue could not be loaded." />}</div></section>
    <section className="panel"><PanelHeader title="Class sections" note={`${sections.data?.length ?? 0} sections configured`} action={<button className="primary-action" onClick={() => setFormOpen((open) => !open)} type="button"><Plus size={14} /> Add section</button>} />{formOpen && <form className="admin-form-grid" onSubmit={submit}>{user.role === "SUPER_ADMIN" && <label className="form-field">Branch<select className={inputClass} name="branchId" defaultValue={branchId} required>{branches.data?.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select></label>}<label className="form-field">Academic year<select className={inputClass} name="academicYearId" required>{years.data?.map((year) => <option key={year.id} value={year.id}>{year.name}</option>)}</select></label><label className="form-field">Class<select className={inputClass} name="classId" required>{classes.data?.map((schoolClass) => <option key={schoolClass.id} value={schoolClass.id}>{schoolClass.name}</option>)}</select></label><label className="form-field">Section<input className={inputClass} name="sectionName" placeholder="A" required /></label><label className="form-field">Capacity<input className={inputClass} defaultValue="30" max="100" min="1" name="capacity" type="number" required /></label><button className="primary-action admin-form-submit" disabled={create.isPending} type="submit">Create section</button></form>}<div className="table-wrap"><table className="student-table"><thead><tr><th>CLASS</th><th>SECTION</th><th>CAPACITY</th></tr></thead><tbody>{sections.data?.map((section) => <tr key={section.id}><td>{classNames.get(section.classId) ?? "Class"}</td><td className="student-name">{section.sectionName}</td><td>{section.capacity}</td></tr>)}<TableState loading={sections.isLoading} error={sections.isError} empty={!sections.isLoading && !sections.isError && !sections.data?.length} colSpan={3} noun="sections" /></tbody></table></div></section></div>;
}

function FeesWorkspace({ onToast }: { onToast: (message: string) => void }) {
  const client = useQueryClient();
  const branches = useQuery({ queryKey: ["branches", "fees"], queryFn: async () => (await apiClient.get<Branch[]>("/branches")).data });
  const classes = useQuery({ queryKey: ["classes"], queryFn: async () => (await apiClient.get<SchoolClass[]>("/classes")).data });
  const years = useQuery({ queryKey: ["academic-years", "all"], queryFn: async () => (await apiClient.get<AcademicYear[]>("/academic-years")).data });
  const fees = useQuery({ queryKey: ["fee-structures"], queryFn: async () => (await apiClient.get<FeeStructure[]>("/fees/structures")).data });
  const [formOpen, setFormOpen] = useState(false);
  const create = useMutation({
    mutationFn: async (body: object) => apiClient.post("/fees/structures", body),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ["fee-structures"] }); setFormOpen(false); onToast("Fee structure created"); },
    onError: () => onToast("Fee structure could not be created. Check the academic year and class."),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    create.mutate({ branchId: data.get("branchId"), academicYearId: data.get("academicYearId"), classId: data.get("classId"), feeName: data.get("feeName"), amount: Number(data.get("amount")), currency: data.get("currency"), dueDay: Number(data.get("dueDay")) || null });
  }
  const branchNames = new Map(branches.data?.map((item) => [item.id, item.name]) ?? []);
  const classNames = new Map(classes.data?.map((item) => [item.id, item.name]) ?? []);
  const yearNames = new Map(years.data?.map((item) => [item.id, item.name]) ?? []);
  return <section className="panel"><PanelHeader title="Fee structures" note="Tuition and fee schedules by branch, year, and class" action={<button className="primary-action" onClick={() => setFormOpen((open) => !open)} type="button"><Plus size={14} /> Add fee</button>} />
    {formOpen && <form className="admin-form-grid" onSubmit={submit}><label className="form-field">Branch<select className={inputClass} name="branchId" required>{branches.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label><label className="form-field">Academic year<select className={inputClass} name="academicYearId" required>{years.data?.map((item) => <option key={item.id} value={item.id}>{item.name} · {branchNames.get(item.branchId)}</option>)}</select></label><label className="form-field">Class<select className={inputClass} name="classId" required>{classes.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label><label className="form-field">Fee name<input className={inputClass} name="feeName" placeholder="Tuition fee" required /></label><label className="form-field">Amount<input className={inputClass} min="0" name="amount" step="0.01" type="number" required /></label><label className="form-field">Currency<input className={inputClass} defaultValue="INR" maxLength={3} name="currency" required /></label><label className="form-field">Due day<input className={inputClass} max="28" min="1" name="dueDay" type="number" /></label><button className="primary-action admin-form-submit" disabled={create.isPending} type="submit">Save fee structure</button></form>}
    <div className="table-wrap"><table className="student-table"><thead><tr><th>FEE</th><th>BRANCH</th><th>ACADEMIC YEAR</th><th>CLASS</th><th>AMOUNT</th><th>DUE DAY</th></tr></thead><tbody>{fees.data?.map((fee) => <tr key={fee.id}><td className="student-name">{fee.feeName}</td><td>{branchNames.get(fee.branchId) ?? "—"}</td><td>{yearNames.get(fee.academicYearId) ?? "—"}</td><td>{classNames.get(fee.classId) ?? "—"}</td><td>{fee.currency} {fee.amount.toFixed(2)}</td><td>{fee.dueDay ?? "—"}</td></tr>)}<TableState loading={fees.isLoading} error={fees.isError} empty={!fees.isLoading && !fees.isError && !fees.data?.length} colSpan={6} noun="fee structures" /></tbody></table></div>
  </section>;
}

function AnnouncementsWorkspace({ user, branchId, onToast }: { user: SessionUser; branchId: string; onToast: (message: string) => void }) {
  const client = useQueryClient();
  const branches = useQuery({ queryKey: ["branches", "announcements"], queryFn: async () => (await apiClient.get<Branch[]>("/branches")).data });
  const announcements = useQuery({ queryKey: ["announcements", branchId], queryFn: async () => (await apiClient.get<Announcement[]>("/announcements", { params: { branchId: branchId || undefined } })).data });
  const [formOpen, setFormOpen] = useState(false);
  const create = useMutation({
    mutationFn: async (body: object) => apiClient.post("/announcements", body),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ["announcements"] }); setFormOpen(false); onToast("Announcement published"); },
    onError: () => onToast("Announcement could not be published."),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    create.mutate({ branchId: user.role === "SUPER_ADMIN" ? (data.get("branchId") || null) : user.branchId, title: data.get("title"), body: data.get("body"), audience: data.get("audience") });
  }
  const branchNames = new Map(branches.data?.map((item) => [item.id, item.name]) ?? []);
  return <section className="panel"><PanelHeader title="Announcements" note="Published notices for staff and families" action={<button className="primary-action" onClick={() => setFormOpen((open) => !open)} type="button"><Plus size={14} /> New announcement</button>} />
    {formOpen && <form className="admin-form-grid" onSubmit={submit}>{user.role === "SUPER_ADMIN" && <label className="form-field">Audience scope<select className={inputClass} name="branchId" defaultValue={branchId}><option value="">All branches</option>{branches.data?.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>}<label className="form-field">Audience<select className={inputClass} name="audience" defaultValue="ALL"><option value="ALL">Everyone</option><option value="STAFF">Staff</option><option value="TEACHERS">Teachers</option><option value="PARENTS">Parents</option></select></label><label className="form-field">Title<input className={inputClass} name="title" maxLength={180} required /></label><label className="form-field admin-wide-field">Message<textarea className={inputClass} name="body" maxLength={10000} required rows={4} /></label><button className="primary-action admin-form-submit" disabled={create.isPending} type="submit">{create.isPending ? "Publishing..." : "Publish announcement"}</button></form>}
    <div className="announcement-list">{announcements.data?.map((item) => <article className="announcement-row" key={item.id}><div className="announcement-top"><h3>{item.title}</h3><span className="status-pill">{item.audience}</span></div><p>{item.body}</p><div className="panel-note">{item.branchId ? branchNames.get(item.branchId) : "All branches"} · {new Date(item.publishedAt).toLocaleString()}</div></article>)}{announcements.isLoading && <InfoRow message="Loading announcements..." />}{announcements.isError && <InfoRow message="Announcements could not be loaded." />}{!announcements.isLoading && !announcements.isError && !announcements.data?.length && <InfoRow message="No announcements have been published." />}</div>
  </section>;
}

function AttendanceWorkspace({ branchId, onToast }: { branchId: string; onToast: (message: string) => void }) {
  const client = useQueryClient();
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [sectionId, setSectionId] = useState("");
  const [overrides, setOverrides] = useState<Record<string, AttendanceStatus>>({});
  const sections = useQuery({ queryKey: ["sections", branchId], queryFn: async () => (await apiClient.get<Section[]>("/sections", { params: { branchId: branchId || undefined } })).data });
  const section = sections.data?.find((candidate) => candidate.id === sectionId);
  const students = useQuery({ queryKey: ["students", "attendance", sectionId], enabled: Boolean(section), queryFn: async () => (await apiClient.get<PageResult<Student>>("/students", { params: { branchId: section?.branchId, page: 0, size: 100 } })).data });
  const roster = (students.data?.content ?? []).filter((student) => student.sectionId === sectionId && student.status === "ACTIVE");
  const attendance = useQuery({ queryKey: ["attendance", date, sectionId], enabled: Boolean(sectionId), queryFn: async () => (await apiClient.get<AttendanceRecord[]>("/attendance", { params: { date, sectionId, branchId: section?.branchId } })).data });
  const saved = new Map(attendance.data?.map((record) => [record.studentId, record.status]) ?? []);
  const statusFor = (studentId: string) => overrides[studentId] ?? saved.get(studentId) ?? "PRESENT";
  const save = useMutation({
    mutationFn: async () => apiClient.post("/attendance", { sectionId, date, records: roster.map((student) => ({ studentId: student.id, status: statusFor(student.id) })) }, { params: { branchId: section?.branchId } }),
    onSuccess: async () => { setOverrides({}); await client.invalidateQueries({ queryKey: ["attendance", date, sectionId] }); onToast("Attendance register saved"); },
    onError: () => onToast("Attendance could not be saved. Verify the section roster."),
  });
  return <section className="panel"><PanelHeader title="Daily attendance" note="Mark Present, Absent, or Leave for one section." action={<button className="primary-action" disabled={!sectionId || !roster.length || save.isPending} onClick={() => save.mutate()} type="button"><CalendarCheck size={14} /> {save.isPending ? "Saving..." : "Save register"}</button>} /><div className="attendance-toolbar"><label className="form-field">Date<input className={inputClass} onChange={(event) => { setDate(event.target.value); setOverrides({}); }} type="date" value={date} /></label><label className="form-field">Section<select className={inputClass} onChange={(event) => { setSectionId(event.target.value); setOverrides({}); }} value={sectionId}><option value="">Choose a section</option>{sections.data?.map((item) => <option key={item.id} value={item.id}>{item.sectionName} · {item.id.slice(0, 8)}</option>)}</select></label></div><div className="table-wrap"><table className="student-table"><thead><tr><th>STUDENT</th><th>ADMISSION NUMBER</th><th>ATTENDANCE</th></tr></thead><tbody>{roster.map((student) => <tr key={student.id}><td className="student-name">{student.firstName} {student.lastName}</td><td>{student.admissionNumber}</td><td><div className="attendance-choice">{(["PRESENT", "ABSENT", "LEAVE"] as AttendanceStatus[]).map((status) => <button aria-pressed={statusFor(student.id) === status} className={`range-button ${statusFor(student.id) === status ? "selected" : ""}`} key={status} onClick={() => setOverrides((current) => ({ ...current, [student.id]: status }))} type="button">{status}</button>)}</div></td></tr>)}{sections.isLoading && <TableState loading error={false} empty={false} colSpan={3} noun="sections" />}{students.isLoading && <TableState loading error={false} empty={false} colSpan={3} noun="students" />}{students.isError && <TableState loading={false} error empty={false} colSpan={3} noun="students" />}{!sectionId && <tr><td colSpan={3} className="empty-table-cell">Choose a section to load its register.</td></tr>}{sectionId && !students.isLoading && !students.isError && roster.length === 0 && <tr><td colSpan={3} className="empty-table-cell">No active students are assigned to this section.</td></tr>}</tbody></table></div></section>;
}

function PanelHeader({ title, note, action }: { title: string; note: string; action?: React.ReactNode }) {
  return <div className="panel-header"><div><h2 className="panel-title">{title}</h2><div className="panel-note">{note}</div></div>{action}</div>;
}

function TableState({ loading, error, empty, colSpan, noun }: { loading: boolean; error: boolean; empty: boolean; colSpan: number; noun: string }) {
  if (!loading && !error && !empty) return null;
  const message = loading ? `Loading ${noun}...` : error ? `${noun} could not be loaded. Retry the request.` : `No ${noun} found.`;
  return <tr><td colSpan={colSpan} className="empty-table-cell">{loading && <RefreshCw className="spin-icon" size={14} />}{message}</td></tr>;
}

function InfoRow({ message }: { message: string }) {
  return <div className="empty-table-cell">{message}</div>;
}
