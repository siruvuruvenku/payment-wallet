import { useEffect, useState } from "react";
import { ArrowDown, ArrowLeft, ArrowRight, CheckCircle2, ShieldCheck } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { api, messageFromError } from "../api.js";
import { ErrorState, Loading } from "../components/Feedback.jsx";
import { formatMoney } from "../components/TransactionTable.jsx";

export default function TransferPage() {
  const [wallets, setWallets] = useState(null);
  const [sourceWalletId, setSourceWalletId] = useState("");
  const [destinationWalletId, setDestinationWalletId] = useState("");
  const [amount, setAmount] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  useEffect(() => {
    api.get("/wallets").then(({ data }) => {
      setWallets(data);
      setSourceWalletId(data[0]?.walletId || "");
    }).catch((requestError) => setError(messageFromError(requestError)));
  }, []);

  async function submit(event) {
    event.preventDefault(); setError("");
    const source = wallets.find((wallet) => wallet.walletId === sourceWalletId);
    setBusy(true);
    try {
      const response = await api.post("/transfers", {
        sourceWalletId, destinationWalletId, amount: Number(amount), currency: source.currency,
      }, { headers: { "Idempotency-Key": crypto.randomUUID() } });
      setResult(response.data);
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally { setBusy(false); }
  }

  if (!wallets && !error) return <Loading />;
  if (error && !wallets) return <ErrorState message={error} />;
  if (result) return (
    <div className="success-panel"><span className="success-mark"><CheckCircle2 size={26} /></span><div className="page-kicker">TRANSFER COMPLETE</div><h1>Money is on its way.</h1><p>{formatMoney(result.amount, result.currency)} was transferred successfully.</p><code>{result.transferId}</code><div className="success-actions"><button className="secondary-button" onClick={() => { setResult(null); setAmount(""); setDestinationWalletId(""); }}>Make another transfer</button><button className="primary-button" onClick={() => navigate(`/transactions/${result.transferId}`)}>View details <ArrowRight size={16} /></button></div></div>
  );

  const source = wallets?.find((wallet) => wallet.walletId === sourceWalletId);
  return (
    <div className="page-stack">
      <div className="page-heading"><div><div className="page-kicker">FAST, CLEAR, SECURE</div><h1>Send money</h1><p>Move funds directly between active Orbit wallets.</p></div><Link to="/history" className="subtle-link"><ArrowLeft size={15} /> Transaction history</Link></div>
      {error && <ErrorState message={error} />}
      <form className="transfer-card" onSubmit={submit}>
        <div className="transfer-card-header"><span className="transfer-header-icon"><ArrowRight size={18} /></span><div><h2>New transfer</h2><p>Transfers are final once confirmed.</p></div></div>
        <label className="field-label" htmlFor="source-wallet">From wallet</label>
        <select id="source-wallet" value={sourceWalletId} onChange={(event) => setSourceWalletId(event.target.value)} required>{wallets?.map((wallet) => <option key={wallet.walletId} value={wallet.walletId}>{wallet.currency} · {formatMoney(wallet.balance, wallet.currency)}</option>)}</select>
        <div className="transfer-arrow"><ArrowDown size={16} /></div>
        <label className="field-label" htmlFor="destination-wallet">Recipient wallet ID</label>
        <input id="destination-wallet" value={destinationWalletId} onChange={(event) => setDestinationWalletId(event.target.value)} placeholder="Paste the recipient wallet UUID" required />
        <label className="field-label" htmlFor="amount">Amount <span className="label-meta">Available {formatMoney(source?.balance, source?.currency)}</span></label>
        <div className="amount-input"><span>{source?.currency || "USD"}</span><input id="amount" type="number" min="0.01" max={source?.balance} step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="0.00" required /></div>
        <div className="transfer-disclaimer"><ShieldCheck size={16} /><span>Your transfer is protected by secure, atomic processing. A unique request key prevents accidental duplicate submissions.</span></div>
        <button className="primary-button transfer-submit" disabled={busy || !wallets?.length}>{busy ? "Processing securely…" : "Review & send"} <ArrowRight size={17} /></button>
      </form>
    </div>
  );
}
