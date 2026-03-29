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
public class PaymentService {
    private final PaymentRepository paymentRepo;
    private final InvoiceRepository invoiceRepo;

    public List<Map<String,Object>> getAll() {
        return paymentRepo.findByOrderByPaymentDateDescIdDesc().stream().map(this::toMap).collect(Collectors.toList());
    }

    public List<Map<String,Object>> byInvoice(Long invoiceId) {
        return paymentRepo.findByInvoiceId(invoiceId).stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> save(Map<String,Object> body) {
        Long invoiceId = toLong(body.get("invoiceId"));
        InvoiceEntity inv = invoiceRepo.findById(invoiceId).orElseThrow();
        PaymentEntity p = new PaymentEntity();
        p.setInvoice(inv); p.setAmount(toDouble(body.get("amount")));
        p.setPaymentDate(LocalDate.parse((String)body.get("paymentDate")));
        p.setMethod((String)body.getOrDefault("method","CASH"));
        p.setReference((String)body.getOrDefault("reference",""));
        p.setNotes((String)body.getOrDefault("notes",""));
        p.setCreatedAt(LocalDateTime.now());
        PaymentEntity saved = paymentRepo.save(p);
        updateInvoiceStatus(inv);
        return toMap(saved);
    }

    public void delete(Long id) {
        paymentRepo.findById(id).ifPresent(p -> {
            InvoiceEntity inv = p.getInvoice();
            paymentRepo.deleteById(id);
            updateInvoiceStatus(inv);
        });
    }

    private void updateInvoiceStatus(InvoiceEntity inv) {
        double paid = paymentRepo.findByInvoiceId(inv.getId()).stream().mapToDouble(PaymentEntity::getAmount).sum();
        inv.setPaidAmount(paid);
        if (paid >= inv.getTotalAmount()) inv.setStatus("PAID");
        else if (paid > 0) inv.setStatus("PARTIAL");
        else inv.setStatus("UNPAID");
        invoiceRepo.save(inv);
    }

    public ResponseEntity<byte[]> exportExcel() {
        try { byte[] d = ExcelExportUtil.exportPayments(paymentRepo.findByOrderByPaymentDateDescIdDesc());
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename=payments.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(d); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try { byte[] d = PdfExportUtil.exportPayments(paymentRepo.findByOrderByPaymentDateDescIdDesc());
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename=payments.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(d); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private Map<String,Object> toMap(PaymentEntity p) {
        return Map.of("id",p.getId(),"invoiceId",p.getInvoice().getId(),
            "invoiceNumber",p.getInvoice().getInvoiceNumber(),
            "customerName",p.getInvoice().getCustomer()!=null?p.getInvoice().getCustomer().getName():"",
            "amount",p.getAmount(),"paymentDate",p.getPaymentDate(),
            "method",p.getMethod(),"reference",nvl(p.getReference()),"notes",nvl(p.getNotes()));
    }

    double toDouble(Object o) { if(o instanceof Number) return ((Number)o).doubleValue(); try{return Double.parseDouble(o.toString());}catch(Exception e){return 0;} }
    Long toLong(Object o) { if(o instanceof Number) return ((Number)o).longValue(); try{return Long.parseLong(o.toString());}catch(Exception e){return null;} }
    String nvl(String s) { return s==null?"":s; }
}
