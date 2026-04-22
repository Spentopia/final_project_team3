import React from 'react';
import ReactDOM from "react-dom/client";
import { Buffer } from "buffer";
import App from "@/app/App";
import './styles/index.css';

if (typeof window !== "undefined" && !window.Buffer) {
  window.Buffer = Buffer;
}

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <React.StrictMode>
        <App/>
    </React.StrictMode>
);
