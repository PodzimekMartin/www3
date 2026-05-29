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
  expandedCourseIds: new Set(),
  pendingDeleteCourseId: null,
  session: JSON.parse(window.localStorage.getItem("courseSession") || "null"),
};

const api = async (path, options = {}) => {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (state.session?.token) {
    headers.Authorization = `Bearer ${state.session.token}`;
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
  state.expandedCourseIds = new Set([...state.expandedCourseIds]
    .filter((id) => state.courses.some((course) => course.id === id)));
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
  if (state.session?.role === "STUDENT") {
    const publishedCourses = state.courses.filter((course) => course.status === "PUBLISHED");
    const ownCourses = state.courses.filter((course) => ownEnrollmentFor(course));
    const publishedOpenSeats = publishedCourses.reduce((total, course) =>
      total + Math.max(course.capacity - course.enrolledCount, 0), 0);
    document.querySelector("#stats").innerHTML = [
      ["K", "Dostupne kurzy", publishedCourses.length, "Publikovane kurzy"],
      ["M", "Moje kurzy", ownCourses.length, "Zapsano nebo cekaci listina"],
      ["V", "Volna mista", publishedOpenSeats, "V publikovanych kurzech"],
    ].map(([icon, label, value, detail]) => `
      <article class="stat-card">
        <span class="stat-icon">${icon}</span>
        <span>${label}</span>
        <strong>${value}</strong>
        <p>${detail}</p>
      </article>
    `).join("");
    return;
  }
  const cards = [
    ["K", "Kurzy", state.courses.length, `${published} publikovano, ${drafts} konceptu`],
    ["V", "Volna mista", openSeats, "Napric kurzy"],
  ];
  if (state.session?.role !== "STUDENT") {
    cards.push(["S", "Studenti", state.students.length, `${activeStudents} aktivni, ${blockedStudents} blokovani`]);
  }
  if (state.session?.role === "ADMIN") {
    cards.push(["U", "Vyucujici", state.instructors.length, "Prirazeni ke kurzum"]);
  }
  document.querySelector("#stats").innerHTML = cards.map(([icon, label, value, detail]) => `
    <article class="stat-card">
      <span class="stat-icon">${icon}</span>
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
    container.innerHTML = emptyState(
      state.session?.role === "INSTRUCTOR" ? "Zatim nemas zadny kurz" : "Zadny kurz",
      state.session?.role === "INSTRUCTOR"
        ? "Vytvor prvni kurz vlevo a pridej mu alespon jeden termin."
        : "Zmen filtr nebo vytvor novy kurz s terminem.",
    );
    return;
  }
  if (state.session?.role === "STUDENT") {
    renderStudentCourseGroups(container, courses);
    return;
  }
  courses.forEach((course) => {
    container.append(renderCourseCard(course));
  });
};

const renderStudentCourseGroups = (container, courses) => {
  const publishedCourses = courses.filter((course) => course.status === "PUBLISHED" && course.bookable);
  const ownCourses = courses.filter((course) => ownEnrollmentFor(course));
  container.append(renderCourseGroup(
    "Vsechny kurzy",
    "Publikovane kurzy, na ktere se muzes prihlasit.",
    publishedCourses,
    "Ted neni otevreny zadny kurz.",
  ));
  container.append(renderCourseGroup(
    "Moje kurzy",
    "Kurzy, kde jsi zapsany nebo cekas na misto.",
    ownCourses,
    "Zatim nejsi zapsany na zadny kurz.",
  ));
};

const renderCourseGroup = (title, text, courses, emptyText) => {
  const section = document.createElement("section");
  section.className = "course-group";
  section.innerHTML = `
    <div class="course-group-head">
      <div>
        <h3>${title}</h3>
        <p class="muted">${text}</p>
      </div>
      <span class="group-count">${courses.length}</span>
    </div>
  `;
  const list = document.createElement("div");
  list.className = "course-group-list";
  if (courses.length === 0) {
    list.innerHTML = emptyState(title, emptyText);
  } else {
    courses.forEach((course) => list.append(renderCourseCard(course)));
  }
  section.append(list);
  return section;
};

const renderCourseCard = (course) => {
  const expanded = state.expandedCourseIds.has(course.id);
  const article = document.createElement("article");
  article.className = `course${expanded ? " expanded" : ""}`;
  article.innerHTML = `
      <button class="course-summary" type="button" data-toggle-course="${course.id}" aria-expanded="${expanded}">
        <span class="chevron" aria-hidden="true">›</span>
        <span class="course-summary-main">
          <strong>${escapeHtml(course.title)}</strong>
          <span>Vyucujici: ${escapeHtml(course.instructorName || "Neprirazeno")}</span>
        </span>
        <span class="course-chips">
          ${publicationBadge(course)}
          ${capacityBadge(course)}
        </span>
      </button>
      <div class="course-details ${expanded ? "" : "hidden"}">
        <div class="course-facts">
          <div>
            <span>Zapsano</span>
            <strong>${course.enrolledCount}/${course.capacity}</strong>
          </div>
          <div>
            <span>Cekaci listina</span>
            <strong>${course.waitlistCount}</strong>
          </div>
          <div>
            <span>Terminy</span>
            <strong>${course.sessions.length}</strong>
          </div>
          <div>
            <span>Volna mista</span>
            <strong>${Math.max(course.capacity - course.enrolledCount, 0)}</strong>
          </div>
        </div>
        <div class="course-section">
          <div class="course-section-title">
            <h4>Terminy</h4>
            <span>${course.sessions.length === 0 ? "Bez terminu" : `${course.sessions.length} planovano`}</span>
          </div>
          ${renderSessions(course.sessions)}
        </div>
        ${isCourseManager()
          ? renderCourseManagerActions(course)
          : renderStudentCourseActions(course)}
        <div class="course-section">
          <div class="course-section-title">
            <h4>Zapsani studenti</h4>
            <span>${course.enrollments.length} zaznamu</span>
          </div>
          ${course.enrollments.map((enrollment) => renderEnrollment(course, enrollment)).join("")}
          ${course.enrollments.length === 0 ? `<p class="muted spacing">Zatim bez zapisu.</p>` : ""}
        </div>
      </div>
    `;
  return article;
};

const publicationBadge = (course) => `
  <span class="badge ${course.finished ? "archived" : course.status === "PUBLISHED" ? "published" : "draft"}">
    ${course.finished ? "Probehl" : course.status === "PUBLISHED" ? "Publikovano" : "Koncept"}
  </span>
`;

const capacityBadge = (course) => {
  if (!course.bookable && course.status === "PUBLISHED") {
    return `<span class="badge archived">Uzavreno</span>`;
  }
  if (course.enrolledCount < course.capacity) {
    return `<span class="badge open">Volno ${course.capacity - course.enrolledCount}</span>`;
  }
  if (course.waitlistCount > 0) {
    return `<span class="badge wait">Ceka ${course.waitlistCount}</span>`;
  }
  return `<span class="badge full">Plno</span>`;
};

const renderSessions = (sessions) => {
  if (sessions.length === 0) {
    return `<p class="muted">Zatim neni pridan zadny termin.</p>`;
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
  <div class="course-section">
    <div class="course-section-title">
      <h4>Akce kurzu</h4>
      <span>${course.status === "DRAFT" ? "Koncept lze publikovat" : "Kurz je publikovany"}</span>
    </div>
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
  if (!course.bookable) {
    return `
      <div class="student-actions">
        <button class="secondary" data-view-course-detail="${course.id}">Detail kurzu</button>
        <span class="status muted-status">Termin kurzu uz probehl</span>
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

const setFormMessage = (form, message, type = "error") => {
  let messageElement = form.querySelector(".form-message");
  if (!messageElement) {
    messageElement = document.createElement("p");
    form.append(messageElement);
  }
  messageElement.className = `form-message ${type}`;
  messageElement.textContent = message;
};

const clearFormMessage = (form) => {
  form?.querySelector(".form-message")?.remove();
};

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
  clearFormMessage(event.currentTarget);
  const form = new FormData(event.currentTarget);
  await handle(async () => {
    state.session = await api("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username: form.get("username"), password: form.get("password") }),
    });
    window.localStorage.setItem("courseSession", JSON.stringify(state.session));
    await loadState();
  }, "Prihlaseni probehlo.", event.submitter, event.currentTarget);
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
  clearFormMessage(event.currentTarget);
  const form = new FormData(event.currentTarget);
  const saved = await handle(() => api("/api/students", {
    method: "POST",
    body: JSON.stringify({ name: form.get("name"), email: form.get("email") }),
  }), "Student vytvoren.", event.submitter, event.currentTarget);
  if (saved) {
    event.currentTarget.reset();
  }
});

document.querySelector("#instructorForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  clearFormMessage(event.currentTarget);
  const form = new FormData(event.currentTarget);
  const saved = await handle(() => api("/api/instructors", {
    method: "POST",
    body: JSON.stringify({ name: form.get("name"), email: form.get("email") }),
  }), "Vyucujici vytvoren.", event.submitter, event.currentTarget);
  if (saved) {
    event.currentTarget.reset();
  }
});

document.querySelector("#courseForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  clearFormMessage(event.currentTarget);
  const submitButton = event.submitter;
  const shouldPublish = submitButton?.value === "publish";
  const form = new FormData(event.currentTarget);
  const saved = await handle(async () => {
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
  }, shouldPublish ? "Kurz vytvoren a publikovan." : "Koncept kurzu vytvoren.", submitButton, event.currentTarget);
  if (saved) {
    event.currentTarget.reset();
    resetCourseSessionRows();
  }
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
  if ("closeModal" in target.dataset || target.classList.contains("modal")) {
    closeModals();
    return;
  }
  if ("cancelConfirm" in target.dataset) {
    closeModals();
    return;
  }
  if ("confirmDelete" in target.dataset) {
    const courseId = state.pendingDeleteCourseId;
    if (!courseId) {
      closeModals();
      showToast("Kurz uz neni vybrany.");
      return;
    }
    const deleted = await handle(
      () => api(`/api/courses/${courseId}`, { method: "DELETE" }),
      "Kurz zrusen.",
      target,
    );
    if (deleted) {
      state.expandedCourseIds.delete(courseId);
      closeModals();
    }
    return;
  }
  await routeAction(target);
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
  const courseToggle = target.closest("[data-toggle-course]");
  if (courseToggle) {
    const courseId = Number(courseToggle.dataset.toggleCourse);
    if (state.expandedCourseIds.has(courseId)) {
      state.expandedCourseIds.delete(courseId);
    } else {
      state.expandedCourseIds.add(courseId);
    }
    renderCourses();
    return;
  }
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

const sessionsFromForm = (container) => [...container.querySelectorAll(".session-row")]
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

const handle = async (action, successMessage, button = null, form = null) => {
  try {
    if (button) {
      button.disabled = true;
    }
    await action();
    clearFormMessage(form);
    showToast(successMessage);
    if (state.session) {
      await loadState();
    }
    return true;
  } catch (error) {
    if (error.message.includes("vyprselo") || error.message.includes("Nejprve")) {
      state.session = null;
      window.localStorage.removeItem("courseSession");
      render();
    }
    if (form) {
      setFormMessage(form, error.message);
    }
    showToast(error.message);
    return false;
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
