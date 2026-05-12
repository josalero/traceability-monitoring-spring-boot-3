import { useEffect, useId, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getOrder, listOrders, type OrderResponse } from "../lib/api";
import { orderStatusBadgeClasses } from "../lib/order-status-styles";

function formatWhen(iso: string) {
  try {
    return new Date(iso).toLocaleString(undefined, {
      dateStyle: "short",
      timeStyle: "medium",
    });
  } catch {
    return iso;
  }
}

type OrderDetailDialogProps = {
  orderId: string | null;
  listCreatedAt: string | undefined;
  /** Status from the polled list row — used to invalidate stale detail cache when saga completes */
  listRowStatus: OrderResponse["status"] | undefined;
  onClose: () => void;
};

function OrderDetailDialog({ orderId, listCreatedAt, listRowStatus, onClose }: OrderDetailDialogProps) {
  const titleId = useId();
  const queryClient = useQueryClient();
  const detail = useQuery({
    queryKey: ["orders", "detail", orderId],
    queryFn: () => getOrder(orderId!),
    enabled: Boolean(orderId),
    staleTime: 0,
    gcTime: 120_000,
    refetchOnMount: "always",
    refetchInterval: (q) => {
      const st = q.state.data?.status;
      if (st === "CONFIRMED" || st === "FAILED") return false;
      return 2000;
    },
  });

  useEffect(() => {
    if (!orderId || listRowStatus === undefined) return;
    void queryClient.invalidateQueries({ queryKey: ["orders", "detail", orderId] });
  }, [orderId, listRowStatus, queryClient]);

  const statusMismatch =
    detail.isSuccess &&
    detail.data &&
    listRowStatus !== undefined &&
    detail.data.status !== listRowStatus;

  useEffect(() => {
    if (!orderId) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [orderId, onClose]);

  if (!orderId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <button
        type="button"
        className="absolute inset-0 z-0 bg-slate-900/40 backdrop-blur-[1px]"
        aria-label="Close order details"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="relative z-10 w-full max-w-lg rounded-t-xl border border-slate-200 bg-white shadow-xl sm:rounded-xl"
      >
        <div className="flex items-start justify-between gap-3 border-b border-slate-100 px-5 py-4">
          <div>
            <h3 id={titleId} className="text-lg font-semibold text-slate-900">
              Order details
            </h3>
            <p className="mt-1 text-sm text-slate-600">
              Live lookup via <code className="rounded bg-slate-100 px-1 text-xs">GET /orders/{"{id}"}</code> — order-service
              may serve from Redis cache or Postgres.
            </p>
          </div>
          <button
            type="button"
            className="shrink-0 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-800 hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900"
            onClick={onClose}
          >
            Close
          </button>
        </div>

        <div className="space-y-4 px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Order ID</p>
            <p className="mt-1 break-all font-mono text-sm text-slate-900">{orderId}</p>
          </div>

          {listCreatedAt && (
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Created (from list)
              </p>
              <p className="mt-1 text-sm text-slate-700">{formatWhen(listCreatedAt)}</p>
            </div>
          )}

          {detail.isLoading && (
            <p className="text-sm text-slate-600" role="status">
              Loading latest status…
            </p>
          )}
          {detail.isError && (
            <div
              className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-900"
              role="alert"
            >
              Could not load order details. Check the gateway and order-service.
            </div>
          )}
          {statusMismatch && (
            <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-950" role="status">
              Table shows <span className="font-semibold">{listRowStatus}</span>; detail was stale — refreshing…
            </p>
          )}
          {detail.isSuccess && detail.data && (
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Current status
              </p>
              <p className="mt-2">
                <span
                  className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${orderStatusBadgeClasses(detail.data.status)}`}
                >
                  {detail.data.status}
                </span>
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function OrdersDashboard() {
  const limit = 20;
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selectedCreatedAt, setSelectedCreatedAt] = useState<string | undefined>();

  const orders = useQuery({
    queryKey: ["orders", "list", limit],
    queryFn: () => listOrders(limit),
    staleTime: 0,
    /** Avoid reusing nested objects when status strings change (browser cache / reconciliation edge cases). */
    structuralSharing: false,
    refetchOnWindowFocus: true,
    refetchInterval: (q) =>
      q.state.data?.some((o) => o.status === "PENDING") ? 2000 : 12_000,
  });

  function openDetail(row: { id: string; createdAt: string }) {
    setSelectedId(row.id);
    setSelectedCreatedAt(row.createdAt);
  }

  function closeDetail() {
    setSelectedId(null);
    setSelectedCreatedAt(undefined);
  }

  return (
    <section
      className="rounded-xl border border-slate-200 bg-white shadow-sm"
      aria-labelledby="orders-heading"
    >
      <OrderDetailDialog
        orderId={selectedId}
        listCreatedAt={selectedCreatedAt}
        listRowStatus={
          selectedId && orders.data ? orders.data.find((o) => o.id === selectedId)?.status : undefined
        }
        onClose={closeDetail}
      />

      <div className="flex flex-col gap-4 border-b border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 id="orders-heading" className="text-lg font-semibold text-slate-900">
            Recent orders
          </h2>
          <p className="text-sm text-slate-600">
            Showing the latest {limit} orders (newest first). Click a row for live details (cache / DB). Rows refresh
            automatically while any order is still pending.
          </p>
        </div>
        <button
          type="button"
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-sm hover:bg-slate-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900 disabled:opacity-50"
          disabled={orders.isFetching}
          onClick={() => orders.refetch()}
        >
          {orders.isFetching ? "Refreshing…" : "Refresh"}
        </button>
      </div>

      <div className="px-2 pb-4 pt-2">
        {orders.isLoading && (
          <p className="px-3 py-8 text-center text-sm text-slate-600" role="status">
            Loading orders…
          </p>
        )}
        {orders.isError && (
          <div className="px-3 py-6 text-center">
            <p className="text-sm text-red-700">Could not load orders.</p>
            <button
              type="button"
              className="mt-3 rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-900"
              onClick={() => orders.refetch()}
            >
              Try again
            </button>
          </div>
        )}
        {orders.isSuccess && orders.data.length === 0 && (
          <p className="px-3 py-8 text-center text-sm text-slate-600">
            No orders yet. Place one from checkout to populate this table.
          </p>
        )}
        {orders.isSuccess && orders.data.length > 0 && (
          <div
            className="mx-2 max-h-[min(70vh,28rem)] overflow-x-auto overflow-y-auto rounded-lg border border-slate-200 shadow-inner"
            tabIndex={0}
            role="region"
            aria-label="Order list, scroll vertically for all rows. Activate a row to open details."
          >
            <table className="min-w-full text-left text-sm text-slate-800">
              <caption className="sr-only">
                Order identifiers, statuses, and creation times. Select a row to open details.
              </caption>
              <thead className="sticky top-0 z-10 border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-600 shadow-sm">
                <tr>
                  <th scope="col" className="px-3 py-3">
                    Order ID
                  </th>
                  <th scope="col" className="px-3 py-3">
                    Status
                  </th>
                  <th scope="col" className="px-3 py-3">
                    Created
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.data.map((row) => {
                  const isFailed = row.status === "FAILED";
                  const selected = selectedId === row.id;
                  return (
                    <tr
                      key={row.id}
                      tabIndex={0}
                      role="button"
                      aria-label={`Order ${row.id}, status ${row.status}. Open details.`}
                      aria-pressed={selected}
                      className={`cursor-pointer outline-none ring-inset transition-colors focus-visible:ring-2 focus-visible:ring-slate-900 ${
                        selected ? "ring-2 ring-slate-500" : ""
                      } ${isFailed ? "bg-red-50/70 hover:bg-red-50" : "even:bg-slate-50/80 hover:bg-slate-100/80"}`}
                      onClick={() => openDetail(row)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          openDetail(row);
                        }
                      }}
                    >
                      <td className="px-3 py-3 font-mono text-xs text-slate-900 sm:text-sm">{row.id}</td>
                      <td className="px-3 py-3">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${orderStatusBadgeClasses(row.status)}`}
                        >
                          {row.status}
                        </span>
                      </td>
                      <td className="px-3 py-3 whitespace-nowrap text-slate-700">{formatWhen(row.createdAt)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}
