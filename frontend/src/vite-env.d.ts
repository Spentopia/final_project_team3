/// <reference types="vite/client" />


interface ImportMetaEnv {
    readonly VITE_UNITY_LAUNCHER_URL?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}