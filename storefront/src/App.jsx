import React, { useEffect, useState } from "react";

// The browser calls these paths. The storefront's nginx forwards them to the
// Catalog and Orders services inside the cluster (see nginx.conf), so the
// frontend never needs to know the backends' real addresses.
const CATALOG_URL = "/api/catalog/products";
const ORDERS_URL = "/api/orders";

export default function App() {
  const [products, setProducts] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(CATALOG_URL)
      .then((res) => res.json())
      .then((data) => setProducts(data))
      .catch(() => setMessage("Could not load products. Is the Catalog service running?"))
      .finally(() => setLoading(false));
  }, []);

  async function placeOrder(productId) {
    setMessage("");
    try {
      const res = await fetch(ORDERS_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ productId, quantity: 1 })
      });
      if (res.ok) {
        const order = await res.json();
        setMessage("Order placed. Order id: " + order.id);
      } else {
        setMessage("Order failed: " + (await res.text()));
      }
    } catch (e) {
      setMessage("Order failed. Is the Orders service running?");
    }
  }

  return (
    <div style={{ fontFamily: "system-ui, sans-serif", maxWidth: 640, margin: "40px auto", padding: "0 16px" }}>
      <h1 style={{ color: "#15577d" }}>ShopFlow</h1>
      <p style={{ color: "#475569" }}>A small store that talks to real backend services.</p>

      {message && (
        <div style={{ background: "#eef4f8", border: "1px solid #cdd6dd", borderRadius: 8, padding: "10px 14px", margin: "16px 0" }}>
          {message}
        </div>
      )}

      {loading ? (
        <p>Loading products...</p>
      ) : (
        <ul style={{ listStyle: "none", padding: 0 }}>
          {products.map((p) => (
            <li key={p.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", border: "1px solid #e2e8f0", borderRadius: 8, padding: "12px 16px", marginBottom: 10 }}>
              <span>
                <strong>{p.name}</strong>
                <span style={{ color: "#475569" }}> &mdash; ${p.price.toFixed(2)} ({p.stock} in stock)</span>
              </span>
              <button onClick={() => placeOrder(p.id)} style={{ background: "#15577d", color: "#fff", border: "none", borderRadius: 6, padding: "8px 14px", cursor: "pointer" }}>
                Buy
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
