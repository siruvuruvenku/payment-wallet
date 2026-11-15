import { useEffect, useState } from "react";
import { ArrowLeft, ArrowUpRight, CheckCircle2, Copy, ShieldCheck } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { api, messageFromError } from "../api.js";
import { ErrorState, Loading } from "../components/Feedback.jsx";
import { formatMoney } from "../components/TransactionTable.jsx";

export default function TransactionPage() {
  const { transferId } = useParams();
  const [transfer, setTransfer] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => { api.get(`/transfers/${transferId}`).then(({ data }) => setTransfer(data)).catch((reason) => setError(messageFromError(reason))); }, [transferId]);
  if (error) return <ErrorState message={error} />;
  if (!transfer) return <Loading />;
  return (
    <div className="page-stack">
      <div className="page-heading"><div><div className="page-kicker">TRANSFER RECEIPT</div><h1>Transaction details</h1><p>Review the recorded transfer information.</p></div><Link to="/history" className="subtle-link"><ArrowLeft size={15} /> All transactions</Link></div>
      <section className="receipt-card">
        <div className="receipt-status"><span className="receipt-check"><CheckCircle2 size={24} /></span><span className="status-badge"><i />{transfer.status.toLowerCase()}</span></div>
        <div className="receipt-amount">{formatMoney(transfer.amount, transfer.currency)}</div><p className="receipt-caption">Wallet transfer</p>
        <div className="receipt-divider" />
        <dl className="receipt-details">
          <div><dt>Transfer ID</dt><dd><code>{transfer.transferId}</code><button className="copy-button" onClick={() => navigator.clipboard.writeText(transfer.transferId)} aria-label="Copy transfer ID"><Copy size={14} /></button></dd></div>
          <div><dt>From wallet</dt><dd><code>{transfer.sourceWalletId}</code></dd></div>
          <div><dt>To wallet</dt><dd><code>{transfer.destinationWalletId}</code></dd></div>
          <div><dt>Created</dt><dd>{transfer.createdAt ? new Date(transfer.createdAt).toLocaleString() : "Recorded"}</dd></div>
          <div><dt>Currency</dt><dd>{transfer.currency}</dd></div>
        </dl>
        <div className="receipt-note"><ShieldCheck size={16} /> Stored as an atomic, balanced ledger transfer.</div>
      </section>
      <Link to="/wallets" className="subtle-link"><ArrowUpRight size={15} /> Explore wallet ledger</Link>
    </div>
  );
}
