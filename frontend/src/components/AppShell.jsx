import { NavLink, Outlet, useLocation } from "react-router-dom";
import { ArrowDownLeft, ArrowLeftRight, ArrowUpRight, Bell, CircleHelp, LayoutDashboard, LogOut, Wallet, WalletCards } from "lucide-react";

const links = [
  { to: "/", label: "Overview", icon: LayoutDashboard, end: true },
  { to: "/wallets", label: "My wallets", icon: Wallet },
  { to: "/transfer", label: "Send money", icon: ArrowUpRight },
  { to: "/history", label: "Transactions", icon: ArrowLeftRight },
];

export default function AppShell({ user, onSignOut }) {
  const location = useLocation();
  const title = links.find((link) => link.to === location.pathname)?.label || "Wallet details";
  return (
    <div className="app-frame">
      <aside className="sidebar">
        <a className="brand" href="/" aria-label="Orbit Wallet home">
          <span className="brand-mark"><WalletCards size={21} /></span>
          <span>orbit<span className="brand-dot">.</span></span>
        </a>
        <div className="workspace-label">WORKSPACE</div>
        <nav className="side-nav">
          {links.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
              <Icon size={18} strokeWidth={1.8} /><span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="support-card">
            <div className="support-icon"><CircleHelp size={17} /></div>
            <strong>Need a hand?</strong>
            <span>We’re here to help you.</span>
          </div>
          <button className="profile-button" onClick={onSignOut}>
            <span className="avatar">{(user.email || "U").slice(0, 1).toUpperCase()}</span>
            <span className="profile-copy"><strong>{user.email}</strong><small>Personal account</small></span>
            <LogOut size={16} />
          </button>
        </div>
      </aside>
      <main className="main-panel">
        <header className="topbar">
          <div className="mobile-brand"><span className="brand-mark"><Wallet size={19} /></span> orbit<span className="brand-dot">.</span></div>
          <div className="breadcrumb"><span>Workspace</span><span className="crumb-slash">/</span><strong>{title}</strong></div>
          <div className="topbar-actions"><span className="secure-pill"><span /> Secure connection</span><button className="icon-button" aria-label="Notifications"><Bell size={18} /></button></div>
        </header>
        <div className="content"><Outlet /></div>
      </main>
      <nav className="mobile-nav">
        {links.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end} className={({ isActive }) => `mobile-nav-link ${isActive ? "active" : ""}`}>
            <Icon size={19} /><span>{label === "Overview" ? "Home" : label.split(" ")[0]}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
