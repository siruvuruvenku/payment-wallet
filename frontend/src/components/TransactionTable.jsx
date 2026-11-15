import { Link } from "react-router-dom";
import { ArrowDownLeft, ArrowUpRight, MoreHorizontal } from "lucide-react";

export function formatMoney(amount, currency = "USD") {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(Number(amount || 0));
}

export function TransactionTable({ rows = [], ownedWalletIds = [] }) {
  if (!rows.length) return <div className="table-empty">Your completed transfers will show up here.</div>;
  return (
    <div className="table-scroll">
      <table className="transactions-table">
        <thead><tr><th>Transaction</th><th>Date</th><th>Amount</th><th>Status</th><th /></tr></thead>
        <tbody>
          {rows.map((row) => {
            const isSent = ownedWalletIds.includes(row.sourceWalletId);
            return (
              <tr key={row.transferId}>
                <td><div className="transaction-name"><span className={`transaction-icon ${isSent ? "sent" : "received"}`}>{isSent ? <ArrowUpRight size={16} /> : <ArrowDownLeft size={16} />}</span><span><strong>{isSent ? "Money sent" : "Money received"}</strong><small>{row.transferId.slice(0, 8)}…</small></span></div></td>
                <td className="muted-cell">{row.createdAt ? new Date(row.createdAt).toLocaleDateString() : "—"}</td>
                <td className={`amount-cell ${isSent ? "" : "positive"}`}>{isSent ? "−" : "+"}{formatMoney(row.amount, row.currency)}</td>
                <td><span className="status-badge"><i />{row.status.toLowerCase()}</span></td>
                <td><Link className="row-action" aria-label="View transfer" to={`/transactions/${row.transferId}`}><MoreHorizontal size={19} /></Link></td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
