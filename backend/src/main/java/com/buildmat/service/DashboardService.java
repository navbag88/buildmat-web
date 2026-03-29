package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class DashboardService {
    private final InvoiceRepository invoiceRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRevenue",    invoiceRepo.sumPaidAmount());
        stats.put("outstanding",     invoiceRepo.sumOutstanding());
        stats.put("paidCount",       invoiceRepo.countByStatus("PAID"));
        stats.put("unpaidCount",     invoiceRepo.countByStatus("UNPAID"));
        stats.put("partialCount",    invoiceRepo.countByStatus("PARTIAL"));
        stats.put("customerCount",   customerRepo.count());
        stats.put("productCount",    productRepo.count());
        stats.put("recentInvoices",  invoiceRepo.findByOrderByInvoiceDateDescIdDesc()
            .stream().limit(8).map(this::invoiceSummary).collect(Collectors.toList()));
        return stats;
    }

    private Map<String,Object> invoiceSummary(InvoiceEntity i) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", i.getId()); m.put("invoiceNumber", i.getInvoiceNumber());
        m.put("customerName", i.getCustomer() != null ? i.getCustomer().getName() : "");
        m.put("invoiceDate", i.getInvoiceDate()); m.put("totalAmount", i.getTotalAmount());
        m.put("status", i.getStatus());
        return m;
    }
}
