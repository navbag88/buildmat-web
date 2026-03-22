package com.buildmat.dao;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class ReportQueryService {
    private final EntityManager em;

    public Map<String,Object> salesSummary(String from, String to) {
        List<?> rows = em.createNativeQuery("""
            SELECT strftime('%Y-%m', invoice_date) as period,
                   COUNT(*) as invoice_count,
                   SUM(subtotal) as subtotal,
                   SUM(CASE WHEN include_gst=1 THEN sgst_amount ELSE 0 END) as sgst,
                   SUM(CASE WHEN include_gst=1 THEN cgst_amount ELSE 0 END) as cgst,
                   SUM(CASE WHEN include_gst=1 THEN tax_amount ELSE 0 END) as total_gst,
                   SUM(total_amount) as total_amount,
                   SUM(paid_amount) as paid_amount,
                   SUM(total_amount - paid_amount) as outstanding
            FROM invoices WHERE invoice_date BETWEEN ?1 AND ?2
            GROUP BY period ORDER BY period""")
            .setParameter(1, from).setParameter(2, to).getResultList();
        List<?> totals = em.createNativeQuery("""
            SELECT COUNT(*),SUM(subtotal),
                   SUM(CASE WHEN include_gst=1 THEN sgst_amount ELSE 0 END),
                   SUM(CASE WHEN include_gst=1 THEN cgst_amount ELSE 0 END),
                   SUM(CASE WHEN include_gst=1 THEN tax_amount ELSE 0 END),
                   SUM(total_amount),SUM(paid_amount),SUM(total_amount-paid_amount)
            FROM invoices WHERE invoice_date BETWEEN ?1 AND ?2""")
            .setParameter(1, from).setParameter(2, to).getResultList();
        return Map.of("rows", toListOfMaps(rows,
            "period","invoice_count","subtotal","sgst","cgst","total_gst","total_amount","paid_amount","outstanding"),
            "totals", toSingleMap(totals,
            "invoice_count","subtotal","sgst","cgst","total_gst","total_amount","paid_amount","outstanding"));
    }

    public List<Map<String,Object>> outstanding(String asOf) {
        List<?> rows = em.createNativeQuery("""
            SELECT i.invoice_number, c.name as customer_name, c.phone,
                   i.invoice_date, i.due_date, i.total_amount, i.paid_amount,
                   (i.total_amount - i.paid_amount) as balance_due, i.status,
                   CASE WHEN i.due_date < ?1 AND i.status <> 'PAID' THEN 'OVERDUE' ELSE 'CURRENT' END as overdue_flag,
                   CASE WHEN i.due_date < ?2 AND i.status <> 'PAID' THEN CAST(julianday(?3)-julianday(i.due_date) AS INTEGER) ELSE 0 END as days_overdue
            FROM invoices i LEFT JOIN customers c ON i.customer_id=c.id
            WHERE i.status <> 'PAID' ORDER BY overdue_flag DESC, days_overdue DESC""")
            .setParameter(1,asOf).setParameter(2,asOf).setParameter(3,asOf).getResultList();
        return toListOfMaps(rows,"invoice_number","customer_name","phone","invoice_date","due_date",
            "total_amount","paid_amount","balance_due","status","overdue_flag","days_overdue");
    }

    public List<Map<String,Object>> customerSales(String from, String to) {
        List<?> rows = em.createNativeQuery("""
            SELECT c.name, c.phone, COUNT(i.id) as invoice_count,
                   SUM(i.subtotal) as subtotal,
                   SUM(CASE WHEN i.include_gst=1 THEN i.tax_amount ELSE 0 END) as gst_amount,
                   SUM(i.total_amount) as total_amount, SUM(i.paid_amount) as paid_amount,
                   SUM(i.total_amount-i.paid_amount) as outstanding
            FROM invoices i LEFT JOIN customers c ON i.customer_id=c.id
            WHERE i.invoice_date BETWEEN ?1 AND ?2
            GROUP BY i.customer_id ORDER BY total_amount DESC""")
            .setParameter(1,from).setParameter(2,to).getResultList();
        return toListOfMaps(rows,"customer_name","phone","invoice_count","subtotal","gst_amount","total_amount","paid_amount","outstanding");
    }

    public List<Map<String,Object>> productSales(String from, String to) {
        List<?> rows = em.createNativeQuery("""
            SELECT ii.product_name, p.category, ii.unit,
                   SUM(ii.quantity) as total_qty, AVG(ii.unit_price) as avg_price,
                   SUM(ii.total) as total_sales,
                   SUM(CASE WHEN inv.include_gst=1 THEN ii.total*(ii.sgst_percent+ii.cgst_percent)/100.0 ELSE 0 END) as gst_collected,
                   COUNT(DISTINCT inv.id) as invoice_count
            FROM invoice_items ii JOIN invoices inv ON ii.invoice_id=inv.id
            LEFT JOIN products p ON ii.product_id=p.id
            WHERE inv.invoice_date BETWEEN ?1 AND ?2
            GROUP BY ii.product_name ORDER BY total_sales DESC""")
            .setParameter(1,from).setParameter(2,to).getResultList();
        return toListOfMaps(rows,"product_name","category","unit","total_qty","avg_price","total_sales","gst_collected","invoice_count");
    }

    public Map<String,Object> gst(String from, String to) {
        List<?> monthly = em.createNativeQuery("""
            SELECT strftime('%Y-%m',invoice_date) as period, COUNT(*) as invoice_count,
                   SUM(subtotal) as taxable_value, SUM(sgst_amount) as sgst_amount,
                   SUM(cgst_amount) as cgst_amount, SUM(tax_amount) as total_gst,
                   SUM(total_amount) as total_with_gst
            FROM invoices WHERE invoice_date BETWEEN ?1 AND ?2 AND include_gst=1
            GROUP BY period ORDER BY period""")
            .setParameter(1,from).setParameter(2,to).getResultList();
        List<?> byProduct = em.createNativeQuery("""
            SELECT ii.product_name, p.category,
                   SUM(ii.total) as taxable_value, ii.sgst_percent, ii.cgst_percent,
                   SUM(ii.total*ii.sgst_percent/100.0) as sgst_amount,
                   SUM(ii.total*ii.cgst_percent/100.0) as cgst_amount,
                   SUM(ii.total*(ii.sgst_percent+ii.cgst_percent)/100.0) as total_gst
            FROM invoice_items ii JOIN invoices inv ON ii.invoice_id=inv.id AND inv.include_gst=1
            LEFT JOIN products p ON ii.product_id=p.id
            WHERE inv.invoice_date BETWEEN ?1 AND ?2
            GROUP BY ii.product_name, ii.sgst_percent, ii.cgst_percent ORDER BY taxable_value DESC""")
            .setParameter(1,from).setParameter(2,to).getResultList();
        return Map.of(
            "monthly", toListOfMaps(monthly,"period","invoice_count","taxable_value","sgst_amount","cgst_amount","total_gst","total_with_gst"),
            "byProduct", toListOfMaps(byProduct,"product_name","category","taxable_value","sgst_percent","cgst_percent","sgst_amount","cgst_amount","total_gst")
        );
    }

    public Map<String,Object> paymentCollection(String from, String to) {
        List<?> rows = em.createNativeQuery("""
            SELECT p.payment_date, i.invoice_number, c.name as customer_name,
                   p.amount, p.method, p.reference, p.notes
            FROM payments p JOIN invoices i ON p.invoice_id=i.id
            LEFT JOIN customers c ON i.customer_id=c.id
            WHERE p.payment_date BETWEEN ?1 AND ?2 ORDER BY p.payment_date DESC""")
            .setParameter(1,from).setParameter(2,to).getResultList();
        List<?> bMethod = em.createNativeQuery("""
            SELECT method, COUNT(*) as cnt, SUM(amount) as total_amount
            FROM payments WHERE payment_date BETWEEN ?1 AND ?2 GROUP BY method ORDER BY total_amount DESC""")
            .setParameter(1,from).setParameter(2,to).getResultList();
        return Map.of(
            "rows", toListOfMaps(rows,"payment_date","invoice_number","customer_name","amount","method","reference","notes"),
            "methodSummary", toListOfMaps(bMethod,"method","count","total_amount")
        );
    }

    // Helpers
    private List<Map<String,Object>> toListOfMaps(List<?> rows, String... cols) {
        List<Map<String,Object>> result = new ArrayList<>();
        for (Object row : rows) {
            Object[] r = row instanceof Object[] ? (Object[])row : new Object[]{row};
            Map<String,Object> m = new LinkedHashMap<>();
            for (int i=0; i<Math.min(cols.length, r.length); i++) m.put(cols[i], r[i]);
            result.add(m);
        }
        return result;
    }

    private Map<String,Object> toSingleMap(List<?> rows, String... cols) {
        if (rows.isEmpty()) return new LinkedHashMap<>();
        Object[] r = rows.get(0) instanceof Object[] ? (Object[])rows.get(0) : new Object[]{rows.get(0)};
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i=0; i<Math.min(cols.length, r.length); i++) m.put(cols[i], r[i]);
        return m;
    }
}
