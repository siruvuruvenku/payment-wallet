import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1",
  timeout: 10000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("wallet.token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("wallet.token");
      localStorage.removeItem("wallet.user");
      window.dispatchEvent(new Event("wallet:unauthorized"));
    }
    return Promise.reject(error);
  },
);

export function messageFromError(error) {
  return error.response?.data?.message || error.message || "Something went wrong. Please try again.";
}
