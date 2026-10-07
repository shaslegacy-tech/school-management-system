"use client";

import { AnimatePresence, motion } from "framer-motion";
import {
  Activity, ArrowDownRight, ArrowUpRight, Bell, BookOpen, CalendarCheck, CreditCard,
  CalendarDays, Check, ChevronDown, CircleHelp, Clock3, Download, FileText,
  GraduationCap, Home, Image, LayoutDashboard, Menu, MessageCircle, Moon,
  LogOut, MoreHorizontal, Plus, Search, Settings2, ShieldCheck, Sparkles, Sun, UsersRound,
  X, type LucideIcon,
} from "lucide-react";
import { useState, type FormEvent } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { Button } from "@/components/ui/button";
import AdminWorkspace from "@/components/admin-workspace";
import { apiClient } from "@/lib/api-client";
import type { SessionUser } from "@/stores/session-store";

type BranchOption = { id: string; code: string; name: string };
type StudentRecord = {
  id: string;
  branchId: string;
  sectionId: string | null;
  admissionNumber: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  status: string;
};
type PageResponse<T> = { content: T[]; totalElements: number };
type NavigationItem = { label: string; icon: LucideIcon; count?: string };

const superAdminNavigation: { title: string; items: NavigationItem[] }[] = [
  { title: "Workspace", items: [{ label: "Overview", icon: LayoutDashboard }] },
  { title: "Organization", items: [
    { label: "Branches", icon: Home },
    { label: "Users", icon: UsersRound },
  ] },
  { title: "Setup", items: [
    { label: "Classes", icon: GraduationCap },
    { label: "Academic years", icon: CalendarDays },
  ] },
  { title: "Operations", items: [
    { label: "Students", icon: UsersRound, count: "248" },
    { label: "Attendance", icon: CalendarCheck },
    { label: "Fees", icon: CreditCard },
  ] },
  { title: "Governance", items: [{ label: "Reports", icon: FileText }, { label: "Audit logs", icon: ShieldCheck }, { label: "Announcements", icon: Bell }] },
];
const branchAdminNavigation: { title: string; items: NavigationItem[] }[] = [
  { title: "Workspace", items: [{ label: "Overview", icon: LayoutDashboard }] },
  { title: "School", items: [
    { label: "Students", icon: UsersRound }, { label: "Attendance", icon: CalendarCheck },
    { label: "Classes & staff", icon: GraduationCap }, { label: "Timetable", icon: Clock3 },
  ] },
  { title: "Learning", items: [
    { label: "Daily activity", icon: Sparkles }, { label: "Syllabus", icon: BookOpen }, { label: "Gallery", icon: Image },
  ] },
  { title: "Connect", items: [{ label: "Announcements", icon: Bell }, { label: "Messages", icon: MessageCircle }] },
  { title: "Insights", items: [{ label: "Reports", icon: FileText }] },
];
const chartData = {
  Week: [
    { day: "Mon", present: 91, absent: 9 }, { day: "Tue", present: 94, absent: 6 },
    { day: "Wed", present: 89, absent: 11 }, { day: "Thu", present: 96, absent: 4 },
    { day: "Fri", present: 93, absent: 7 }, { day: "Sat", present: 97, absent: 3 }, { day: "Sun", present: 95, absent: 5 },
  ],
  Month: [
    { day: "Wk 1", present: 92, absent: 8 }, { day: "Wk 2", present: 94, absent: 6 },
    { day: "Wk 3", present: 91, absent: 9 }, { day: "Wk 4", present: 96, absent: 4 },
  ],
  Term: [
    { day: "Jun", present: 90, absent: 10 }, { day: "Jul", present: 93, absent: 7 },
    { day: "Aug", present: 92, absent: 8 }, { day: "Sep", present: 95, absent: 5 }, { day: "Oct", present: 96, absent: 4 },
  ],
};
const activityItems = [
  { icon: Check, tone: "", title: "Attendance completed for Junior KG A", meta: "By Priya Nair · 8:42 AM" },
  { icon: Sparkles, tone: "coral", title: "A new daily activity was shared", meta: "Nature walk · Nursery B · 9:16 AM" },
  { icon: MessageCircle, tone: "blue", title: "New message from Rhea Mehta", meta: "Parent of Aanya · 9:31 AM" },
];
const classSummary = [
  { name: "Day Care", students: 32, present: 29 }, { name: "Play Group", students: 48, present: 45 },
  { name: "Nursery", students: 56, present: 51 }, { name: "Junior KG", students: 62, present: 59 },
  { name: "Senior KG", students: 50, present: 47 },
];

function initialsFor(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase() ?? "").join("");
}

export default function SchoolDashboard({ user, onLogout }: { user: SessionUser; onLogout: () => void }) {
  const queryClient = useQueryClient();
  const [activePage, setActivePage] = useState("Overview");
  const [activeBranchId, setActiveBranchId] = useState(user.role === "SUPER_ADMIN" ? "" : user.branchId ?? "");
  const [range, setRange] = useState<keyof typeof chartData>("Week");
  const [darkMode, setDarkMode] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [toast, setToast] = useState("");
  const [studentDialog, setStudentDialog] = useState(false);
  const [savingStudent, setSavingStudent] = useState(false);
  const userInitials = initialsFor(user.displayName);
  const firstName = user.displayName.split(" ")[0] || "there";
  const branchesQuery = useQuery({
    queryKey: ["branches", user.id],
    queryFn: async () => (await apiClient.get<BranchOption[]>("/branches")).data,
  });
  const sectionsQuery = useQuery({
    queryKey: ["sections", "admission", user.id, activeBranchId],
    queryFn: async () => (await apiClient.get<{ id: string; branchId: string; sectionName: string }[]>("/sections", { params: { branchId: activeBranchId || undefined } })).data,
  });
  const navigation = user.role === "SUPER_ADMIN" ? superAdminNavigation : branchAdminNavigation;
  const activeBranch = branchesQuery.data?.find((branch) => branch.id === activeBranchId);
  const branchLabel = activeBranch?.name ?? (activeBranchId ? "Selected branch" : "All branches");
  const studentsQuery = useQuery({
    queryKey: ["students", user.id, activeBranchId, search],
    queryFn: async () => (await apiClient.get<PageResponse<StudentRecord>>("/students", {
      params: { branchId: activeBranchId || undefined, search: search.trim() || undefined, page: 0, size: 100 },
    })).data,
  });
  const students = studentsQuery.data?.content ?? [];
  const studentCount = studentsQuery.data?.totalElements ?? 0;

  function announce(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(""), 2800);
  }
  function exportStudents() {
    const rows = [["Student", "Admission number", "Section", "Status"], ...students.map((student) => [`${student.firstName} ${student.lastName}`, student.admissionNumber, student.sectionId ?? "Unassigned", student.status])];
    const csv = rows.map((row) => row.map((value) => `"${value.replaceAll('"', '""')}"`).join(",")).join("\n");
    const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "little-home-students.csv";
    anchor.click();
    URL.revokeObjectURL(url);
    announce("Student list exported as CSV");
  }
  async function createStudent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const branchId = user.role === "SUPER_ADMIN" ? String(formData.get("branchId") ?? "") : null;
    const admissionNumber = String(formData.get("admissionNumber") ?? "").trim();
    const firstName = String(formData.get("firstName") ?? "").trim();
    const lastName = String(formData.get("lastName") ?? "").trim();
    const dateOfBirth = String(formData.get("dateOfBirth") ?? "");
    const sectionId = String(formData.get("sectionId") ?? "") || null;
    if (user.role === "SUPER_ADMIN" && !branchId) {
      announce("Choose a branch before creating this admission");
      return;
    }
    setSavingStudent(true);
    try {
      await apiClient.post("/students", { branchId, sectionId, admissionNumber, firstName, lastName, dateOfBirth, gender: "UNSPECIFIED" });
      await queryClient.invalidateQueries({ queryKey: ["students", user.id] });
      setStudentDialog(false);
      announce(`${firstName} ${lastName} added to the student register`);
    } catch {
      announce("Admission could not be saved. Check the details and try again.");
    } finally {
      setSavingStudent(false);
    }
  }

  return (
    <main className="school-shell" data-theme={darkMode ? "dark" : "light"}>
      <div className={`mobile-overlay ${menuOpen ? "visible" : ""}`} onClick={() => setMenuOpen(false)} />
      <aside className={`sidebar ${menuOpen ? "open" : ""}`} aria-label="Main navigation">
        <div className="brand"><div className="brand-mark"><Home size={19} strokeWidth={2.5} /></div><div><div className="brand-name">Little Home</div><div className="brand-caption">SCHOOL MANAGEMENT</div></div></div>
        <label className="branch-switcher"><span className="branch-dot" /><select aria-label="Choose a branch" className="branch-select" value={activeBranchId} onChange={(event) => { setActiveBranchId(event.target.value); announce("Branch view updated"); }} disabled={branchesQuery.isLoading || user.role !== "SUPER_ADMIN"}><option value="">All branches</option>{branchesQuery.data?.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select><ChevronDown size={13} aria-hidden="true" /></label>
        <nav className="nav-scroll">{navigation.map((group) => <div className="nav-group" key={group.title}><div className="nav-label">{group.title}</div>{group.items.map(({ label, icon: Icon, count }) => <button className={`nav-item ${activePage === label ? "active" : ""}`} key={label} onClick={() => { setActivePage(label); setMenuOpen(false); }} type="button"><Icon size={15} strokeWidth={1.9} /><span>{label}</span>{count && <span className="nav-count">{count}</span>}</button>)}</div>)}</nav>
        <div className="sidebar-bottom">
          <button className="nav-item" onClick={() => announce("Workspace settings are ready to connect")} type="button"><Settings2 size={15} /><span>Settings</span></button>
          <button className="nav-item" onClick={() => announce("Help centre is coming soon")} type="button"><CircleHelp size={15} /><span>Help centre</span></button>
          <div className="profile"><div className="profile-avatar">{userInitials}</div><div><div className="profile-name">{user.displayName}</div><div className="profile-role">{user.role.replaceAll("_", " ")}</div></div><button className="icon-button" aria-label="Sign out" onClick={onLogout} type="button"><LogOut size={15} /></button></div>
        </div>
      </aside>

      <section className="main-panel">
        <header className="topbar"><div className="topbar-left"><button className="icon-button mobile-menu" aria-label="Open navigation" onClick={() => setMenuOpen(true)} type="button"><Menu size={17} /></button><span className="crumb">Little Home School</span><span className="crumb-separator">/</span><span className="crumb-current">{activePage}</span></div>
          <div className="topbar-actions">{searchOpen && <input autoFocus aria-label="Search students" className="search-field" onChange={(event) => setSearch(event.target.value)} placeholder="Search students..." value={search} />}<button className="icon-button" aria-label={searchOpen ? "Close search" : "Search"} onClick={() => { setSearchOpen((open) => !open); setSearch(""); }} type="button"><Search size={16} /></button><button className="icon-button" aria-label="Notifications" onClick={() => announce("You are all caught up")} type="button"><Bell size={16} /><span className="notification-dot" /></button><button className="icon-button" aria-label={darkMode ? "Switch to light theme" : "Switch to dark theme"} onClick={() => setDarkMode((dark) => !dark)} type="button">{darkMode ? <Sun size={16} /> : <Moon size={15} />}</button><span className="topbar-divider" /><div className="header-avatar" aria-label={user.displayName}>{userInitials}</div></div>
        </header>

        <div className="content-wrap">
          <div className="page-heading"><div><div className="eyebrow">WEDNESDAY, 07 OCTOBER 2026 <span style={{ color: "var(--orange)" }}>·</span> ACADEMIC YEAR 2026–27</div><h1 className="page-title">{activePage === "Overview" ? `Good morning, ${firstName}` : activePage}</h1><p className="page-subtitle">Here&apos;s what&apos;s happening at {branchLabel === "All branches" ? "Little Home School" : branchLabel.replace("Little Home · ", "")} today.</p></div>
            <div className="heading-actions"><span className="date-chip"><CalendarDays size={13} /> 07 Oct 2026</span><button className="outline-action" onClick={exportStudents} type="button"><Download size={13} /> <span>Export</span></button><Button className="primary-action" onClick={() => setStudentDialog(true)} type="button"><Plus size={14} /> Add student</Button></div>
          </div>

          {activePage === "Overview" ? <>
            <div className="metrics-grid"><MetricCard icon={UsersRound} label="Total students" value={String(studentCount)} change="" note="across selected branches" /><MetricCard icon={CalendarCheck} label="Present today" value="—" change="" note="attendance module pending" /><MetricCard icon={GraduationCap} label="Teaching staff" value="—" change="" note="staff module pending" /><MetricCard icon={Activity} label="Pending actions" value="—" change="" note="live notifications pending" /></div>
            <div className="overview-grid">
              <section className="panel" aria-labelledby="attendance-chart-heading"><div className="panel-header"><div><h2 className="panel-title" id="attendance-chart-heading">Attendance overview</h2><div className="panel-note">Sample trend · attendance API pending</div></div><div style={{ display: "flex", alignItems: "center", gap: 13 }}><div className="chart-legend"><span><i className="legend-dot" />Present</span><span><i className="legend-dot orange" />Absent</span></div><div className="range-control" aria-label="Chart time range">{(Object.keys(chartData) as (keyof typeof chartData)[]).map((option) => <button className={`range-button ${range === option ? "selected" : ""}`} key={option} onClick={() => setRange(option)} type="button">{option}</button>)}</div></div></div>
                <div className="chart-body"><ResponsiveContainer width="100%" height="100%"><AreaChart data={chartData[range]} margin={{ top: 8, right: 10, left: -18, bottom: 0 }}><defs><linearGradient id="presentFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#70ad86" stopOpacity={0.22} /><stop offset="100%" stopColor="#70ad86" stopOpacity={0.01} /></linearGradient><linearGradient id="absentFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#e9865b" stopOpacity={0.17} /><stop offset="100%" stopColor="#e9865b" stopOpacity={0.01} /></linearGradient></defs><CartesianGrid stroke="var(--line)" strokeDasharray="3 5" vertical={false} /><XAxis axisLine={false} tickLine={false} dataKey="day" tick={{ fill: "#87948c", fontSize: 9 }} dy={8} /><YAxis axisLine={false} tickLine={false} domain={[0, 100]} ticks={[0, 25, 50, 75, 100]} tick={{ fill: "#87948c", fontSize: 9 }} tickFormatter={(value: number) => `${value}%`} /><Tooltip content={<ChartTooltip />} /><Area type="monotone" dataKey="present" name="Present" stroke="#4f916b" strokeWidth={2.3} fill="url(#presentFill)" activeDot={{ r: 4, strokeWidth: 0 }} /><Area type="monotone" dataKey="absent" name="Absent" stroke="#e9865b" strokeWidth={1.8} fill="url(#absentFill)" activeDot={{ r: 3, strokeWidth: 0 }} /></AreaChart></ResponsiveContainer></div>
                <div className="attendance-summary"><span>School average <strong>93.1%</strong></span><span>Target <strong>95%</strong></span><span>Updated <strong>9:42 AM</strong></span></div>
              </section>
              <section className="panel" aria-labelledby="class-attendance-heading"><div className="panel-header"><div><h2 className="panel-title" id="class-attendance-heading">By class</h2><div className="panel-note">Attendance API pending · sample only</div></div></div><div className="class-list">{classSummary.map((item) => { const percentage = Math.round((item.present / item.students) * 100); return <div className="class-row" key={item.name}><div><div className="class-title">{item.name}</div><div className="class-subtitle">{item.students} students</div></div><div className="class-count">{percentage}%</div><div className="progress-track"><div className="progress-fill" style={{ width: `${percentage}%` }} /></div></div>; })}</div></section>
            </div>
            <div className="activity-grid">
              <section className="panel" aria-labelledby="students-heading"><div className="panel-header"><div><h2 className="panel-title" id="students-heading">Student register</h2><div className="panel-note">Live records · {studentsQuery.isLoading ? "loading" : `${studentCount} students`}</div></div><button className="table-action" onClick={() => setActivePage("Students")} type="button">View all <ArrowUpRight size={12} /></button></div><div className="table-wrap"><table className="student-table"><thead><tr><th>STUDENT</th><th>ADMISSION</th><th>STATUS</th><th aria-label="Actions" /></tr></thead><tbody>{students.slice(0, 5).map((student) => { const name = `${student.firstName} ${student.lastName}`; return <tr key={student.id}><td><div className="student-person"><div className="student-avatar" style={{ background: "#d9e9d9", color: "#38463d" }}>{initialsFor(name)}</div><div><div className="student-name">{name}</div><div className="student-code">{student.sectionId ? "Section assigned" : "Section unassigned"}</div></div></div></td><td>{student.admissionNumber}</td><td><span className="status-pill">{student.status}</span></td><td><button className="icon-button" aria-label={`More actions for ${name}`} onClick={() => announce(`${name} student record selected`)} type="button"><MoreHorizontal size={15} /></button></td></tr>; })}{studentsQuery.isLoading && <tr><td colSpan={4} style={{ height: 72, textAlign: "center", color: "var(--muted-ink)" }}>Loading student records...</td></tr>}{studentsQuery.isError && <tr><td colSpan={4} style={{ height: 72, textAlign: "center", color: "var(--muted-ink)" }}>Student records could not be loaded.</td></tr>}{!studentsQuery.isLoading && !studentsQuery.isError && students.length === 0 && <tr><td colSpan={4} style={{ height: 72, textAlign: "center", color: "var(--muted-ink)" }}>No students in this branch yet.</td></tr>}</tbody></table></div></section>
              <section className="panel" aria-labelledby="activity-heading"><div className="panel-header"><div><h2 className="panel-title" id="activity-heading">School activity</h2><div className="panel-note">Recent updates from your branch</div></div><button className="icon-button" aria-label="View activity options" onClick={() => announce("Showing the latest school updates")} type="button"><MoreHorizontal size={16} /></button></div><div className="activity-list">{activityItems.map(({ icon: Icon, tone, title, meta }) => <div className="activity-item" key={title}><div className={`activity-icon ${tone}`}><Icon size={14} /></div><div className="activity-copy"><div className="activity-title">{title}</div><div className="activity-meta">{meta}</div></div></div>)}</div></section>
            </div>
          </> : activePage === "Students" ? <StudentsWorkspace students={students} loading={studentsQuery.isLoading} error={studentsQuery.isError} totalStudents={studentCount} onCreate={() => setStudentDialog(true)} /> : ["Branches", "Users", "Classes", "Classes & staff", "Academic years", "Attendance", "Fees", "Announcements"].includes(activePage) ? <AdminWorkspace page={activePage} user={user} branchId={activeBranchId} onToast={announce} /> : <ModuleWorkspace page={activePage} onCreate={() => announce(`${activePage} is not available in this workspace yet`)} />}
        </div>
      </section>

      <AnimatePresence>{toast && <motion.div className="toast" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 8 }} role="status"><ShieldCheck size={16} color="var(--green)" />{toast}</motion.div>}</AnimatePresence>
      <AnimatePresence>{studentDialog && <motion.div className="modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onMouseDown={(event) => { if (event.target === event.currentTarget) setStudentDialog(false); }}><motion.form className="modal" initial={{ opacity: 0, y: 10, scale: .98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 8, scale: .98 }} onSubmit={createStudent}><div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}><div><h2 className="modal-title">Add a student</h2><p className="modal-description">Create a student record for {branchLabel}.</p></div><button className="icon-button" aria-label="Close" onClick={() => setStudentDialog(false)} type="button"><X size={16} /></button></div>{user.role === "SUPER_ADMIN" && <label className="form-field">Branch<select name="branchId" defaultValue={activeBranchId} required><option value="">Select a branch</option>{branchesQuery.data?.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select></label>}<label className="form-field">Class section<select name="sectionId" defaultValue=""><option value="">Not assigned</option>{sectionsQuery.data?.map((section) => <option key={section.id} value={section.id}>{section.sectionName} · {section.id.slice(0, 8)}</option>)}</select></label><label className="form-field">Admission number<input autoComplete="off" name="admissionNumber" placeholder="e.g. LHS-27001" required /></label><label className="form-field">First name<input autoFocus name="firstName" placeholder="e.g. Samira" required /></label><label className="form-field">Last name<input name="lastName" placeholder="e.g. Patel" required /></label><label className="form-field">Date of birth<input name="dateOfBirth" required type="date" /></label><div className="modal-actions"><button className="outline-action" onClick={() => setStudentDialog(false)} type="button">Cancel</button><button className="primary-action" disabled={savingStudent} type="submit"><Plus size={13} /> {savingStudent ? "Saving..." : "Create student"}</button></div></motion.form></motion.div>}</AnimatePresence>
    </main>
  );
}

function StudentsWorkspace({ students, loading, error, totalStudents, onCreate }: { students: StudentRecord[]; loading: boolean; error: boolean; totalStudents: number; onCreate: () => void }) {
  return <section className="panel"><div className="panel-header"><div><h2 className="panel-title">Student directory</h2><div className="panel-note">{totalStudents} records in your authorized branch scope</div></div><button className="primary-action" onClick={onCreate} type="button"><Plus size={13} /> Add student</button></div><div className="table-wrap"><table className="student-table"><thead><tr><th>STUDENT</th><th>ADMISSION NUMBER</th><th>SECTION</th><th>STATUS</th></tr></thead><tbody>{students.map((student) => { const fullName = `${student.firstName} ${student.lastName}`; return <tr key={student.id}><td><div className="student-person"><div className="student-avatar" style={{ background: "#d9e9d9", color: "#38463d" }}>{initialsFor(fullName)}</div><div><div className="student-name">{fullName}</div><div className="student-code">Born {student.dateOfBirth}</div></div></div></td><td>{student.admissionNumber}</td><td>{student.sectionId ? "Assigned" : "Unassigned"}</td><td><span className="status-pill">{student.status}</span></td></tr>; })}{loading && <tr><td colSpan={4} style={{ height: 72, textAlign: "center", color: "var(--muted-ink)" }}>Loading student records...</td></tr>}{error && <tr><td colSpan={4} style={{ height: 72, textAlign: "center", color: "var(--muted-ink)" }}>Student records could not be loaded. Sign in again or retry.</td></tr>}{!loading && !error && students.length === 0 && <tr><td colSpan={4} style={{ height: 72, textAlign: "center", color: "var(--muted-ink)" }}>No students match this view.</td></tr>}</tbody></table></div></section>;
}

function MetricCard({ icon: Icon, label, value, change, note, negative = false }: { icon: LucideIcon; label: string; value: string; change: string; note: string; negative?: boolean }) {
  return <motion.section className="metric-card" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: .28 }}><div className="metric-top"><span className="metric-label">{label}</span><span className="metric-icon"><Icon size={14} /></span></div><div className="metric-value">{value}</div><div className="metric-foot"><span className={`metric-change ${negative ? "down" : ""}`}>{negative ? <ArrowDownRight size={11} /> : <ArrowUpRight size={11} />}{change}</span><span>{note}</span></div></motion.section>;
}

function ChartTooltip({ active, payload, label }: { active?: boolean; payload?: { name: string; value: number; color: string }[]; label?: string }) {
  if (!active || !payload?.length) return null;
  return <div className="chart-tooltip"><div style={{ marginBottom: 6, fontWeight: 800 }}>{label}</div>{payload.map((item) => <div key={item.name} style={{ display: "flex", justifyContent: "space-between", gap: 16, color: item.color, marginTop: 3 }}><span>{item.name}</span><strong>{item.value}%</strong></div>)}</div>;
}

function ModuleWorkspace({ page, onCreate }: { page: string; onCreate: () => void }) {
  const pageIcons: Record<string, LucideIcon> = { Students: UsersRound, Attendance: CalendarCheck, "Classes & staff": GraduationCap, Timetable: Clock3, "Daily activity": Sparkles, Syllabus: BookOpen, Gallery: Image, Announcements: Bell, Messages: MessageCircle, Reports: FileText };
  const Icon = pageIcons[page] ?? LayoutDashboard;
  return <section className="panel module-empty"><div><div className="empty-mark"><Icon size={20} /></div><h2 className="empty-title">{page === "Students" ? "Student records" : page}</h2><p className="empty-copy">{page === "Students" ? "Your student directory is ready for admissions and profile updates." : `Your ${page.toLowerCase()} workspace is ready for today's school operations.`}</p><button className="primary-action" onClick={onCreate} type="button"><Plus size={13} /> {page === "Students" ? "Add student" : `Create ${page.toLowerCase()}`}</button></div></section>;
}