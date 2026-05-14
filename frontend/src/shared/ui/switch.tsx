"use client";

import * as React from "react";
import * as SwitchPrimitive from "@radix-ui/react-switch";

import { cn } from "./utils";

function Switch({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      data-slot="switch"
      className={cn(
        "peer inline-flex h-[1.15rem] w-8 shrink-0 items-center rounded-full border transition-all outline-none focus-visible:ring-[3px] focus-visible:border-ring focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:border-blue-400 data-[state=checked]:bg-blue-600 data-[state=unchecked]:border-blue-200 data-[state=unchecked]:bg-blue-100 dark:data-[state=checked]:border-violet-300/60 dark:data-[state=checked]:bg-violet-400 dark:data-[state=unchecked]:border-slate-500 dark:data-[state=unchecked]:bg-slate-800",
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        data-slot="switch-thumb"
        className={cn(
          "pointer-events-none block size-4 rounded-full bg-white shadow-sm ring-0 transition-transform data-[state=checked]:translate-x-[calc(100%-2px)] data-[state=unchecked]:translate-x-0 dark:data-[state=checked]:bg-white dark:data-[state=unchecked]:bg-slate-200",
        )}
      />
    </SwitchPrimitive.Root>
  );
}

export { Switch };
