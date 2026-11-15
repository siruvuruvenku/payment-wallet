import { useCallback, useEffect, useState } from "react";
import { ArrowUpRight, Plus, WalletCards } from "lucide-react";
import { Link } from "react-router-dom";
import { api, messageFromError } from "../api.js";
import { ErrorState, Loading } from "../components/Feedback.jsx";
import { formatMoney } from "../components/TransactionTable.jsx";

export default function WalletsPage() {
  const [wallets, setWallets] = useState(null);
  const [currency, setCurrency] = useState("USD");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const load = useCallback(async () => {
    try { setError(""); setWallets((await api.get("/wallets")).data); }
    catch (requestError) { setError(messageFromError(requestError)); }
  }, []);
  useEffect(() => { load(); }, [load]);
  async function createWallet(event) {
    event.preventDefault(); setBusy(true); setError("");
    try { await api.post("/wallets", { currency }); await load(); }
    catch (requestError) { setError(messageFromError(requestError)); }
    finally { setBusy(false); }
  }
  if (error && !wallets) return <ErrorState message={error} onRetry={load} />;
  if (!wallets) return <Loading />;
  return (
    <div className="page-stack">
      <div className="page-heading"><div><div className="page-kicker">YOUR MONEY, ORGANIZED</div><h1>My wallets</h1><p>See your available balances and account activity.</p></div></div>
      {error && <ErrorState message={error} />}
      <section className="wallet-grid">
        {wallets.map((wallet, index) => (
          <article className="wallet-card" key={wallet.walletId}>
            <div className="wallet-card-top"><span className={`wallet-symbol symbol-${index % 3}`}><WalletCards size={20} /></span><span className="status-badge"><i />{wallet.status.toLowerCase()}</span></div>
            <div className="wallet-currency">{wallet.currency} wallet</div>
            <strong className="wallet-balance">{formatMoney(wallet.balance, wallet.currency)}</strong>
            <div className="wallet-card-bottom"><code>{wallet.walletId}</code><Link to={`/ledger/${wallet.walletId}`} aria-label="View ledger"><ArrowUpRight size={16} /></Link></div>
          </article>
        ))}
      </section>
      <section className="section-card create-wallet-card">
        <div><h2>Create a wallet</h2><p>Add a wallet balance in a supported three-letter currency.</p></div>
        <form className="inline-form" onSubmit={createWallet}><label className="sr-only" htmlFor="currency">Currency</label><input id="currency" value={currency} onChange={(event) => setCurrency(event.target.value.toUpperCase().slice(0, 3))} minLength="3" maxLength="3" pattern="[A-Z]{3}" required /><button className="primary-button" disabled={busy}><Plus size={16} /> {busy ? "Creating…" : "Add wallet"}</button></form>
      </section>
    </div>
  );
}
