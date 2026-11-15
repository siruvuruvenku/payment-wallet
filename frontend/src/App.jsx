import { useEffect, useState } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { api } from "./api.js";
import AppShell from "./components/AppShell.jsx";
import AuthPage from "./pages/AuthPage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import HistoryPage from "./pages/HistoryPage.jsx";
import LedgerPage from "./pages/LedgerPage.jsx";
import TransferPage from "./pages/TransferPage.jsx";
import WalletsPage from "./pages/WalletsPage.jsx";
import TransactionPage from "./pages/TransactionPage.jsx";

export default function App() {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("wallet.user") || "null");
    } catch {
      return null;
    }
  });
  const navigate = useNavigate();

  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
      navigate("/login");
    };
    window.addEventListener("wallet:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("wallet:unauthorized", handleUnauthorized);
  }, [navigate]);

  const authenticate = async (mode, values) => {
    const response = await api.post(`/auth/${mode}`, values);
    localStorage.setItem("wallet.token", response.data.accessToken);
    localStorage.setItem("wallet.user", JSON.stringify(response.data));
    setUser(response.data);
  };

  const signOut = () => {
    localStorage.removeItem("wallet.token");
    localStorage.removeItem("wallet.user");
    setUser(null);
  };

  return (
    <Routes>
      <Route
        path="/login"
        element={user ? <Navigate to="/" replace /> : <AuthPage onAuthenticate={authenticate} />}
      />
      <Route
        path="/"
        element={user ? <AppShell user={user} onSignOut={signOut} /> : <Navigate to="/login" replace />}
      >
        <Route index element={<DashboardPage />} />
        <Route path="wallets" element={<WalletsPage />} />
        <Route path="transfer" element={<TransferPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="transactions/:transferId" element={<TransactionPage />} />
        <Route path="ledger/:walletId" element={<LedgerPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
