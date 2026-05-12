import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { placeOrder, getOrder } from "../lib/api";
import { orderStatusBadgeClasses } from "../lib/order-status-styles";

export default function CheckoutPage() {
  const queryClient = useQueryClient();
  const [orderId, setOrderId] = useState<string | null>(null);

  const place = useMutation({
    mutationFn: placeOrder,
    onSuccess: (res) => {
      setOrderId(res.id);
      queryClient.invalidateQueries({ queryKey: ["orders", "list"] });
    },
  });

  const status = useQuery({
    queryKey: ["order", orderId],
    queryFn: () => getOrder(orderId!),
    enabled: Boolean(orderId),
    refetchInterval: (q) => {
      const st = q.state.data?.status;
      if (st === "CONFIRMED" || st === "FAILED") return false;
      return 1500;
    },
  });

  const latestStatus = status.data?.status;

  useEffect(() => {
    if (!latestStatus) return;
    void queryClient.invalidateQueries({ queryKey: ["orders", "list"] });
  }, [latestStatus, queryClient]);

  return (
    <section
      className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
      aria-labelledby="checkout-heading"
    >
      <h2 id="checkout-heading" className="text-lg font-semibold text-slate-900">
        Checkout
      </h2>
      <p className="mt-1 text-sm text-slate-600">
        Demo checkout uses a fixed basket (SKU-1 × 1). Inventory seeds <strong>10</strong> units of SKU-1 on first
        startup. Payment may randomly decline for tracing demos; failed orders appear below and in{" "}
        <strong>Recent orders</strong> with status <span className="font-medium text-red-900">FAILED</span>.
      </p>

      <div className="mt-5 space-y-3">
        <button
          type="button"
          className="rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900 disabled:opacity-50"
          disabled={place.isPending}
          onClick={() =>
            place.mutate({
              customerEmail: "demo@example.com",
              lines: [{ sku: "SKU-1", quantity: 1, unitPrice: 19.99 }],
            })
          }
        >
          {place.isPending ? "Placing order…" : "Place order"}
        </button>

        {place.isError && (
          <p className="text-sm text-red-700" role="alert">
            Order could not be placed. Check the gateway and order-service, then try again.
          </p>
        )}

        {orderId && (
          <div className="space-y-2 border-t border-slate-100 pt-4">
            <p className="text-sm text-slate-700">
              Latest order{" "}
              <code className="rounded bg-slate-100 px-1 py-0.5 text-xs text-slate-900">{orderId}</code>
              <span className="mx-2 text-slate-400" aria-hidden="true">
                ·
              </span>
              <span className="inline-flex items-center gap-2 align-middle">
                <span className="sr-only">Status:</span>
                <span
                  aria-live="polite"
                  className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${orderStatusBadgeClasses(latestStatus ?? "PENDING")}`}
                >
                  {latestStatus ?? "…"}
                </span>
              </span>
            </p>

            {latestStatus === "FAILED" && (
              <div
                role="alert"
                aria-live="assertive"
                className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-950"
              >
                This order did not complete (demo saga ended in failure — often a simulated payment decline,
                sometimes inventory). Check <strong>Recent orders</strong> and traces in Jaeger for details.
              </div>
            )}

            {latestStatus === "CONFIRMED" && (
              <p className="text-sm text-emerald-900" aria-live="polite">
                Order completed successfully — see <strong>Recent orders</strong> for the full list.
              </p>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
