import { useState } from "react";

/** Tela de autenticação; o papel é obtido do servidor após o login. */
export default function Login({ onSubmit, busy, error }) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <main className="login-screen">
      <section className="login-card">
        <header className="login-hero">
          <span className="brand-mark">S</span>
          <p>Colégio Sanquim</p>
          <h1>Educação que ilumina o futuro</h1>
        </header>
        <div className="login-body">
          <p className="eyebrow">PORTAL ESCOLAR</p>
          <h2>Bem-vindo de volta!</h2>
          <p className="muted">Entre com seu e-mail e senha para acessar sua área.</p>
          <form className="form-stack" onSubmit={onSubmit}>
            <label htmlFor="email">E-mail</label>
            <input id="email" name="email" type="email" autoComplete="username" required placeholder="voce@escola.com.br" />
            <label htmlFor="password">Senha</label>
            <div className="password-field">
              <input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                required
                placeholder="Sua senha"
              />
              <button type="button" onClick={() => setShowPassword((value) => !value)}>
                {showPassword ? "Ocultar" : "Mostrar"}
              </button>
            </div>
            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="primary-button login-button" disabled={busy} type="submit">
              {busy ? "Entrando…" : "Entrar"}
            </button>
          </form>
          <p className="login-footnote">Seu acesso e suas permissões são definidos pela conta cadastrada na escola.</p>
        </div>
      </section>
    </main>
  );
}
