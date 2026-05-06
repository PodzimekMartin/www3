const state = {
  students: [],
  courses: [],
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
  document.querySelector("#logoutButton").classList.toggle("hidden", !signedIn);
  document.querySelector("#refreshButton").classList.toggle("hidden", !signedIn);
  document.querySelector("#userBadge").textContent = signedIn
    ? `${state.session.displayName} (${state.session.role === "ADMIN" ? "admin" : "student"})`
    : "";

  document.querySelectorAll(".admin-only").forEach((element) => {
    element.classList.toggle("hidden", state.session?.role !== "ADMIN");
  });

  if (!signedIn) {
    return;
  }
  document.querySelector("#summary").textContent =
    `${state.courses.length} kurzu, ${state.students.length} studentu`;
  renderStudents();
  renderCourses();
};

const renderStudents = () => {
  const container = document.querySelector("#students");
  container.innerHTML = "";
  state.students.forEach((student) => {
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

const renderCourses = () => {
  const container = document.querySelector("#courses");
  container.innerHTML = "";
  state.courses.forEach((course) => {
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
        </div>
        <span class="badge ${course.status === "PUBLISHED" ? "published" : "draft"}">
          ${course.status === "PUBLISHED" ? "Publikovano" : "Koncept"}
        </span>
      </div>
      ${renderSessions(course.sessions)}
      ${state.session.role === "ADMIN" ? renderAdminCourseActions(course) : renderStudentCourseActions(course)}
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

const renderAdminCourseActions = (course) => `
  <div class="grid-actions admin-actions">
    <button class="secondary" data-publish="${course.id}">Publikovat</button>
    <input data-capacity="${course.id}" type="number" min="1" value="${course.capacity}">
    <button class="secondary" data-save-capacity="${course.id}">Ulozit kapacitu</button>
  </div>
  <form class="session-inline" data-existing-session-form="${course.id}">
    <label>Datum <input name="date" required type="date" value="${defaultSessionDate()}"></label>
    <label>Od <input name="startsAt" required type="time" value="10:00"></label>
    <label>Do <input name="endsAt" required type="time" value="12:00"></label>
    <button class="secondary" type="submit">Pridat termin</button>
  </form>
`;

const renderStudentCourseActions = (course) => {
  const ownEnrollment = ownEnrollmentFor(course);
  if (ownEnrollment) {
    return `
      <div class="student-actions">
        <span class="status ${ownEnrollment.status === "WAITLISTED" ? "waitlisted" : ""}">
          ${ownEnrollment.status === "WAITLISTED" ? "Jsi na cekaci listine" : "Jsi zapsany"}
        </span>
        <button class="danger" data-cancel="${course.id}:${state.session.studentId}">Zrusit muj zapis</button>
      </div>
    `;
  }
  return `
    <div class="student-actions">
      <button data-enroll="${course.id}">Zapsat se na kurz</button>
    </div>
  `;
};

const renderEnrollment = (course, enrollment) => {
  const canCancel = state.session.role === "ADMIN";
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

document.querySelector("#courseForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  await handle(async () => {
    const course = await api("/api/courses", {
      method: "POST",
      body: JSON.stringify({ title: form.get("title"), capacity: Number(form.get("capacity")) }),
    });
    for (const session of sessionsFromForm(document.querySelector("#courseSessionRows"))) {
      await api(`/api/courses/${course.id}/sessions`, {
        method: "POST",
        body: JSON.stringify(session),
      });
    }
  }, "Kurz vytvoren.");
  event.currentTarget.reset();
  resetCourseSessionRows();
});

document.querySelector("#addCourseSessionButton").addEventListener("click", () => addCourseSessionRow());

document.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLElement)) {
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

const routeAction = async (target) => {
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
    return {
      startsAt: `${value("date")}T${value("startsAt")}:00`,
      endsAt: `${value("date")}T${value("endsAt")}:00`,
    };
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

const handle = async (action, successMessage) => {
  try {
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
  }
};

resetCourseSessionRows();
loadState().catch((error) => {
  state.session = null;
  window.localStorage.removeItem("courseSession");
  render();
  showToast(error.message);
});
