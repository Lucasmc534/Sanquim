const ROLE_NAMES = {
  ALUNO: "Aluno",
  PROFESSOR: "Professor",
  SECRETARIA: "Secretaria",
};

/** Formulário administrativo e listagem de contas; a API autoriza somente secretaria. */
export default function AccountsPage({ users, busy, onCreate }) {
  return (
    <main className="content">
      <section className="section-block page-section">
        <div className="section-heading">
          <div><p className="eyebrow">ADMINISTRAÇÃO</p><h1>Contas</h1></div>
        </div>
        <form className="panel form-stack account-form" onSubmit={onCreate}>
          <h2>Criar conta</h2>
          <div className="form-row">
            <div className="form-stack">
              <label htmlFor="account-name">Nome completo</label>
              <input id="account-name" name="name" required maxLength="120" placeholder="Nome completo" />
            </div>
            <div className="form-stack">
              <label htmlFor="account-email">E-mail</label>
              <input id="account-email" name="email" type="email" required maxLength="190" placeholder="pessoa@escola.com.br" />
            </div>
          </div>
          <div className="form-row">
            <div className="form-stack">
              <label htmlFor="account-password">Senha inicial</label>
              <input id="account-password" name="password" type="password" required minLength="12" maxLength="72" autoComplete="new-password" />
              <span className="muted small">Use de 12 a 72 caracteres; o servidor também limita a 72 bytes.</span>
            </div>
            <div className="form-stack">
              <label htmlFor="account-role">Perfil</label>
              <select id="account-role" name="role" defaultValue="ALUNO">
                <option value="ALUNO">Aluno</option>
                <option value="PROFESSOR">Professor</option>
                <option value="SECRETARIA">Secretaria</option>
              </select>
            </div>
          </div>
          <button className="primary-button" disabled={busy} type="submit">Criar conta</button>
        </form>
        <div className="panel account-list">
          <div className="section-heading"><h2>Contas cadastradas</h2><span className="count">{users.length}</span></div>
          {users.length ? users.map((account) => (
            <div className="account-row" key={account.id}>
              <span className="avatar">
                {account.name.split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase()}
              </span>
              <span><strong>{account.name}</strong><small>{account.email}</small></span>
              <span className="role-tag">{ROLE_NAMES[account.role] || account.role}</span>
            </div>
          )) : <div className="empty-state">Nenhuma conta cadastrada.</div>}
        </div>
      </section>
    </main>
  );
}
