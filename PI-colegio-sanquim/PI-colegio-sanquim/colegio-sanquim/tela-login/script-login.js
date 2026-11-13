// ===== Colégio Sanquim — Tela de Login =====
const form = document.getElementById("loginForm");
const usuario = document.getElementById("usuario");
const senha = document.getElementById("senha");
const lembrar = document.getElementById("lembrar");
const btnLogin = document.getElementById("btnLogin");
const mensagem = document.getElementById("mensagem");
const togglePass = document.getElementById("togglePass");
const eyeIcon = document.getElementById("eyeIcon");
const card = document.querySelector(".login-card");

const OLHO_ABERTO =
  '<path d="M12 5C7 5 2.73 8.11 1 12c1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7zm0 12a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-8a3 3 0 1 0 0 6 3 3 0 0 0 0-6z"/>';
const OLHO_FECHADO =
  '<path d="M12 7a5 5 0 0 1 5 5c0 .65-.13 1.26-.36 1.83l2.92 2.92A11.8 11.8 0 0 0 23 12c-1.73-3.89-6-7-11-7-1.4 0-2.74.25-3.98.7l2.16 2.16A4.86 4.86 0 0 1 12 7zM2 4.27l2.28 2.28.46.46A11.8 11.8 0 0 0 1 12c1.73 3.89 6 7 11 7 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 21 21 19.73 3.27 2 2 3.27zM7.53 9.8l1.55 1.55A2.82 2.82 0 0 0 9 12a3 3 0 0 0 3 3c.22 0 .44-.03.65-.08l1.55 1.55A4.96 4.96 0 0 1 7 12c0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16a3 3 0 0 0-3-3l-.17.01z"/>';

// Ano no rodapé
document.getElementById("ano").textContent = new Date().getFullYear();

// Preenche o usuário salvo (Lembrar de mim)
const salvo = localStorage.getItem("sanquim_usuario");
if (salvo) {
  usuario.value = salvo;
  lembrar.checked = true;
}

// Mostrar / ocultar senha
togglePass.addEventListener("click", () => {
  const oculto = senha.type === "password";
  senha.type = oculto ? "text" : "password";
  eyeIcon.innerHTML = oculto ? OLHO_FECHADO : OLHO_ABERTO;
  togglePass.setAttribute("aria-label", oculto ? "Ocultar senha" : "Mostrar senha");
});

function mostrarErro(input, idErro, texto) {
  input.closest(".field").classList.add("invalido");
  document.getElementById(idErro).textContent = texto;
}

function limparErro(input, idErro) {
  input.closest(".field").classList.remove("invalido");
  document.getElementById(idErro).textContent = "";
}

function mostrarMensagem(texto, tipo) {
  mensagem.textContent = texto;
  mensagem.className = "message visivel " + tipo;
}

function esconderMensagem() {
  mensagem.className = "message";
}

usuario.addEventListener("input", () => limparErro(usuario, "erroUsuario"));
senha.addEventListener("input", () => limparErro(senha, "erroSenha"));

function validar() {
  let ok = true;
  const valorUsuario = usuario.value.trim();
  const valorSenha = senha.value;
  const ehEmail = valorUsuario.includes("@");
  const emailValido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valorUsuario);
  const matriculaValida = /^\d{4,}$/.test(valorUsuario);

  if (!valorUsuario) {
    mostrarErro(usuario, "erroUsuario", "Informe seu e-mail ou matrícula.");
    ok = false;
  } else if (ehEmail && !emailValido) {
    mostrarErro(usuario, "erroUsuario", "Digite um e-mail válido.");
    ok = false;
  } else if (!ehEmail && !matriculaValida) {
    mostrarErro(usuario, "erroUsuario", "A matrícula deve ter apenas números (mín. 4).");
    ok = false;
  }

  if (!valorSenha) {
    mostrarErro(senha, "erroSenha", "Informe sua senha.");
    ok = false;
  } else if (valorSenha.length < 6) {
    mostrarErro(senha, "erroSenha", "A senha deve ter pelo menos 6 caracteres.");
    ok = false;
  }

  return ok;
}

function tremerCard() {
  card.classList.remove("tremer");
  void card.offsetWidth; // reinicia a animação
  card.classList.add("tremer");
}

form.addEventListener("submit", (e) => {
  e.preventDefault();
  esconderMensagem();

  if (!validar()) {
    tremerCard();
    return;
  }

  // Lembrar de mim
  if (lembrar.checked) {
    localStorage.setItem("sanquim_usuario", usuario.value.trim());
  } else {
    localStorage.removeItem("sanquim_usuario");
  }

  btnLogin.disabled = true;
  btnLogin.classList.add("carregando");

  // Simula a verificação no servidor
  setTimeout(() => {
    btnLogin.disabled = false;
    btnLogin.classList.remove("carregando");
    mostrarMensagem("Login realizado com sucesso! Redirecionando...", "sucesso");
  }, 1500);
});

// Esqueci minha senha
document.getElementById("esqueciSenha").addEventListener("click", (e) => {
  e.preventDefault();
  const valorUsuario = usuario.value.trim();
  if (!valorUsuario) {
    mostrarMensagem("Digite seu e-mail ou matrícula para recuperar a senha.", "info");
    usuario.focus();
    return;
  }
  mostrarMensagem("Enviamos as instruções de recuperação para o seu e-mail cadastrado.", "info");
});
