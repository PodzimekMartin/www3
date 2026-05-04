const state = {
  students: [],
  courses: [],
};

const api = async (path, options = {}) => {
  const response = await fetch(path, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
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
  const nextState = await api("/api/state");
  state.students = nextState.students;
  state.courses = nextState.courses;
  render();
};

const render = () => {
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
      <div class="grid-actions">
        <select data-student-select="${course.id}">
          ${state.students.map((student) =>
            `<option value="${student.id}">${escapeHtml(student.name)}</option>`).join("")}
        </select>
        <button data-enroll="${course.id}">Zapsat</button>
        <button class="secondary" data-publish="${course.id}">Publikovat</button>
        <input data-capacity="${course.id}" type="number" min="1" value="${course.capacity}">
        <button class="secondary" data-save-capacity="${course.id}">Ulozit kapacitu</button>
        <button class="secondary" data-session="${course.id}">Pridat termin</button>
      </div>
      <div>
        ${course.enrollments.map((enrollment) => `
          <div class="enrollment">
            <span>${escapeHtml(enrollment.studentName)}</span>
            <span class="status ${enrollment.status === "WAITLISTED" ? "waitlisted" : ""}">
              ${enrollment.status === "WAITLISTED" ? "Ceka" : "Zapsan"}
            </span>
            <button class="danger" data-cancel="${course.id}:${enrollment.studentId}">Zrusit</button>
          </div>
        `).join("")}
      </div>
    `;
    container.append(article);
  });
};

const escapeHtml = (value) => String(value)
  .replaceAll("&", "&amp;")
  .replaceAll("<", "&lt;")
  .replaceAll(">", "&gt;")
  .replaceAll('"', "&quot;");

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
  await handle(() => api("/api/courses", {
    method: "POST",
    body: JSON.stringify({ title: form.get("title"), capacity: Number(form.get("capacity")) }),
  }), "Kurz vytvoren.");
  event.currentTarget.reset();
});

document.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLElement)) {
    return;
  }
  await routeAction(target);
});

document.querySelector("#refreshButton").addEventListener("click", () => handle(loadState, "Data obnovena."));

const routeAction = async (target) => {
  if (target.dataset.enroll) {
    const courseId = target.dataset.enroll;
    const select = document.querySelector(`[data-student-select="${courseId}"]`);
    await handle(() => api(`/api/courses/${courseId}/enroll`, {
      method: "POST",
      body: JSON.stringify({ studentId: Number(select.value) }),
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
  if (target.dataset.session) {
    const start = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
    start.setMinutes(0, 0, 0);
    const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
    await handle(() => api(`/api/courses/${target.dataset.session}/sessions`, {
      method: "POST",
      body: JSON.stringify({ startsAt: toLocalDateTime(start), endsAt: toLocalDateTime(end) }),
    }), "Termin pridan.");
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
};

const toLocalDateTime = (date) => {
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
};

const handle = async (action, successMessage) => {
  try {
    await action();
    showToast(successMessage);
    await loadState();
  } catch (error) {
    showToast(error.message);
  }
};

loadState().catch((error) => showToast(error.message));
