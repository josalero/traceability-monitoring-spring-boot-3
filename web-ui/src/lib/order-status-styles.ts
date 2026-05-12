/** Tailwind classes for order status pills (checkout + orders table). */
export function orderStatusBadgeClasses(status: string): string {
  switch (status) {
    case "CONFIRMED":
      return "bg-emerald-50 text-emerald-900 ring-emerald-700/15";
    case "FAILED":
      return "bg-red-50 text-red-900 ring-red-700/15";
    default:
      return "bg-amber-50 text-amber-950 ring-amber-700/15";
  }
}
