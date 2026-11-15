import { useCallback, useEffect, useState } from "react";
import { ArrowUpRight } from "lucide-react";
import { api, messageFromError } from "../api.js";
import { ErrorState, Loading } from "../components/Feedback.jsx";
import { TransactionTable } from "../components/TransactionTable.jsx";

export default function HistoryPage() {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [walletIds, setWalletIds] = useState([]);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    try {
      setError("");
      const [transferResponse, walletResponse] = await Promise.all([
        api.get("/transfers", { params: { page, size: 20 } }),
        api.get("/wallets"),
      ]);
      setResult(transferResponse.data);
      setWalletIds(walletResponse.data.map((wallet) => wallet.walletId));
    }
    catch (requestError) { setError(messageFromError(requestError)); }
  }, [page]);
  useEffect(() => { load(); }, [load]);
  if (error && !result) return <ErrorState message={error} onRetry={load} />;
  if (!result) return <Loading />;
  return (
    <div className="page-stack">
      <div className="page-heading"><div><div className="page-kicker">EVERY MOVE, ACCOUNTED FOR</div><h1>Transactions</h1><p>Browse transfers across your wallets.</p></div></div>
      {error && <ErrorState message={error} onRetry={load} />}
      <section className="section-card"><div className="section-heading"><div><h2>Transfer history</h2><p>{result.totalElements} records · page {page + 1} of {Math.max(result.totalPages, 1)}</p></div><span className="secure-pill"><span /> Live records</span></div><TransactionTable rows={result.content} ownedWalletIds={walletIds} />
        <div className="pagination"><button className="secondary-button" onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={page === 0}>Previous</button><span>{page + 1} / {Math.max(result.totalPages, 1)}</span><button className="secondary-button" onClick={() => setPage((value) => value + 1)} disabled={page + 1 >= result.totalPages}>Next <ArrowUpRight size={14} /></button></div>
      </section>
    </div>
  );
}
