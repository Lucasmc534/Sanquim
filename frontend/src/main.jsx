import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./style.css";

// O componente raiz concentra a sessão e a navegação do portal.
createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
