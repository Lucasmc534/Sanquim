/** Exibe o calendário escolar e libera ações administrativas apenas à secretaria. */
export default function EventsPage({ events, isSecretary, busy, onCreate, onDelete }) {
  return (
    <main className="content">
      <section className="section-block page-section">
        <div className="section-heading">
          <div><p className="eyebrow">CALENDÁRIO ESCOLAR</p><h1>Eventos</h1></div>
        </div>
        {isSecretary && (
          <form className="panel form-stack event-form" onSubmit={onCreate}>
            <h2>Publicar evento</h2>
            <label htmlFor="event-title">Título</label>
            <input id="event-title" name="title" required maxLength="160" placeholder="Nome do evento" />
            <label htmlFor="event-description">Descrição</label>
            <textarea id="event-description" name="description" required maxLength="4000" rows="3" placeholder="Detalhes importantes" />
            <label htmlFor="event-date">Data e horário</label>
            <input id="event-date" name="startsAt" type="datetime-local" required />
            <button className="primary-button" disabled={busy} type="submit">Publicar evento</button>
          </form>
        )}
        <div className="event-list">
          {events.map((event) => (
            <article className="panel event-card" key={event.id}>
              <time className="event-date" dateTime={event.startsAt}>
                <strong>{new Date(event.startsAt).toLocaleDateString("pt-BR", { day: "2-digit" })}</strong>
                <span>{new Date(event.startsAt).toLocaleDateString("pt-BR", { month: "short" }).replace(".", "")}</span>
              </time>
              <div><h2>{event.title}</h2><p>{event.description}</p></div>
              <span className="event-time">{new Date(event.startsAt).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" })}</span>
              {isSecretary && (
                <button className="danger-button" disabled={busy} type="button"
                  onClick={() => {
                    if (window.confirm(`Remover o evento "${event.title}"?`)) {
                      onDelete(event.id);
                    }
                  }}>
                  Remover
                </button>
              )}
            </article>
          ))}
          {!events.length && <div className="empty-state">Não há eventos publicados.</div>}
        </div>
      </section>
    </main>
  );
}
