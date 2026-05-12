import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  timeout: 8000,
  headers: { "Content-Type": "application/json" },
});

export type PlaceOrderRequest = {
  customerEmail: string;
  lines: { sku: string; quantity: number; unitPrice: number }[];
};

export type OrderResponse = { id: string; status: "PENDING" | "CONFIRMED" | "FAILED" };

export type OrderSummary = OrderResponse & { createdAt: string };

export async function placeOrder(req: PlaceOrderRequest) {
  const { data } = await api.post<OrderResponse>("/api/v1/orders", req);
  return data;
}

export async function listOrders(limit = 20) {
  const { data } = await api.get<OrderSummary[]>("/api/v1/orders", {
    params: { limit },
    // Browsers may heuristic-cache GETs without strong Cache-Control; belt-and-suspenders with order-service headers.
    headers: { "Cache-Control": "no-cache", Pragma: "no-cache" },
  });
  return data;
}

export async function getOrder(id: string) {
  const { data } = await api.get<OrderResponse>(`/api/v1/orders/${id}`, {
    headers: { "Cache-Control": "no-cache", Pragma: "no-cache" },
  });
  return data;
}
