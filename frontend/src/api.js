const API_URL = import.meta.env?.VITE_API_URL || "http://localhost:8080";

/**
 * Sends one API request, attaches the current token, and converts API errors to Error objects.
 */
export async function apiRequest(path, token, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      if (response.ok) throw new Error("A API retornou uma resposta inválida.");
    }
  }

  if (!response.ok) {
    const error = new Error(
      body?.detail || body?.message || `A solicitação falhou (${response.status}).`,
    );
    error.status = response.status;
    throw error;
  }
  return body;
}

/** Returns a user-facing Portuguese message for network and API failures. */
export function readableError(error) {
  if (error instanceof TypeError) {
    return "Não foi possível conectar à API. Verifique se o back-end está ativo.";
  }
  return error.message || "Não foi possível concluir a solicitação.";
}
