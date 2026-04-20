"use client";

import * as React from "react";
import * as ProgressPrimitive from "@radix-ui/react-progress";

import { cn } from "./utils";
import styles from "./progress.module.css";

function Progress({
  className,
  value,
  ...props
}: React.ComponentProps<typeof ProgressPrimitive.Root>) {
  const progressValue = Math.min(100, Math.max(0, Math.round(value || 0)));

  return (
    <ProgressPrimitive.Root
      data-slot="progress"
      className={cn(
        "bg-primary/20 relative h-2 w-full overflow-hidden rounded-full",
        className,
      )}
      {...props}
    >
      <ProgressPrimitive.Indicator
        data-slot="progress-indicator"
        data-progress-value={progressValue}
        className={cn(
          "bg-primary h-full w-full flex-1 transition-all",
          styles.indicator,
        )}
      />
    </ProgressPrimitive.Root>
  );
}

export { Progress };
