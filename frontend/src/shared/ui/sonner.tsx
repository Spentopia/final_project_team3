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
      expand
      visibleToasts={4}
      duration={4000}
      gap={8}
      offset={20}
      {...props}
    />
  );
};

export { Toaster };
