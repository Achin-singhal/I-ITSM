import React from "react";
import { cn } from "../../lib/utils";

export function Badge({ className, variant = "default", children, ...props }) {
  const variants = {
    default: "bg-blue-600/20 text-blue-400 border-blue-500/30",
    success: "bg-emerald-600/20 text-emerald-400 border-emerald-500/30",
    warning: "bg-amber-600/20 text-amber-400 border-amber-500/30",
    destructive: "bg-rose-600/20 text-rose-400 border-rose-500/30",
    outline: "text-slate-400 border-slate-700",
  };

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors",
        variants[variant] || variants.default,
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
}
