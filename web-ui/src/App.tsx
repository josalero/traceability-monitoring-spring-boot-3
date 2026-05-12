import CheckoutPage from "./pages/CheckoutPage";
import OrdersDashboard from "./pages/OrdersDashboard";

function App() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-col gap-1 px-4 py-5 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Commerce observability POC
            </p>
            <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
              Orders console
            </h1>
          </div>
          <p className="max-w-md text-sm text-slate-600">
            Place demo orders and watch statuses update. Correlate with Jaeger using response tracing
            headers from the gateway.
          </p>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8">
        <div className="grid gap-8 lg:grid-cols-5 lg:items-start">
          <div className="lg:col-span-2">
            <CheckoutPage />
          </div>
          <div className="lg:col-span-3">
            <OrdersDashboard />
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
