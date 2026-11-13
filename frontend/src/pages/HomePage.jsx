function SummaryCard({ value, label, onClick }) {
  return (
    <button className="summary-card" onClick={onClick} type="button">
      <strong>{value}</strong><span>{label}</span>
    </button>
  );
}

function Shortcut({ title, description, onClick }) {
  return (
    <button className="shortcut" onClick={onClick} type="button">
      <span className="shortcut-icon" aria-hidden="true">↗</span>
      <strong>{title}</strong><span>{description}</span>
    </button>
  );
}

/** Mostra um resumo e atalhos compatíveis com o perfil da pessoa conectada. */
export default function HomePage({ user, data, onNavigate }) {
  const roleNames = {
    ALUNO: "Aluno",
    PROFESSOR: "Professor",
    SECRETARIA: "Secretaria",
  };
  const isSecretary = user.role === "SECRETARIA";
  const subjectDescription = isSecretary
    ? "Matérias cadastradas"
    : "Matérias vinculadas à sua conta";

  return (
    <main className="content">
      <section className="welcome">
        <div>
          <p className="eyebrow">{(roleNames[user.role] || user.role).toUpperCase()} · ÁREA ESCOLAR</p>
          <h1>Olá, {user.name.split(/\s+/)[0]}!</h1>
          <p>Bem-vindo(a) ao portal do Colégio Sanquim. Acesse abaixo as informações disponíveis para sua conta.</p>
        </div>
        <span className="welcome-mark" aria-hidden="true">S</span>
      </section>
      <section className="summary-grid" aria-label="Resumo">
        <SummaryCard value={data.subjects.length} label={subjectDescription} onClick={() => onNavigate("materias")} />
        <SummaryCard value={data.events.length} label="Eventos da escola" onClick={() => onNavigate("eventos")} />
        {isSecretary && (
          <SummaryCard value={data.users.length} label="Contas cadastradas" onClick={() => onNavigate("contas")} />
        )}
      </section>
      <section className="section-block">
        <div className="section-heading">
          <div><p className="eyebrow">ACESSO RÁPIDO</p><h2>O que você precisa hoje?</h2></div>
        </div>
        <div className="shortcut-grid">
          <Shortcut title="Matérias" description="Consulte turmas e vínculos" onClick={() => onNavigate("materias")} />
          <Shortcut title="Eventos" description="Confira o calendário escolar" onClick={() => onNavigate("eventos")} />
          {isSecretary && (
            <Shortcut title="Contas" description="Cadastre alunos e professores" onClick={() => onNavigate("contas")} />
          )}
        </div>
      </section>
      <section className="section-block">
        <div className="section-heading">
          <div><p className="eyebrow">AGENDA</p><h2>Próximos eventos</h2></div>
          <button className="text-button" onClick={() => onNavigate("eventos")} type="button">Ver todos</button>
        </div>
        <div className="event-list">
          {data.events.slice(0, 3).map((event) => (
            <article className="panel event-card" key={event.id}>
              <time className="event-date" dateTime={event.startsAt}>
                <strong>{new Date(event.startsAt).toLocaleDateString("pt-BR", { day: "2-digit" })}</strong>
                <span>{new Date(event.startsAt).toLocaleDateString("pt-BR", { month: "short" }).replace(".", "")}</span>
              </time>
              <div><h2>{event.title}</h2><p>{event.description}</p></div>
              <span className="event-time">{new Date(event.startsAt).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" })}</span>
            </article>
          ))}
          {!data.events.length && <div className="empty-state">Não há eventos publicados.</div>}
        </div>
      </section>
    </main>
  );
}
