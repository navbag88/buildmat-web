package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional
public class InvoiceService {
    private final InvoiceRepository invoiceRepo;
    private final CustomerRepository customerRepo;
    private final PaymentRepository paymentRepo;

    @SuppressWarnings("unchecked")
    public Map<String,Object> save(Long id, Map<String,Object> body) {
        InvoiceEntity inv = id != null ? invoiceRepo.findById(id).orElseThrow() : new InvoiceEntity();
        Long custId = toLong(body.get("customerId"));
        if (custId != null) inv.setCustomer(customerRepo.findById(custId).orElse(null));
        inv.setInvoiceDate(LocalDate.parse((String)body.get("invoiceDate")));
        if (body.get("dueDate") != null && !body.get("dueDate").toString().isBlank())
            inv.setDueDate(LocalDate.parse((String)body.get("dueDate")));
        inv.setIncludeGst((Boolean)body.getOrDefault("includeGst", true));
        inv.setNotes((String)body.getOrDefault("notes",""));
        if (inv.getInvoiceNumber() == null || inv.getInvoiceNumber().isBlank())
            inv.setInvoiceNumber(generateNumber());
        if (inv.getCreatedAt() == null) inv.setCreatedAt(LocalDateTime.now());

        inv.getItems().clear();
        List<Map<String,Object>> items = (List<Map<String,Object>>) body.get("items");
        if (items != null) {
            for (Map<String,Object> it : items) {
                InvoiceItemEntity item = new InvoiceItemEntity();
                item.setInvoice(inv);
                item.setProductId(toLong(it.get("productId")));
                item.setProductName((String)it.get("productName"));
                item.setUnit((String)it.getOrDefault("unit",""));
                item.setQuantity(toDouble(it.get("quantity")));
                item.setUnitPrice(toDouble(it.get("unitPrice")));
                item.setTotal(item.getQuantity() * item.getUnitPrice());
                item.setSgstPercent(toDouble(it.getOrDefault("sgstPercent",0)));
                item.setCgstPercent(toDouble(it.getOrDefault("cgstPercent",0)));
                inv.getItems().add(item);
            }
        }
        recalculate(inv);
        return toMap(invoiceRepo.save(inv));
    }

    private void recalculate(InvoiceEntity inv) {
        double sub = inv.getItems().stream().mapToDouble(InvoiceItemEntity::getTotal).sum();
        inv.setSubtotal(sub);
        if (inv.getIncludeGst()) {
            double sgst = inv.getItems().stream().mapToDouble(i -> i.getTotal() * i.getSgstPercent() / 100).sum();
            double cgst = inv.getItems().stream().mapToDouble(i -> i.getTotal() * i.getCgstPercent() / 100).sum();
            inv.setSgstAmount(sgst); inv.setCgstAmount(cgst); inv.setTaxAmount(sgst + cgst);
        } else { inv.setSgstAmount(0.0); inv.setCgstAmount(0.0); inv.setTaxAmount(0.0); }
        inv.setTotalAmount(sub + inv.getTaxAmount());
        double paid = paymentRepo.findByInvoiceId(inv.getId() != null ? inv.getId() : 0L)
            .stream().mapToDouble(PaymentEntity::getAmount).sum();
        inv.setPaidAmount(paid);
        if (paid >= inv.getTotalAmount()) inv.setStatus("PAID");
        else if (paid > 0) inv.setStatus("PARTIAL");
        else inv.setStatus("UNPAID");
    }

    public List<Map<String,Object>> getAll(String q) {
        List<InvoiceEntity> list = (q == null || q.isBlank())
            ? invoiceRepo.findByOrderByInvoiceDateDescIdDesc()
            : invoiceRepo.findByInvoiceNumberContainingIgnoreCaseOrCustomerNameContainingIgnoreCaseOrStatusContainingIgnoreCase(q,q,q);
        return list.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> getById(Long id) { return invoiceRepo.findById(id).map(this::toFullMap).orElseThrow(); }
    public void delete(Long id) { invoiceRepo.deleteById(id); }

    public ResponseEntity<byte[]> generatePdf(Long id) {
        try { InvoiceEntity inv = invoiceRepo.findById(id).orElseThrow();
            byte[] pdf = PdfExportUtil.generateInvoicePdf(inv);
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+inv.getInvoiceNumber()+".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> exportExcel() {
        try { byte[] d = ExcelExportUtil.exportInvoices(invoiceRepo.findByOrderByInvoiceDateDescIdDesc());
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename=invoices.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(d); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try { byte[] d = PdfExportUtil.exportInvoices(invoiceRepo.findByOrderByInvoiceDateDescIdDesc());
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename=invoices.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(d); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private String generateNumber() {
        String prefix = "INV-" + LocalDate.now().getYear() + "-";
        Optional<String> max = invoiceRepo.findMaxInvoiceNumber(prefix);
        int next = 1;
        if (max.isPresent()) {
            try { next = Integer.parseInt(max.get().substring(prefix.length())) + 1; } catch (Exception ignored) {}
        }
        return prefix + String.format("%04d", next);
    }

    private Map<String,Object> toMap(InvoiceEntity i) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",i.getId()); m.put("invoiceNumber",i.getInvoiceNumber());
        m.put("customerId", i.getCustomer()!=null?i.getCustomer().getId():null);
        m.put("customerName", i.getCustomer()!=null?i.getCustomer().getName():"");
        m.put("invoiceDate",i.getInvoiceDate()); m.put("dueDate",i.getDueDate());
        m.put("subtotal",i.getSubtotal()); m.put("sgstAmount",i.getSgstAmount());
        m.put("cgstAmount",i.getCgstAmount()); m.put("taxAmount",i.getTaxAmount());
        m.put("totalAmount",i.getTotalAmount()); m.put("paidAmount",i.getPaidAmount());
        m.put("balanceDue",i.getTotalAmount()-i.getPaidAmount());
        m.put("includeGst",i.getIncludeGst()); m.put("status",i.getStatus()); m.put("notes",i.getNotes());
        return m;
    }

    private Map<String,Object> toFullMap(InvoiceEntity i) {
        Map<String,Object> m = toMap(i);
        m.put("items", i.getItems().stream().map(item -> Map.of(
            "id",item.getId(),"productId",nvl(item.getProductId()),"productName",item.getProductName(),
            "unit",nvl(item.getUnit()),"quantity",item.getQuantity(),"unitPrice",item.getUnitPrice(),
            "total",item.getTotal(),"sgstPercent",item.getSgstPercent(),"cgstPercent",item.getCgstPercent()
        )).collect(Collectors.toList()));
        return m;
    }

    double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number)o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
    Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number)o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
    Object nvl(Object o) { return o == null ? "" : o; }
}
