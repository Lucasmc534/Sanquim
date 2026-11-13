import { useCallback, useEffect, useState } from "react";
import { apiRequest, readableError } from "./api";
import Login from "./components/Login";
import PortalShell from "./components/PortalShell";
import AccountsPage from "./pages/AccountsPage";
import EventsPage from "./pages/EventsPage";
import HomePage from "./pages/HomePage";
import SubjectsPage from "./pages/SubjectsPage";
import SubjectWorkspace from "./pages/SubjectWorkspace";

const TOKEN_KEY = "sanquim_access_token";
const EMPTY_DATA = { users: [], students: [], subjects: [], events: [] };

/** Coordena a sessão, carrega dados compartilhados e encaminha ações às páginas. */
export default function App() {
  const [token, setToken] = useState(() => sessionStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(null);
  const [restoringSession, setRestoringSession] = useState(Boolean(token));
  const [formError, setFormError] = useState("");
  const [pageError, setPageError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(false);
  const [version, setVersion] = useState(0);
  const [page, setPage] = useState("inicio");
  const [selectedSubjectId, setSelectedSubjectId] = useState(null);
  const [data, setData] = useState(EMPTY_DATA);

  const signOut = useCallback(() => {
    sessionStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
    setData(EMPTY_DATA);
    setPage("inicio");
    setSelectedSubjectId(null);
    setFormError("");
    setPageError("");
    setNotice("");
  }, []);

  const expireSession = useCallback(() => {
    signOut();
    setFormError("Sua sessão expirou. Entre novamente.");
  }, [signOut]);

  useEffect(() => {
    if (!token) {
      setRestoringSession(false);
      return undefined;
    }
    let active = true;
    apiRequest("/api/me", token)
      .then((profile) => {
        if (active) setUser(profile);
      })
      .catch((error) => {
        if (!active) return;
        signOut();
        setFormError(error.status === 401
          ? "Sua sessão expirou. Entre novamente."
          : readableError(error));
      })
      .finally(() => {
        if (active) setRestoringSession(false);
      });
    return () => {
      active = false;
    };
  }, [token, signOut]);

  const role = user?.role;
  const refresh = useCallback(async () => {
    if (!token || !role) return;
    setLoading(true);
    setPageError("");
    try {
      const [subjects, events] = await Promise.all([
        apiRequest("/api/subjects", token),
        apiRequest("/api/events", token),
      ]);
      const [users, students] = role === "SECRETARIA"
        ? await Promise.all([
          apiRequest("/api/users", token),
          apiRequest("/api/users/students", token),
        ])
        : role === "PROFESSOR"
          ? [[], await apiRequest("/api/users/students", token)]
          : [[], []];
      setData({ users, students, subjects, events });
    } catch (error) {
      if (error.status === 401) {
        signOut();
        setFormError("Sua sessão expirou. Entre novamente.");
      } else {
        setPageError(readableError(error));
      }
    } finally {
      setLoading(false);
    }
  }, [role, signOut, token]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function handleLogin(event) {
    event.preventDefault();
    setBusy(true);
    setFormError("");
    const form = new FormData(event.currentTarget);
    try {
      const result = await apiRequest("/api/auth/login", null, {
        method: "POST",
        body: JSON.stringify({
          email: String(form.get("email") || "").trim(),
          password: String(form.get("password") || ""),
        }),
      });
      if (!result?.accessToken || !result.userId || !result.role) {
        throw new Error("A API não retornou os dados necessários para iniciar a sessão.");
      }
      sessionStorage.setItem(TOKEN_KEY, result.accessToken);
      setToken(result.accessToken);
      setUser({
        id: result.userId,
        name: result.name,
        email: result.email,
        role: result.role,
      });
      setRestoringSession(false);
      setPage("inicio");
    } catch (error) {
      setFormError(readableError(error));
    } finally {
      setBusy(false);
    }
  }

  // Atualiza a interface somente depois da API confirmar a operação.
  async function runAction(action, successMessage) {
    setBusy(true);
    setPageError("");
    setNotice("");
    try {
      await action();
      await refresh();
      setVersion((previous) => previous + 1);
      setNotice(successMessage);
      return true;
    } catch (error) {
      if (error.status === 401) {
        signOut();
        setFormError("Sua sessão expirou. Entre novamente.");
      } else {
        setPageError(readableError(error));
      }
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function runForm(event, action, successMessage) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const fields = new FormData(formElement);
    const success = await runAction(() => action(fields), successMessage);
    if (success) formElement.reset();
  }

  function navigate(nextPage) {
    setPage(nextPage);
    setSelectedSubjectId(null);
    setPageError("");
    setNotice("");
  }

  if (restoringSession && !user) {
    return <main className="login-screen"><p role="status">Validando sua sessão…</p></main>;
  }

  if (!token || !user) {
    return <Login onSubmit={handleLogin} busy={busy} error={formError} />;
  }

  const isSecretary = role === "SECRETARIA";
  const selectedSubject = data.subjects.find((subject) => subject.id === selectedSubjectId);

  return (
    <PortalShell user={user} page={page} onNavigate={navigate} onSignOut={signOut}>
      <div className="portal-messages">
        {(pageError || notice) && (
          <div className={pageError ? "alert error-alert" : "alert success-alert"} role="status">
            {pageError || notice}
            {pageError && (
              <button type="button" onClick={() => setPageError("")} aria-label="Fechar aviso">×</button>
            )}
          </div>
        )}
        {loading && <p className="loading" role="status">Atualizando informações…</p>}
      </div>
      {page === "inicio" && <HomePage user={user} data={data} onNavigate={navigate} />}
      {page === "materias" && (
        <SubjectsPage
          subjects={data.subjects}
          users={data.users}
          isSecretary={isSecretary}
          busy={busy}
          onCreate={(event) => runForm(event, (fields) => apiRequest("/api/subjects", token, {
            method: "POST",
            body: JSON.stringify({
              name: fields.get("name"),
              className: fields.get("className"),
              teacherId: fields.get("teacherId"),
            }),
          }), "Matéria criada e professor atribuído.")}
          onAssignTeacher={(subjectId, teacherId) => runAction(
            () => apiRequest(`/api/subjects/${subjectId}/teacher`, token, {
              method: "PUT",
              body: JSON.stringify({ teacherId }),
            }),
            "Professor responsável atualizado.",
          )}
          onDelete={(subjectId) => runAction(
            () => apiRequest(`/api/subjects/${subjectId}`, token, { method: "DELETE" }),
            "Matéria removida.",
          )}
          onOpen={(subject) => {
            setSelectedSubjectId(subject.id);
            setPage("material");
            setPageError("");
            setNotice("");
          }}
        />
      )}
      {page === "material" && selectedSubject && (
        <SubjectWorkspace
          subject={selectedSubject}
          user={user}
          token={token}
          busy={busy}
          version={version}
          onBack={() => navigate("materias")}
          onAction={runAction}
          onUnauthorized={expireSession}
        />
      )}
      {page === "material" && !selectedSubject && (
        <main className="content"><div className="empty-state">Matéria não encontrada ou não acessível.</div></main>
      )}
      {page === "eventos" && (
        <EventsPage
          events={data.events}
          isSecretary={isSecretary}
          busy={busy}
          onCreate={(event) => runForm(event, (fields) => apiRequest("/api/events", token, {
            method: "POST",
            body: JSON.stringify({
              title: fields.get("title"),
              description: fields.get("description"),
              startsAt: fields.get("startsAt"),
            }),
          }), "Evento publicado.")}
          onDelete={(eventId) => runAction(
            () => apiRequest(`/api/events/${eventId}`, token, { method: "DELETE" }),
            "Evento removido.",
          )}
        />
      )}
      {page === "contas" && isSecretary && (
        <AccountsPage
          users={data.users}
          busy={busy}
          onCreate={(event) => runForm(event, (fields) => apiRequest("/api/users", token, {
            method: "POST",
            body: JSON.stringify({
              name: fields.get("name"),
              email: fields.get("email"),
              password: fields.get("password"),
              role: fields.get("role"),
            }),
          }), "Conta criada.")}
        />
      )}
    </PortalShell>
  );
}
