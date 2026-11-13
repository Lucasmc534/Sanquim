
/* =================================
   COLÉGIO SANQUIM - PORTAL DO ALUNO
   PAINEL DE CONSULTA
================================= */

/*
  DADOS DEMONSTRATIVOS.

  Em produção, estes dados devem vir
  de uma API autenticada, vinculada
  ao aluno que iniciou a sessão.
*/

const aluno = {
  nome: "Ana Luiza",
  turma: "9º ano B",
  frequencia: 96
};

// NOTAS POR DISCIPLINA

const notas = [
  {
    disciplina: "Matemática",
    bimestres: [8, 8, 9, 8]
  },
  {
    disciplina: "Português",
    bimestres: [9, 8, 9, 9]
  },
  {
    disciplina: "Biologia",
    bimestres: [8, 9, 8, 9]
  },
  {
    disciplina: "História",
    bimestres: [8, 8, 9, 8]
  },
  {
    disciplina: "Educação Física",
    bimestres: [9, 10, 9, 10]
  },
  {
    disciplina: "Química",
    bimestres: [8, 8, 9, 9]
  },
  {
    disciplina: "Inglês",
    bimestres: [8, 9, 8, 9]
  },
  {
    disciplina: "Geografia",
    bimestres: [8, 9, 8, 9]
  }
];

// FALTAS POR DISCIPLINA

const faltas = [
  {
    disciplina: "Matemática",
    quantidade: 2
  },
  {
    disciplina: "Português",
    quantidade: 1
  },
  {
    disciplina: "Biologia",
    quantidade: 0
  },
  {
    disciplina: "História",
    quantidade: 1
  },
  {
    disciplina: "Educação Física",
    quantidade: 2
  },
  {
    disciplina: "Química",
    quantidade: 0
  },
  {
    disciplina: "Inglês",
    quantidade: 1
  },
  {
    disciplina: "Geografia",
    quantidade: 0
  }
];

// PLANOS DE AULA JÁ MINISTRADOS

const planosDeAula = [
  {
    disciplina: "Matemática",
    professor: "Prof. Ricardo",
    data: "09/11/2026",
    conteudo: "Equações do segundo grau: resolução e aplicação de fórmulas."
  },
  {
    disciplina: "Português",
    professor: "Profª. Helena",
    data: "09/11/2026",
    conteudo: "Interpretação textual, figuras de linguagem e produção de texto."
  },
  {
    disciplina: "Biologia",
    professor: "Prof. Marcos",
    data: "06/11/2026",
    conteudo: "Sistema respiratório humano e suas principais funções."
  },
  {
    disciplina: "História",
    professor: "Profª. Cláudia",
    data: "06/11/2026",
    conteudo: "Brasil República: principais acontecimentos históricos."
  },
  {
    disciplina: "Educação Física",
    professor: "Prof. André",
    data: "05/11/2026",
    conteudo: "Atividades práticas, condicionamento físico e trabalho em equipe."
  },
  {
    disciplina: "Química",
    professor: "Profª. Fernanda",
    data: "05/11/2026",
    conteudo: "Introdução às reações químicas e transformações da matéria."
  }
];


/* =================================
   FUNÇÕES DO SISTEMA
================================= */

// FORMATAÇÃO DE NOTAS

function formatarNota(valor) {
  return Number(valor).toFixed(1).replace(".", ",");
}


// CALCULAR MÉDIA DE UMA DISCIPLINA

function calcularMedia(bimestres) {

  const notasValidas = bimestres.filter(
    nota => nota !== null && nota !== undefined
  );

  if (notasValidas.length === 0) {
    return null;
  }

  const soma = notasValidas.reduce(
    (total, nota) => total + nota,
    0
  );

  return soma / notasValidas.length;
}


// CALCULAR MÉDIA GERAL

function calcularMediaGeral() {

  const medias = notas
    .map(item => calcularMedia(item.bimestres))
    .filter(media => media !== null);

  if (medias.length === 0) {
    return 0;
  }

  const soma = medias.reduce(
    (total, media) => total + media,
    0
  );

  return soma / medias.length;
}


// CALCULAR TOTAL DE FALTAS

function calcularTotalFaltas() {

  return faltas.reduce(
    (total, item) => total + item.quantidade,
    0
  );

}


// EXIBIR DATA ATUAL

function carregarData() {

  const elemento = document.getElementById("dataAtual");

  const hoje = new Date();

  const data = hoje.toLocaleDateString("pt-BR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric"
  });

  elemento.textContent = data;

}


// EXIBIR RESUMO DO ALUNO

function carregarResumo() {

  document.getElementById("mediaGeral").textContent =
    formatarNota(calcularMediaGeral());

  document.getElementById("frequenciaGeral").textContent =
    aluno.frequencia + "%";

  document.getElementById("totalFaltas").textContent =
    calcularTotalFaltas();

}


// CARREGAR TABELA DE NOTAS

function carregarNotas() {

  const tabela = document.getElementById("tabelaNotas");

  tabela.innerHTML = "";

  notas.forEach(item => {

    const media = calcularMedia(item.bimestres);

    const valores = item.bimestres.map(nota => {

      if (nota === null || nota === undefined) {
        return "—";
      }

      return formatarNota(nota);

    });

    const classe = media >= 7 ? "nota-boa" : "nota-baixa";

    const linha = document.createElement("tr");

    linha.innerHTML = `
      <td><strong>${item.disciplina}</strong></td>

      ${valores.map(valor => `
        <td class="nota">${valor}</td>
      `).join("")}

      <td class="nota ${classe}">
        ${media === null ? "—" : formatarNota(media)}
      </td>
    `;

    tabela.appendChild(linha);

  });

}


// CARREGAR FREQUÊNCIA

function carregarFrequencia() {

  const percentual = aluno.frequencia;

  document.getElementById(
    "percentualFrequencia"
  ).textContent = percentual + "%";

  document.getElementById(
    "barraFrequencia"
  ).style.width = percentual + "%";

  const situacao = document.getElementById(
    "situacaoFrequencia"
  );

  if (percentual >= 75) {

    situacao.textContent =
      "Sua frequência está acima do mínimo de 75%.";

  } else {

    situacao.textContent =
      "Atenção: sua frequência está abaixo de 75%. Procure a secretaria.";

  }

}


// CARREGAR FALTAS

function carregarFaltas() {

  const tabela = document.getElementById("tabelaFaltas");

  tabela.innerHTML = "";

  faltas.forEach(item => {

    const situacao = item.quantidade > 0
      ? `<span class="status status-atencao">Com faltas</span>`
      : `<span class="status">Sem faltas</span>`;

    const linha = document.createElement("tr");

    linha.innerHTML = `
      <td><strong>${item.disciplina}</strong></td>

      <td class="nota">${item.quantidade}</td>

      <td>${situacao}</td>
    `;

    tabela.appendChild(linha);

  });

}


// CARREGAR PLANOS DE AULA

function carregarPlanos() {

  const lista = document.getElementById("listaPlanos");

  lista.innerHTML = "";

  planosDeAula.forEach(plano => {

    const elemento = document.createElement("article");

    elemento.className = "plano";

    elemento.innerHTML = `
      <div class="plano-info">

        <h3>${plano.disciplina}</h3>

        <p>${plano.conteudo}</p>

        <p>Ministrada por: ${plano.professor}</p>

      </div>

      <span class="plano-data">
        ${plano.data}
      </span>
    `;

    lista.appendChild(elemento);

  });

}


/* =================================
   INICIALIZAÇÃO DO PORTAL
================================= */

function iniciarPortal() {

  carregarData();

  carregarResumo();

  carregarNotas();

  carregarFrequencia();

  carregarFaltas();

  carregarPlanos();

}

// Executa quando a página estiver pronta.

document.addEventListener("DOMContentLoaded", iniciarPortal);
