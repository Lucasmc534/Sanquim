const PAGES = [
  ["inicio", "Início"],
  ["materias", "Matérias"],
  ["eventos", "Eventos"],
  ["contas", "Contas"],
];

/** Estrutura visual comum e navegação filtrada conforme o papel autenticado. */
export default function PortalShell({ user, page, onNavigate, onSignOut, children }) {
  const roleNames = {
    ALUNO: "Aluno",
    PROFESSOR: "Professor",
    SECRETARIA: "Secretaria",
  };
  const initials = user.name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
  const pages = user.role === "SECRETARIA"
    ? PAGES
    : PAGES.filter(([id]) => id !== "contas");

  return (
    <div className="portal">
      <header className="topbar">
        <button className="brand" type="button" onClick={() => onNavigate("inicio")}>
          <span className="brand-mark">S</span>
          <span><small>Colégio</small><strong>Sanquim</strong></span>
        </button>
        <nav className="navigation" aria-label="Navegação principal">
          {pages.map(([id, label]) => (
            <button
              className={page === id ? "nav-link active" : "nav-link"}
              key={id}
              onClick={() => onNavigate(id)}
              type="button"
            >
              {label}
            </button>
          ))}
        </nav>
        <div className="account">
          <span className="avatar" aria-hidden="true">{initials}</span>
          <span className="account-name">
            <strong>{user.name}</strong>
            <small>{roleNames[user.role] || user.role}</small>
          </span>
          <button className="logout" onClick={onSignOut} type="button">Sair</button>
        </div>
      </header>
      {children}
      <footer className="footer">Colégio Sanquim <span>·</span> Portal escolar</footer>
    </div>
  );
}
