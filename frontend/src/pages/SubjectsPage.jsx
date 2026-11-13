/** Lista matérias; secretaria cria e atribui professores, perfis vinculados abrem a área. */
export default function SubjectsPage({
  subjects,
  users,
  isSecretary,
  busy,
  onCreate,
  onAssignTeacher,
  onDelete,
  onOpen,
}) {
  const teachers = users.filter((user) => user.role === "PROFESSOR");

  function assign(event, subjectId) {
    event.preventDefault();
    const form = event.currentTarget;
    const fields = new FormData(form);
    onAssignTeacher(subjectId, fields.get("teacherId")).then((success) => {
      if (success) form.reset();
    });
  }

  return (
    <main className="content">
      <section className="section-block page-section">
        <div className="section-heading">
          <div><p className="eyebrow">ENSINO</p><h1>Matérias</h1></div>
        </div>
        {isSecretary && (
          <form className="panel form-stack subject-form" onSubmit={onCreate}>
            <h2>Criar matéria e atribuir professor</h2>
            <div className="form-row">
              <div className="form-stack">
                <label htmlFor="subject-name">Nome</label>
                <input id="subject-name" name="name" required maxLength="120" placeholder="Ex.: Matemática" />
              </div>
              <div className="form-stack">
                <label htmlFor="class-name">Turma</label>
                <input id="class-name" name="className" required maxLength="40" placeholder="Ex.: 9º ano B" />
              </div>
            </div>
            <label htmlFor="teacher-id">Professor responsável</label>
            <select id="teacher-id" name="teacherId" required defaultValue="">
              <option value="" disabled>Selecione um professor</option>
              {teachers.map((teacher) => <option key={teacher.id} value={teacher.id}>{teacher.name}</option>)}
            </select>
            <button className="primary-button" disabled={busy || teachers.length === 0} type="submit">
              Criar matéria
            </button>
            {teachers.length === 0 && (
              <p className="muted small">Cadastre um professor antes de criar uma matéria.</p>
            )}
          </form>
        )}
        {subjects.length ? (
          <div className="card-grid">
            {subjects.map((subject) => (
              <article className="panel subject-card" key={subject.id}>
                <span className="card-kicker">{subject.className || "Matéria"}</span>
                <h2>{subject.name}</h2>
                <div className="subject-actions">
                  <button className="primary-button" type="button" onClick={() => onOpen(subject)}>
                    Abrir matéria
                  </button>
                  {isSecretary && (
                    <>
                      <form className="form-stack teacher-assignment"
                        onSubmit={(event) => assign(event, subject.id)}>
                        <label htmlFor={`teacher-${subject.id}`}>Professor responsável</label>
                        <select id={`teacher-${subject.id}`} name="teacherId" required defaultValue={subject.teacherId}>
                          {teachers.map((teacher) => <option key={teacher.id} value={teacher.id}>{teacher.name}</option>)}
                        </select>
                        <button className="secondary-button" disabled={busy || teachers.length === 0} type="submit">
                          Atualizar professor
                        </button>
                      </form>
                      <button className="danger-button" disabled={busy} type="button"
                        onClick={() => {
                          if (window.confirm(`Remover a matéria "${subject.name}"? O servidor bloqueará a remoção se houver histórico acadêmico.`)) {
                            onDelete(subject.id);
                          }
                        }}>
                        Remover matéria
                      </button>
                    </>
                  )}
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            {isSecretary
              ? "Ainda não há matérias cadastradas."
              : "Sua conta ainda não está vinculada a nenhuma matéria."}
          </div>
        )}
      </section>
    </main>
  );
}
