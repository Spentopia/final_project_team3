"use client";

import { useTheme } from "next-themes";
import { Toaster as Sonner, ToasterProps } from "sonner";
import styles from "./sonner.module.css";

const Toaster = ({ ...props }: ToasterProps) => {
  const { theme = "system" } = useTheme();

  return (
    <Sonner
      theme={theme as ToasterProps["theme"]}
      className={`toaster group ${styles.toaster}`}
      position="top-right"
      richColors
      closeButton
      duration={4000}
      {...props}
    />
  );
};

export { Toaster };
