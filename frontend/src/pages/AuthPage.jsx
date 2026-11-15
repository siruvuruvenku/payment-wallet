import { useState } from "react";
import { ArrowRight, ShieldCheck, WalletCards } from "lucide-react";
import { messageFromError } from "../api.js";

export default function AuthPage({ onAuthenticate }) {
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await onAuthenticate(mode, { email, password });
    } catch (requestError) {
      setError(messageFromError(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-promo">
        <a className="brand auth-brand" href="/"><span className="brand-mark"><WalletCards size={21} /></span><span>orbit<span className="brand-dot">.</span></span></a>
        <div className="promo-copy">
          <div className="eyebrow"><span className="eyebrow-line" /> A clearer way to move money</div>
          <h1>Your money,<br /><em>in good orbit.</em></h1>
          <p>One place to see your balance, move money, and keep every transaction in view.</p>
        </div>
        <div className="promo-footer"><ShieldCheck size={17} /> Protected access for your wallet</div>
        <div className="promo-orbit orbit-one" /><div className="promo-orbit orbit-two" />
      </section>
      <section className="auth-form-wrap">
        <form className="auth-form" onSubmit={submit}>
          <div className="form-kicker">WELCOME {mode === "login" ? "BACK" : "TO ORBIT"}</div>
          <h2>{mode === "login" ? "Sign in to your wallet" : "Create your account"}</h2>
          <p className="form-subtitle">{mode === "login" ? "Enter your details to continue." : "It takes just a moment to get started."}</p>
          {error && <div className="form-error" role="alert">{error}</div>}
          <label className="field-label" htmlFor="email">Email address</label>
          <input id="email" type="email" autoComplete="email" placeholder="you@example.com" value={email} onChange={(event) => setEmail(event.target.value)} required maxLength={254} />
          <label className="field-label" htmlFor="password">Password</label>
          <input id="password" type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} placeholder="At least 12 characters" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={mode === "login" ? 1 : 12} maxLength={72} />
          <button className="primary-button auth-submit" disabled={busy}>{busy ? "Please wait…" : mode === "login" ? "Sign in" : "Create account"} <ArrowRight size={17} /></button>
          <div className="auth-switch">{mode === "login" ? "New to Orbit?" : "Already have an account?"} <button type="button" onClick={() => { setError(""); setMode(mode === "login" ? "register" : "login"); }}>{mode === "login" ? "Create an account" : "Sign in"}</button></div>
          <div className="auth-note"><ShieldCheck size={15} /> Your password is securely hashed before storage.</div>
        </form>
      </section>
    </main>
  );
}
