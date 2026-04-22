declare module "react-dom/client";

declare global {
  interface Window {
    Buffer: typeof import("buffer").Buffer;
  }
}

