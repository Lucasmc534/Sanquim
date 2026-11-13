import { useCallback, useEffect, useState } from "react";
import { apiRequest, readableError } from "../api";

const TABS = [
  ["tarefas", "Tarefas"],
  ["notas", "Notas e provas"],
  ["presencas", "Presenças"],
  ["aulas", "Cronograma de aulas"],
];

function formatDate(value, options = {}) {
  return value
    ? new Date(value).toLocaleString("pt-BR", options)
    : "Sem data";
}

// Interpreta a data como calendário local para não deslocar o dia por conversão UTC.
function formatLocalDate(value, options = {}) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString("pt-BR", options);
}

function submitForm(event, action, onAction, successMessage) {
  event.preventDefault();
  const formElement = event.currentTarget;
  const fields = new FormData(formElement);
  onAction(() => action(fields), successMessage).then((success) => {
    if (success) formElement.reset();
  });
}

/** Área da matéria com conteúdo, matrículas e ações disponíveis para cada papel. */
export default function SubjectWorkspace({
  subject,
  user,
  token,
  busy,
  version,
  onBack,
  onAction,
  onUnauthorized,
}) {
  const [tab, setTab] = useState("tarefas");
  const [data, setData] = useState({
    students: [],
    availableStudents: [],
    assignments: [],
    grades: [],
    attendance: [],
    lessons: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submissions, setSubmissions] = useState({});
  const isManager = user.role === "PROFESSOR" || user.role === "SECRETARIA";
  const base = `/api/subjects/${subject.id}`;

  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [assignments, grades, attendance, lessons] = await Promise.all([
        apiRequest(`${base}/assignments`, token),
        apiRequest(`${base}/grades`, token),
        apiRequest(`${base}/attendance`, token),
        apiRequest(`${base}/lessons`, token),
      ]);
      const [students, availableStudents] = isManager
        ? await Promise.all([
          apiRequest(`${base}/students`, token),
          apiRequest("/api/users/students", token),
        ])
        : [[], []];
      setData({ students, availableStudents, assignments, grades, attendance, lessons });
    } catch (requestError) {
      if (requestError.status === 401) onUnauthorized();
      else setError(readableError(requestError));
    } finally {
      setLoading(false);
    }
  }, [base, isManager, onUnauthorized, token]);

  useEffect(() => {
    refresh();
  }, [refresh, version]);

  async function loadSubmissions(assignmentId) {
    try {
      const items = await apiRequest(`${base}/assignments/${assignmentId}/submissions`, token);
      setSubmissions((previous) => ({ ...previous, [assignmentId]: items }));
    } catch (requestError) {
      if (requestError.status === 401) onUnauthorized();
      else setError(readableError(requestError));
    }
  }

  const studentOptions = data.students.map((student) => (
    <option key={student.id} value={student.id}>{student.name}</option>
  ));

  return (
    <main className="content">
      <section className="section-block page-section">
        <button className="text-button back-button" type="button" onClick={onBack}>← Voltar às matérias</button>
        <div className="section-heading">
          <div><p className="eyebrow">{subject.className || "MATÉRIA"}</p><h1>{subject.name}</h1></div>
        </div>
        <nav className="workspace-tabs" aria-label="Conteúdo da matéria">
          {TABS.map(([id, label]) => (
            <button className={tab === id ? "workspace-tab active" : "workspace-tab"}
              key={id} type="button" onClick={() => setTab(id)}>
              {label}
            </button>
          ))}
        </nav>
        {loading && <p className="loading" role="status">Carregando conteúdo da matéria…</p>}
        {error && <p className="form-error" role="alert">{error}</p>}
        {isManager && <Roster data={data} busy={busy} base={base} token={token}
          onAction={onAction} studentOptions={studentOptions} />}
        {tab === "tarefas" && (
          <Assignments
            assignments={data.assignments}
            submissions={submissions}
            students={data.students}
            isManager={isManager}
            isStudent={user.role === "ALUNO"}
            busy={busy}
            base={base}
            token={token}
            onAction={onAction}
            onLoadSubmissions={loadSubmissions}
          />
        )}
        {tab === "notas" && (
          <Grades grades={data.grades} students={data.students} isManager={isManager}
            busy={busy} base={base} token={token} onAction={onAction} studentOptions={studentOptions} />
        )}
        {tab === "presencas" && (
          <AttendanceList records={data.attendance} students={data.students} isManager={isManager}
            busy={busy} base={base} token={token} onAction={onAction} studentOptions={studentOptions} />
        )}
        {tab === "aulas" && (
          <Lessons lessons={data.lessons} isManager={isManager} busy={busy}
            base={base} token={token} onAction={onAction} />
        )}
      </section>
    </main>
  );
}

function Roster({ data, busy, base, token, onAction, studentOptions }) {
  return (
    <section className="panel roster-panel">
      <h2>Alunos vinculados</h2>
      <form className="inline-form" onSubmit={(event) => submitForm(event, (fields) => apiRequest(
        `${base}/students/${fields.get("studentId")}`, token, { method: "POST" },
      ), onAction, "Aluno adicionado à matéria.")}>
        <label className="visually-hidden" htmlFor="roster-student">Adicionar aluno</label>
        <select id="roster-student" name="studentId" required defaultValue="">
          <option value="" disabled>Selecione um aluno</option>
          {data.availableStudents
            .filter((student) => !data.students.some((member) => member.id === student.id))
            .map((student) => <option key={student.id} value={student.id}>{student.name}</option>)}
        </select>
        <button className="secondary-button" disabled={busy || !data.availableStudents.length} type="submit">
          Adicionar aluno
        </button>
      </form>
      {data.students.length ? (
        <ul className="roster-list">
          {data.students.map((student) => (
            <li key={student.id}>
              <span>{student.name}</span>
              <button className="text-button danger-text" disabled={busy} type="button"
                onClick={() => onAction(() => apiRequest(
                  `${base}/students/${student.id}`, token, { method: "DELETE" },
                ), "Aluno removido da matéria.")}>
                Remover
              </button>
            </li>
          ))}
        </ul>
      ) : <p className="muted small">Nenhum aluno está matriculado.</p>}
    </section>
  );
}

function Assignments({ assignments, submissions, students, isManager, isStudent, busy, base, token, onAction, onLoadSubmissions }) {
  return (
    <section className="workspace-section">
      {isManager && (
        <form className="panel form-stack resource-form" onSubmit={(event) => submitForm(event, (fields) => apiRequest(
          `${base}/assignments`, token, {
            method: "POST",
            body: JSON.stringify({
              title: fields.get("title"),
              description: fields.get("description"),
              dueAt: fields.get("dueAt") || null,
            }),
          },
        ), onAction, "Tarefa criada.")}>
          <h2>Criar tarefa</h2>
          <label htmlFor="assignment-title">Título</label>
          <input id="assignment-title" name="title" required maxLength="160" />
          <label htmlFor="assignment-description">Instruções</label>
          <textarea id="assignment-description" name="description" rows="3" maxLength="16000" />
          <label htmlFor="assignment-due">Prazo</label>
          <input id="assignment-due" name="dueAt" type="datetime-local" />
          <button className="primary-button" disabled={busy} type="submit">Publicar tarefa</button>
        </form>
      )}
      {assignments.length ? assignments.map((assignment) => (
        <article className="panel resource-card" key={assignment.id}>
          <div className="section-heading">
            <div><h2>{assignment.title}</h2><p className="muted">{assignment.description}</p></div>
            <span className="resource-date">Prazo: {formatDate(assignment.dueAt, { dateStyle: "short", timeStyle: "short" })}</span>
          </div>
          {isStudent && (
            <form className="form-stack" onSubmit={(event) => submitForm(event, (fields) => apiRequest(
              `${base}/assignments/${assignment.id}/submissions`, token, {
                method: "POST",
                body: JSON.stringify({ text: fields.get("text") }),
              },
            ), onAction, "Entrega enviada.")}>
              <label htmlFor={`submission-${assignment.id}`}>
                {assignment.submitted ? "Atualizar sua entrega" : "Enviar sua tarefa"}
              </label>
              <textarea id={`submission-${assignment.id}`} name="text" required maxLength="16000" rows="3" />
              <button className="primary-button" disabled={busy} type="submit">
                {assignment.submitted ? "Atualizar entrega" : "Enviar tarefa"}
              </button>
            </form>
          )}
          {isManager && (
            <div className="resource-actions">
              <button className="secondary-button" type="button" onClick={() => onLoadSubmissions(assignment.id)}>
                Ver entregas
              </button>
              <button className="danger-button" disabled={busy} type="button"
                onClick={() => onAction(() => apiRequest(
                  `${base}/assignments/${assignment.id}`, token, { method: "DELETE" },
                ), "Tarefa removida. Se houver entregas, o servidor preservará os registros.")}>
                Remover tarefa
              </button>
            </div>
          )}
          {submissions[assignment.id] && (
            <div className="submission-list">
              <h3>Entregas ({submissions[assignment.id].length})</h3>
              {submissions[assignment.id].map((submission) => (
                <article className="submission-item" key={submission.id}>
                  {isManager && <strong>{submission.studentName}</strong>}
                  <p>{submission.text}</p>
                  <small>Enviada em {formatDate(submission.submittedAt, { dateStyle: "short", timeStyle: "short" })}</small>
                </article>
              ))}
              {!submissions[assignment.id].length && <p className="muted small">Nenhuma entrega enviada.</p>}
            </div>
          )}
        </article>
      )) : <div className="empty-state">Não há tarefas nesta matéria.</div>}
      {isManager && students.length === 0 && (
        <p className="muted small">Adicione alunos à matéria para receber entregas.</p>
      )}
    </section>
  );
}

function Grades({ grades, students, isManager, busy, base, token, onAction, studentOptions }) {
  return (
    <section className="workspace-section">
      {isManager && (
        <form className="panel form-stack resource-form" onSubmit={(event) => submitForm(event, (fields) => apiRequest(
          `${base}/grades`, token, {
            method: "POST",
            body: JSON.stringify({
              studentId: fields.get("studentId"),
              title: fields.get("title"),
              score: Number(fields.get("score")),
            }),
          },
        ), onAction, "Nota registrada.")}>
          <h2>Registrar nota ou prova</h2>
          <label htmlFor="grade-student">Aluno</label>
          <select id="grade-student" name="studentId" required defaultValue="">
            <option value="" disabled>Selecione um aluno</option>{studentOptions}
          </select>
          <label htmlFor="grade-title">Avaliação</label>
          <input id="grade-title" name="title" required maxLength="160" />
          <label htmlFor="grade-score">Nota (0 a 100)</label>
          <input id="grade-score" name="score" type="number" min="0" max="100" step="0.01" required />
          <button className="primary-button" disabled={busy || !students.length} type="submit">Salvar nota</button>
        </form>
      )}
      {grades.length ? (
        <div className="record-list">
          {grades.map((grade) => (
            <article className="panel record-row" key={grade.id}>
              <div><h2>{grade.title}</h2><p className="muted">{isManager ? `${grade.studentName} · ` : ""}{formatDate(grade.gradedAt, { dateStyle: "short" })}</p></div>
              <strong className="score">{grade.score}</strong>
              {isManager && <button className="danger-button" disabled={busy} type="button"
                onClick={() => onAction(() => apiRequest(`${base}/grades/${grade.id}`, token, { method: "DELETE" }), "Nota removida.")}>
                Remover
              </button>}
            </article>
          ))}
        </div>
      ) : <div className="empty-state">Não há notas ou provas registradas.</div>}
    </section>
  );
}

function AttendanceList({ records, students, isManager, busy, base, token, onAction, studentOptions }) {
  return (
    <section className="workspace-section">
      {isManager && (
        <form className="panel form-stack resource-form" onSubmit={(event) => submitForm(event, (fields) => apiRequest(
          `${base}/attendance`, token, {
            method: "POST",
            body: JSON.stringify({
              studentId: fields.get("studentId"),
              lessonDate: fields.get("lessonDate"),
              present: fields.get("present") === "true",
            }),
          },
        ), onAction, "Presença registrada.")}>
          <h2>Registrar presença</h2>
          <label htmlFor="attendance-student">Aluno</label>
          <select id="attendance-student" name="studentId" required defaultValue="">
            <option value="" disabled>Selecione um aluno</option>{studentOptions}
          </select>
          <label htmlFor="attendance-date">Data da aula</label>
          <input id="attendance-date" name="lessonDate" type="date" required />
          <label htmlFor="attendance-status">Presença</label>
          <select id="attendance-status" name="present" defaultValue="true">
            <option value="true">Presente</option><option value="false">Ausente</option>
          </select>
          <button className="primary-button" disabled={busy || !students.length} type="submit">Salvar presença</button>
        </form>
      )}
      {records.length ? (
        <div className="record-list">
          {records.map((record) => (
            <article className="panel record-row" key={record.id}>
              <div><h2>{isManager ? record.studentName : "Presença"}</h2><p className="muted">{formatLocalDate(record.lessonDate, { dateStyle: "long" })}</p></div>
              <span className={record.present ? "status-tag present" : "status-tag absent"}>
                {record.present ? "Presente" : "Ausente"}
              </span>
              {isManager && <button className="danger-button" disabled={busy} type="button"
                onClick={() => onAction(() => apiRequest(`${base}/attendance/${record.id}`, token, { method: "DELETE" }), "Registro removido.")}>
                Remover
              </button>}
            </article>
          ))}
        </div>
      ) : <div className="empty-state">Não há presenças registradas.</div>}
    </section>
  );
}

function Lessons({ lessons, isManager, busy, base, token, onAction }) {
  return (
    <section className="workspace-section">
      {isManager && (
        <form className="panel form-stack resource-form" onSubmit={(event) => submitForm(event, (fields) => apiRequest(
          `${base}/lessons`, token, {
            method: "POST",
            body: JSON.stringify({
              title: fields.get("title"),
              description: fields.get("description"),
              startsAt: fields.get("startsAt"),
            }),
          },
        ), onAction, "Aula adicionada ao cronograma.")}>
          <h2>Adicionar aula ao cronograma</h2>
          <label htmlFor="lesson-title">Título</label>
          <input id="lesson-title" name="title" required maxLength="160" />
          <label htmlFor="lesson-description">Descrição</label>
          <textarea id="lesson-description" name="description" rows="3" maxLength="16000" />
          <label htmlFor="lesson-start">Data e horário</label>
          <input id="lesson-start" name="startsAt" type="datetime-local" required />
          <button className="primary-button" disabled={busy} type="submit">Adicionar aula</button>
        </form>
      )}
      {lessons.length ? lessons.map((lesson) => (
        <article className="panel lesson-card" key={lesson.id}>
          <div><h2>{lesson.title}</h2><p className="muted">{lesson.description}</p></div>
          <time>{formatDate(lesson.startsAt, { dateStyle: "medium", timeStyle: "short" })}</time>
          {isManager && <button className="danger-button" disabled={busy} type="button"
            onClick={() => onAction(() => apiRequest(`${base}/lessons/${lesson.id}`, token, { method: "DELETE" }), "Aula removida do cronograma.")}>
            Remover
          </button>}
        </article>
      )) : <div className="empty-state">Não há aulas programadas.</div>}
    </section>
  );
}
