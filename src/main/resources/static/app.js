const state = {
  students: [],
  instructors: [],
  courses: [],
  activeAdminTab: "courses",
  filters: {
    courseSearch: "",
    courseStatus: "ALL",
    courseCapacity: "ALL",
    studentSearch: "",
    studentStatus: "ALL",
    instructorSearch: "",
  },
  pendingDeleteCourseId: null,
  session: JSON.parse(window.localStorage.getItem("courseSession") || "null"),
};

const api = async (path, options = {}) => {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.session?.token) {
    headers["X-Auth-Token"] = state.session.token;
  }
  const response = await fetch(path, { headers, ...options });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.message || "Operace se nepodarila.");
  }
  return body;
};

const showToast = (message) => {
  const toast = document.querySelector("#toast");
  toast.textContent = message;
  toast.classList.add("visible");
  window.setTimeout(() => toast.classList.remove("visible"), 3200);
};

const loadState = async () => {
  if (!state.session) {
    render();
    return;
  }
  const nextState = await api("/api/state");
  state.students = nextState.students;
  state.instructors = nextState.instructors;
  state.courses = nextState.courses;
  render();
};

const render = () => {
  const signedIn = Boolean(state.session);
  document.querySelector("#loginPanel").classList.toggle("hidden", signedIn);
  const appContent = document.querySelector("#appContent");
  appContent.classList.toggle("hidden", !signedIn);
  appContent.classList.toggle("admin-mode", state.session?.role === "ADMIN");
  appContent.classList.toggle("student-mode", state.session?.role === "STUDENT");
  appContent.classList.toggle("instructor-mode", state.session?.role === "INSTRUCTOR");
  document.querySelector("#logoutButton").classList.toggle("hidden", !signedIn);
  document.querySelector("#refreshButton").classList.toggle("hidden", !signedIn);
  document.querySelector("#userBadge").textContent = signedIn
    ? `${state.session.displayName} (${roleLabel(state.session.role)})`
    : "";

  document.querySelectorAll(".admin-only").forEach((element) => {
    element.classList.toggle("hidden", state.session?.role !== "ADMIN");
  });
  document.querySelectorAll(".course-manager-only").forEach((element) => {
    element.classList.toggle("hidden", !["ADMIN", "INSTRUCTOR"].includes(state.session?.role));
  });
  renderAdminDashboard();
  syncCourseInstructorField();

  if (!signedIn) {
    return;
  }
  renderStats();
  renderInstructorOptions();
  renderStudents();
  renderInstructors();
  renderCourses();
};

const renderAdminDashboard = () => {
  const isAdmin = state.session?.role === "ADMIN";
  document.querySelectorAll("[data-admin-tab]").forEach((button) => {
    button.classList.toggle("active", button.dataset.adminTab === state.activeAdminTab);
  });
  if (!isAdmin) {
    document.querySelectorAll("[data-dashboard-panel]").forEach((panel) => {
      if (panel.classList.contains("admin-only")) {
        return;
      }
      if (panel.classList.contains("course-manager-only")) {
        panel.classList.toggle("hidden", state.session?.role !== "INSTRUCTOR");
        return;
      }
      panel.classList.remove("hidden");
    });
    return;
  }
  document.querySelectorAll("[data-dashboard-panel]").forEach((panel) => {
    panel.classList.toggle("hidden", panel.dataset.dashboardPanel !== state.activeAdminTab);
  });
};

const renderStats = () => {
  const published = state.courses.filter((course) => course.status === "PUBLISHED").length;
  const drafts = state.courses.filter((course) => course.status === "DRAFT").length;
  const activeStudents = state.students.filter((student) => !student.blocked).length;
  const blockedStudents = state.students.length - activeStudents;
  const openSeats = state.courses.reduce((total, course) =>
    total + Math.max(course.capacity - course.enrolledCount, 0), 0);
  const cards = [
    ["Kurzy", state.courses.length, `${published} publikovano, ${drafts} konceptu`],
    ["Volna mista", openSeats, "Napric kurzy"],
    ["Studenti", state.students.length, `${activeStudents} aktivni, ${blockedStudents} blokovani`],
    ["Vyucujici", state.instructors.length, "Prirazeni ke kurzum"],
  ];
  document.querySelector("#stats").innerHTML = cards.map(([label, value, detail]) => `
    <article class="stat-card">
      <span>${label}</span>
      <strong>${value}</strong>
      <p>${detail}</p>
    </article>
  `).join("");
};

const renderStudents = () => {
  const container = document.querySelector("#students");
  container.innerHTML = "";
  const students = filteredStudents();
  document.querySelector("#studentResultCount").textContent = resultCount(students.length, state.students.length);
  if (students.length === 0) {
    container.innerHTML = emptyState("Zadny student", "Zmen filtr nebo pridej noveho studenta vlevo.");
    return;
  }
  students.forEach((student) => {
    const row = document.createElement("div");
    row.className = "student";
    row.innerHTML = `
      <div>
        <strong>${escapeHtml(student.name)}</strong>
        <p class="muted">${escapeHtml(student.email)}</p>
      </div>
      <button class="secondary" data-block="${student.id}">
        ${student.blocked ? "Odblokovat" : "Blokovat"}
      </button>
    `;
    container.append(row);
  });
};

const renderInstructors = () => {
  const container = document.querySelector("#instructors");
  container.innerHTML = "";
  const instructors = filteredInstructors();
  document.querySelector("#instructorResultCount").textContent = resultCount(instructors.length, state.instructors.length);
  if (instructors.length === 0) {
    container.innerHTML = emptyState("Zadny vyucujici", "Zmen filtr nebo zaloz noveho vyucujiciho vlevo.");
    return;
  }
  instructors.forEach((instructor) => {
    const row = document.createElement("div");
    row.className = "student";
    row.innerHTML = `
      <div>
        <strong>${escapeHtml(instructor.name)}</strong>
        <p class="muted">${escapeHtml(instructor.email)}</p>
      </div>
      <span class="status">Vyucujici</span>
    `;
    container.append(row);
  });
};

const renderInstructorOptions = () => {
  const select = document.querySelector("#courseInstructorSelect");
  select.innerHTML = state.instructors
    .map((instructor) => `
      <option value="${instructor.id}">${escapeHtml(instructor.name)} (${escapeHtml(instructor.email)})</option>
    `)
    .join("");
  syncCourseInstructorField();
};

const syncCourseInstructorField = () => {
  const adminMode = state.session?.role === "ADMIN";
  const field = document.querySelector("#courseInstructorField");
  const select = document.querySelector("#courseInstructorSelect");
  field.classList.toggle("hidden", !adminMode);
  select.disabled = !adminMode;
  select.required = adminMode;
};

const renderCourses = () => {
  const container = document.querySelector("#courses");
  container.innerHTML = "";
  const courses = filteredCourses();
  document.querySelector("#courseResultCount").textContent = resultCount(courses.length, state.courses.length);
  if (courses.length === 0) {
    container.innerHTML = emptyState("Zadny kurz", "Zmen filtr nebo vytvor novy kurz s terminem.");
    return;
  }
  courses.forEach((course) => {
    const article = document.createElement("article");
    article.className = "course";
    article.innerHTML = `
      <div class="course-title">
        <div>
          <h3>${escapeHtml(course.title)}</h3>
          <p class="muted">
            ${course.enrolledCount}/${course.capacity} zapsano,
            ${course.waitlistCount} na cekaci listine
          </p>
          <p class="muted">Vyucujici: ${escapeHtml(course.instructorName || "Neprirazeno")}</p>
        </div>
        <span class="badge ${course.status === "PUBLISHED" ? "published" : "draft"}">
          ${course.status === "PUBLISHED" ? "Publikovano" : "Koncept"}
        </span>
      </div>
      ${renderSessions(course.sessions)}
      ${isCourseManager()
        ? renderCourseManagerActions(course)
        : renderStudentCourseActions(course)}
      <div>
        ${course.enrollments.map((enrollment) => renderEnrollment(course, enrollment)).join("")}
      </div>
    `;
    container.append(article);
  });
};

const renderSessions = (sessions) => {
  if (sessions.length === 0) {
    return `<p class="muted spacing">Zatim neni pridan zadny termin.</p>`;
  }
  return `
    <div class="sessions">
      ${sessions.map((session) => `
        <span>${formatDateTime(session.startsAt)} - ${formatTime(session.endsAt)}</span>
      `).join("")}
    </div>
  `;
};

const renderCourseManagerActions = (course) => `
  <div class="course-actions">
    <div class="action-group ${course.status === "PUBLISHED" ? "single-action" : ""}">
      <span class="action-label">Sprava</span>
      <button class="secondary" data-view-course-detail="${course.id}">Detail</button>
      ${course.status === "DRAFT"
        ? `<button data-publish="${course.id}">Publikovat</button>`
        : ""}
    </div>
    <div class="action-group capacity-action">
      <span class="action-label">Kapacita</span>
      <input aria-label="Kapacita kurzu" data-capacity="${course.id}" type="number" min="1" value="${course.capacity}">
      <button class="secondary" data-save-capacity="${course.id}">Ulozit</button>
    </div>
    <div class="action-group danger-zone">
      <span class="action-label">Kurz</span>
      <button class="danger" data-delete-course="${course.id}">Zrusit kurz</button>
    </div>
  </div>
  <div class="course-subsection">
    <div class="section-head">
      <h3>Pridat termin</h3>
      <p class="muted">Rozsireni rozvrhu kurzu</p>
    </div>
    <form class="session-inline" data-existing-session-form="${course.id}">
      <label>Datum <input name="date" required type="date" value="${defaultSessionDate()}"></label>
      <label>Od <input name="startsAt" required type="time" value="10:00"></label>
      <label>Do <input name="endsAt" required type="time" value="12:00"></label>
      <button class="secondary" type="submit">Pridat termin</button>
    </form>
  </div>
`;

const renderCourseDetail = (course) => `
  <div class="detail-grid">
    <div>
      <span class="detail-label">Vyucujici</span>
      <strong>${escapeHtml(course.instructorName || "Neprirazeno")}</strong>
    </div>
    <div>
      <span class="detail-label">Kapacita</span>
      <strong>${course.enrolledCount}/${course.capacity} zapsano</strong>
    </div>
    <div>
      <span class="detail-label">Cekaci listina</span>
      <strong>${course.waitlistCount}</strong>
    </div>
    <div>
      <span class="detail-label">Stav</span>
      <strong>${course.status === "PUBLISHED" ? "Publikovano" : "Koncept"}</strong>
    </div>
  </div>
  <h3 class="spacing">Terminy</h3>
  ${course.sessions.length === 0
    ? emptyState("Bez terminu", "Kurz zatim nema prirazeny zadny termin.")
    : `<div class="sessions">${course.sessions.map((session) =>
      `<span>${formatDateTime(session.startsAt)} - ${formatTime(session.endsAt)}</span>`).join("")}</div>`}
  <h3 class="spacing">Zapsani studenti</h3>
  ${course.enrollments.length === 0
    ? emptyState("Bez zapisu", "Na kurz se zatim nikdo neprihlasil.")
    : `<div class="students">${course.enrollments.map((enrollment) => `
      <div class="student compact">
        <span>${escapeHtml(enrollment.studentName)}</span>
        <span class="status ${enrollment.status === "WAITLISTED" ? "waitlisted" : ""}">
          ${enrollment.status === "WAITLISTED" ? "Ceka" : "Zapsan"}
        </span>
      </div>
    `).join("")}</div>`}
`;

const renderStudentCourseActions = (course) => {
  if (state.session.role !== "STUDENT") {
    return "";
  }
  const ownEnrollment = ownEnrollmentFor(course);
  if (ownEnrollment) {
    return `
      <div class="student-actions">
        <button class="secondary" data-view-course-detail="${course.id}">Detail kurzu</button>
        <span class="status ${ownEnrollment.status === "WAITLISTED" ? "waitlisted" : ""}">
          ${ownEnrollment.status === "WAITLISTED" ? "Jsi na cekaci listine" : "Jsi zapsany"}
        </span>
        <button class="danger" data-cancel="${course.id}:${state.session.studentId}">Zrusit muj zapis</button>
      </div>
    `;
  }
  return `
    <div class="student-actions">
      <button class="secondary" data-view-course-detail="${course.id}">Detail kurzu</button>
      <button data-enroll="${course.id}">Zapsat se na kurz</button>
    </div>
  `;
};

const renderEnrollment = (course, enrollment) => {
  const canCancel = ["ADMIN", "INSTRUCTOR"].includes(state.session.role);
  const isCurrentStudent = enrollment.studentId === state.session.studentId;
  if (state.session.role === "STUDENT" && !isCurrentStudent) {
    return "";
  }
  return `
    <div class="enrollment">
      <span>${escapeHtml(enrollment.studentName)}</span>
      <span class="status ${enrollment.status === "WAITLISTED" ? "waitlisted" : ""}">
        ${enrollment.status === "WAITLISTED" ? "Ceka" : "Zapsan"}
      </span>
      ${canCancel ? `<button class="danger" data-cancel="${course.id}:${enrollment.studentId}">Zrusit</button>` : ""}
    </div>
  `;
};

const ownEnrollmentFor = (course) =>
  course.enrollments.find((enrollment) => enrollment.studentId === state.session.studentId);

const isCourseManager = () => ["ADMIN", "INSTRUCTOR"].includes(state.session?.role);

const filteredCourses = () => state.courses.filter((course) => {
  const query = normalize(state.filters.courseSearch);
  const matchesText = !query || [
    course.title,
    course.instructorName || "",
    course.status === "PUBLISHED" ? "publikovano" : "koncept",
  ].some((value) => normalize(value).includes(query));
  const matchesStatus = state.filters.courseStatus === "ALL" || course.status === state.filters.courseStatus;
  const hasOpenSeat = course.enrolledCount < course.capacity;
  const matchesCapacity = state.filters.courseCapacity === "ALL"
    || (state.filters.courseCapacity === "OPEN" && hasOpenSeat)
    || (state.filters.courseCapacity === "FULL" && !hasOpenSeat);
  return matchesText && matchesStatus && matchesCapacity;
});

const filteredStudents = () => state.students.filter((student) => {
  const query = normalize(state.filters.studentSearch);
  const matchesText = !query || [student.name, student.email].some((value) => normalize(value).includes(query));
  const matchesStatus = state.filters.studentStatus === "ALL"
    || (state.filters.studentStatus === "BLOCKED" && student.blocked)
    || (state.filters.studentStatus === "ACTIVE" && !student.blocked);
  return matchesText && matchesStatus;
});

const filteredInstructors = () => state.instructors.filter((instructor) => {
  const query = normalize(state.filters.instructorSearch);
  return !query || [instructor.name, instructor.email].some((value) => normalize(value).includes(query));
});

const normalize = (value) => String(value).trim().toLocaleLowerCase("cs-CZ");

const resultCount = (shown, total) => shown === total ? `${total} celkem` : `Zobrazeno ${shown} z ${total}`;

const emptyState = (title, text) => `
  <div class="empty-state">
    <strong>${title}</strong>
    <p>${text}</p>
  </div>
`;

const openCourseDetail = (courseId) => {
  const course = state.courses.find((item) => item.id === Number(courseId));
  if (!course) {
    return;
  }
  document.querySelector("#courseDetailTitle").textContent = course.title;
  document.querySelector("#courseDetailBody").innerHTML = renderCourseDetail(course);
  document.querySelector("#courseDetailModal").classList.remove("hidden");
};

const closeModals = () => {
  document.querySelector("#courseDetailModal").classList.add("hidden");
  document.querySelector("#confirmModal").classList.add("hidden");
  state.pendingDeleteCourseId = null;
};

const openDeleteConfirm = (courseId) => {
  const course = state.courses.find((item) => item.id === Number(courseId));
  state.pendingDeleteCourseId = Number(courseId);
  document.querySelector("#confirmMessage").textContent =
    `Opravdu zrusit kurz "${course?.title || "vybrany kurz"}" vcetne terminu a zapisu?`;
  document.querySelector("#confirmModal").classList.remove("hidden");
};

const roleLabel = (role) => ({
  ADMIN: "admin",
  INSTRUCTOR: "vyucujici",
  STUDENT: "student",
}[role] || role.toLowerCase());

const escapeHtml = (value) => String(value)
  .replaceAll("&", "&amp;")
  .replaceAll("<", "&lt;")
  .replaceAll(">", "&gt;")
  .replaceAll('"', "&quot;");

const loginForm = document.querySelector("#loginForm");
loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await handle(async () => {
    state.session = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username: form.get("username"), password: form.get("password") }),
    });
    window.localStorage.setItem("courseSession", JSON.stringify(state.session));
    await loadState();
  }, "Prihlaseni probehlo.");
});

document.querySelector("#logoutButton").addEventListener("click", () => {
  state.session = null;
  state.students = [];
  state.instructors = [];
  state.courses = [];
  window.localStorage.removeItem("courseSession");
  render();
});

document.querySelector("#studentForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await handle(() => api("/api/students", {
    method: "POST",
    body: JSON.stringify({ name: form.get("name"), email: form.get("email") }),
  }), "Student vytvoren.");
  event.currentTarget.reset();
});

document.querySelector("#instructorForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await handle(() => api("/api/instructors", {
    method: "POST",
    body: JSON.stringify({ name: form.get("name"), email: form.get("email") }),
  }), "Vyucujici vytvoren.");
  event.currentTarget.reset();
});

document.querySelector("#courseForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const submitButton = event.submitter;
  const shouldPublish = submitButton?.value === "publish";
  const form = new FormData(event.currentTarget);
  await handle(async () => {
    const payload = { title: form.get("title"), capacity: Number(form.get("capacity")) };
    if (state.session.role === "ADMIN") {
      if (!form.get("instructorId")) {
        throw new Error("Vyber vyucujiciho pro kurz.");
      }
      payload.instructorId = Number(form.get("instructorId"));
    }
    const sessions = sessionsFromForm(document.querySelector("#courseSessionRows"));
    if (sessions.length === 0) {
      throw new Error("Pridej ke kurzu alespon jeden termin.");
    }
    const course = await api("/api/courses", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    for (const session of sessions) {
      await api(`/api/courses/${course.id}/sessions`, {
        method: "POST",
        body: JSON.stringify(session),
      });
    }
    if (shouldPublish) {
      await api(`/api/courses/${course.id}/publish`, { method: "POST" });
    }
  }, shouldPublish ? "Kurz vytvoren a publikovan." : "Koncept kurzu vytvoren.", submitButton);
  event.currentTarget.reset();
  resetCourseSessionRows();
});

document.querySelector("#addCourseSessionButton").addEventListener("click", () => addCourseSessionRow());

document.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLElement)) {
    return;
  }
  if (target.dataset.adminTab) {
    state.activeAdminTab = target.dataset.adminTab;
    render();
    return;
  }
  if (target.dataset.closeModal || target.classList.contains("modal")) {
    closeModals();
    return;
  }
  if (target.dataset.cancelConfirm) {
    closeModals();
    return;
  }
  if (target.dataset.confirmDelete) {
    const courseId = state.pendingDeleteCourseId;
    closeModals();
    await handle(() => api(`/api/courses/${courseId}`, { method: "DELETE" }), "Kurz zrusen.", target);
    return;
  }
  await routeAction(target);
});

document.addEventListener("submit", async (event) => {
  const form = event.target;
  if (!(form instanceof HTMLFormElement) || !form.dataset.existingSessionForm) {
    return;
  }
  event.preventDefault();
  const [session] = sessionsFromForm(form);
  await handle(() => api(`/api/courses/${form.dataset.existingSessionForm}/sessions`, {
    method: "POST",
    body: JSON.stringify(session),
  }), "Termin pridan.");
  form.reset();
});

document.querySelector("#refreshButton").addEventListener("click", () => handle(loadState, "Data obnovena."));

document.querySelector("#courseSearch").addEventListener("input", (event) => {
  state.filters.courseSearch = event.target.value;
  renderCourses();
});

document.querySelector("#courseStatusFilter").addEventListener("change", (event) => {
  state.filters.courseStatus = event.target.value;
  renderCourses();
});

document.querySelector("#courseCapacityFilter").addEventListener("change", (event) => {
  state.filters.courseCapacity = event.target.value;
  renderCourses();
});

document.querySelector("#studentSearch").addEventListener("input", (event) => {
  state.filters.studentSearch = event.target.value;
  renderStudents();
});

document.querySelector("#studentStatusFilter").addEventListener("change", (event) => {
  state.filters.studentStatus = event.target.value;
  renderStudents();
});

document.querySelector("#instructorSearch").addEventListener("input", (event) => {
  state.filters.instructorSearch = event.target.value;
  renderInstructors();
});

const routeAction = async (target) => {
  if (target.dataset.viewCourseDetail) {
    openCourseDetail(target.dataset.viewCourseDetail);
    return;
  }
  if (target.dataset.enroll) {
    await handle(() => api(`/api/courses/${target.dataset.enroll}/enroll`, {
      method: "POST",
      body: JSON.stringify({ studentId: state.session.studentId }),
    }), "Zapis zpracovan.");
  }
  if (target.dataset.publish) {
    await handle(() => api(`/api/courses/${target.dataset.publish}/publish`, { method: "POST" }), "Kurz publikovan.");
  }
  if (target.dataset.saveCapacity) {
    const courseId = target.dataset.saveCapacity;
    const input = document.querySelector(`[data-capacity="${courseId}"]`);
    await handle(() => api(`/api/courses/${courseId}/capacity`, {
      method: "PATCH",
      body: JSON.stringify({ capacity: Number(input.value) }),
    }), "Kapacita ulozena.");
  }
  if (target.dataset.deleteCourse) {
    openDeleteConfirm(target.dataset.deleteCourse);
  }
  if (target.dataset.cancel) {
    const [courseId, studentId] = target.dataset.cancel.split(":");
    await handle(() => api(`/api/courses/${courseId}/enrollments/${studentId}`, { method: "DELETE" }), "Zapis zrusen.");
  }
  if (target.dataset.block) {
    const student = state.students.find((item) => item.id === Number(target.dataset.block));
    await handle(() => api(`/api/students/${student.id}/blocked`, {
      method: "PATCH",
      body: JSON.stringify({ blocked: !student.blocked }),
    }), student.blocked ? "Student odblokovan." : "Student blokovan.");
  }
  if (target.dataset.removeSessionRow) {
    target.closest(".session-row").remove();
  }
};

const sessionsFromForm = (container) => [...container.querySelectorAll(".session-row, .session-inline")]
  .map((row) => {
    const value = (name) => row.querySelector(`[name="${name}"]`).value;
    const session = {
      startsAt: `${value("date")}T${value("startsAt")}:00`,
      endsAt: `${value("date")}T${value("endsAt")}:00`,
    };
    if (new Date(session.endsAt) <= new Date(session.startsAt)) {
      throw new Error("Cas konce terminu musi byt pozdeji nez cas zacatku.");
    }
    return session;
  });

const addCourseSessionRow = () => {
  const container = document.querySelector("#courseSessionRows");
  const row = document.createElement("div");
  row.className = "session-row";
  row.innerHTML = `
    <label>Datum <input name="date" required type="date" value="${defaultSessionDate()}"></label>
    <label>Od <input name="startsAt" required type="time" value="10:00"></label>
    <label>Do <input name="endsAt" required type="time" value="12:00"></label>
    <button class="danger" data-remove-session-row type="button">Odebrat</button>
  `;
  container.append(row);
};

const resetCourseSessionRows = () => {
  document.querySelector("#courseSessionRows").innerHTML = "";
  addCourseSessionRow();
};

const defaultSessionDate = () => {
  const date = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 10);
};

const formatDateTime = (value) => new Intl.DateTimeFormat("cs-CZ", {
  dateStyle: "medium",
  timeStyle: "short",
}).format(new Date(value));

const formatTime = (value) => new Intl.DateTimeFormat("cs-CZ", {
  timeStyle: "short",
}).format(new Date(value));

const handle = async (action, successMessage, button = null) => {
  try {
    if (button) {
      button.disabled = true;
    }
    await action();
    showToast(successMessage);
    if (state.session) {
      await loadState();
    }
  } catch (error) {
    if (error.message.includes("vyprselo") || error.message.includes("Nejprve")) {
      state.session = null;
      window.localStorage.removeItem("courseSession");
      render();
    }
    showToast(error.message);
  } finally {
    if (button) {
      button.disabled = false;
    }
  }
};

resetCourseSessionRows();
loadState().catch((error) => {
  state.session = null;
  window.localStorage.removeItem("courseSession");
  render();
  showToast(error.message);
});
