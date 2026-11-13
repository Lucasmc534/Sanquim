import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import { apiRequest, readableError } from "./api.js";

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
});

test("apiRequest sends JSON and the bearer token", async () => {
  let request;
  globalThis.fetch = async (url, options) => {
    request = { url, options };
    return new Response(JSON.stringify({ status: "UP" }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  };

  const result = await apiRequest("/api/me", "test-token", {
    method: "POST",
    body: JSON.stringify({ value: "test" }),
  });

  assert.equal(request.url, "http://localhost:8080/api/me");
  assert.equal(request.options.headers.Authorization, "Bearer test-token");
  assert.equal(request.options.headers["Content-Type"], "application/json");
  assert.deepEqual(result, { status: "UP" });
});

test("apiRequest surfaces API problem details and status", async () => {
  globalThis.fetch = async () => new Response(JSON.stringify({
    status: 403,
    detail: "Você não pode alterar esta matéria.",
  }), {
    status: 403,
    headers: { "Content-Type": "application/problem+json" },
  });

  await assert.rejects(apiRequest("/api/subjects/1", "test-token"), (error) => {
    assert.equal(error.status, 403);
    assert.equal(error.message, "Você não pode alterar esta matéria.");
    return true;
  });
});

test("readableError explains network failures in Portuguese", () => {
  assert.equal(
    readableError(new TypeError("Failed to fetch")),
    "Não foi possível conectar à API. Verifique se o back-end está ativo.",
  );
});
