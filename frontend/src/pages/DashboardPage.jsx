import { useCallback, useEffect, useState } from "react";
import { ArrowDownLeft, ArrowUpRight, ArrowUpRight as ArrowUpRightIcon, Plus, WalletCards } from "lucide-react";
import { Link } from "react-router-dom";
import { api, messageFromError } from "../api.js";
import { ErrorState, Loading } from "../components/Feedback.jsx";
import { formatMoney, TransactionTable } from "../components/TransactionTable.jsx";

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setError("");
    try {
      const [walletResponse, transferResponse] = await Promise.all([
        api.get("/wallets"),
        api.get("/transfers", { params: { page: 0, size: 6 } }),
      ]);
      const wallets = walletResponse.data;
      setData({ wallets, transfers: transferResponse.data.content || [] });
    } catch (requestError) {
      setError(messageFromError(requestError));
    }
  }, []);
  useEffect(() => { load(); }, [load]);
  if (error) return <ErrorState message={error} onRetry={load} />;
  if (!data) return <Loading />;

  const wallet = data.wallets[0];
  const ownedWalletIds = data.wallets.map((item) => item.walletId);
  const sent = data.transfers.filter((row) => ownedWalletIds.includes(row.sourceWalletId)).reduce((sum, row) => sum + Number(row.amount), 0);
  const received = data.transfers.filter((row) => ownedWalletIds.includes(row.destinationWalletId)).reduce((sum, row) => sum + Number(row.amount), 0);
  const successful = data.transfers.filter((row) => row.status === "COMPLETED").length;
  return (
    <div className="page-stack">
      <div className="page-heading"><div><div className="page-kicker">YOUR FINANCES AT A GLANCE</div><h1>Good to see you.</h1><p>Here’s what’s happening with your money today.</p></div><Link to="/transfer" className="primary-button"><ArrowUpRightIcon size={17} /> Send money</Link></div>
      <section className="balance-card">
        <div className="balance-top"><span><span className="balance-icon"><WalletCards size={17} /></span> Total wallet balance</span><span className="live-indicator"><i /> LIVE BALANCE</span></div>
        <div className="balance-amount">{formatMoney(data.wallets.reduce((sum, item) => sum + Number(item.balance), 0), wallet?.currency || "USD")}</div>
        <div className="balance-bottom"><span>Across {data.wallets.length} {data.wallets.length === 1 ? "wallet" : "wallets"}</span><Link to="/wallets">View wallets <ArrowUpRightIcon size={14} /></Link></div>
        <div className="balance-glow" />
      </section>
      <section className="metric-grid">
        <article className="metric-card"><span className="metric-icon sent"><ArrowUpRight size={17} /></span><span className="metric-label">Money sent <small>recent activity</small></span><strong>{formatMoney(sent, wallet?.currency || "USD")}</strong></article>
        <article className="metric-card"><span className="metric-icon received"><ArrowDownLeft size={17} /></span><span className="metric-label">Money received <small>recent activity</small></span><strong>{formatMoney(received, wallet?.currency || "USD")}</strong></article>
        <article className="metric-card"><span className="metric-icon completed"><span>✓</span></span><span className="metric-label">Completed <small>recent activity</small></span><strong>{successful}<small> transfers</small></strong></article>
      </section>
      <section className="section-card">
        <div className="section-heading"><div><h2>Recent transactions</h2><p>Your latest wallet activity</p></div><Link to="/history" className="subtle-link">View all <ArrowUpRightIcon size={15} /></Link></div>
        <TransactionTable rows={data.transfers} ownedWalletIds={ownedWalletIds} />
      </section>
      <Link to="/wallets" className="add-wallet-banner"><span className="add-wallet-icon"><Plus size={19} /></span><span><strong>Manage your wallets</strong><small>Create and review your currency balances</small></span><ArrowUpRightIcon size={17} /></Link>
    </div>
  );
}
