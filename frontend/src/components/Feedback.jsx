import { AlertCircle, LoaderCircle } from "lucide-react";

export function Loading({ label = "Loading your account…" }) {
  return <div className="state-card"><LoaderCircle className="spin" size={22} /><span>{label}</span></div>;
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="error-card">
      <AlertCircle size={19} />
      <span>{message}</span>
      {onRetry && <button className="text-button" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function EmptyState({ title, detail }) {
  return <div className="empty-state"><div className="empty-icon">↗</div><strong>{title}</strong><span>{detail}</span></div>;
}
