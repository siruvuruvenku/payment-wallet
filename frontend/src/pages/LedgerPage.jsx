import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { api, messageFromError } from "../api.js";
import { ErrorState, Loading } from "../components/Feedback.jsx";
import { formatMoney } from "../components/TransactionTable.jsx";

export default function LedgerPage() {
  const { walletId } = useParams();
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => { api.get(`/wallets/${walletId}/ledger`).then(({ data }) => setResult(data)).catch((reason) => setError(messageFromError(reason))); }, [walletId]);
  if (error) return <ErrorState message={error} />;
  if (!result) return <Loading />;
  return (
    <div className="page-stack">
      <div className="page-heading"><div><div className="page-kicker">FINANCIAL RECORDS</div><h1>Wallet ledger</h1><p>Immutable debit and credit entries for this wallet.</p></div><Link to="/wallets" className="subtle-link"><ArrowLeft size={15} /> My wallets</Link></div>
      <section className="section-card"><div className="section-heading"><div><h2>Ledger entries</h2><p>{result.totalElements} entries · entries are not editable</p></div></div>
        {!result.content.length ? <div className="table-empty">No entries have been posted to this wallet.</div> : <div className="ledger-list">{result.content.map((entry) => <article className="ledger-row" key={entry.entryId}><span className={`transaction-icon ${entry.type === "DEBIT" ? "sent" : "received"}`}>{entry.type === "DEBIT" ? "−" : "+"}</span><div><strong>{entry.type} entry</strong><small>{entry.createdAt ? new Date(entry.createdAt).toLocaleString() : entry.transferId}</small><code>{entry.transferId}</code></div><strong className={entry.type === "CREDIT" ? "positive" : ""}>{entry.type === "DEBIT" ? "−" : "+"}{formatMoney(entry.amount, entry.currency)}</strong></article>)}</div>}
      </section>
    </div>
  );
}
