"use client";

import { type ReactNode, useState } from "react";

export default function EditorPanel({ title, hint, children, initiallyOpen = false }: {
  title: string; hint?: string; children: ReactNode; initiallyOpen?: boolean;
}) {
  const [visited, setVisited] = useState(initiallyOpen);
  const [open, setOpen] = useState(initiallyOpen);
  return (
    <details className="creator-panel" open={open}
      onToggle={(event) => {
        setOpen(event.currentTarget.open);
        if (event.currentTarget.open) setVisited(true);
      }}>
      <summary><span>{title}</span>{hint ? <small>{hint}</small> : null}</summary>
      <div className="creator-panel-body">{visited ? children : null}</div>
    </details>
  );
}
