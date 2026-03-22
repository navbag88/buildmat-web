package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ── Auth Service ───────────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final com.buildmat.security.JwtUtil jwt;

    public ResponseEntity<?> login(String username, String password) {
        Optional<UserEntity> opt = userRepo.findByUsernameAndActiveTrue(username.trim().toLowerCase());
        if (opt.isEmpty() || !encoder.matches(password, opt.get().getPasswordHash()))
            return ResponseEntity.status(401).body(Map.of("error","Invalid username or password"));
        UserEntity u = opt.get();
        String token = jwt.generate(u.getUsername(), u.getRole());
        return ResponseEntity.ok(Map.of(
            "token", token, "username", u.getUsername(),
            "fullName", u.getFullName(), "role", u.getRole(), "id", u.getId()
        ));
    }

    public ResponseEntity<?> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsernameAndActiveTrue(username)
            .map(u -> ResponseEntity.ok(Map.of(
                "username", u.getUsername(), "fullName", u.getFullName(),
                "role", u.getRole(), "id", u.getId()
            )))
            .orElse(ResponseEntity.status(401).build());
    }

    @jakarta.annotation.PostConstruct
    public void seedAdmin() {
        if (!userRepo.existsByUsername("admin")) {
            userRepo.save(UserEntity.builder()
                .username("admin").fullName("Administrator").role("ADMIN").active(true)
                .passwordHash(encoder.encode("admin123")).build());
        }
    }
}

// ── Dashboard Service ──────────────────────────────────────────────────────────
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

// ── Customer Service ───────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository repo;

    public List<Map<String,Object>> getAll(String q) {
        List<CustomerEntity> list = (q == null || q.isBlank()) ? repo.findAll() :
            repo.findByNameContainingIgnoreCaseOrPhoneContaining(q, q);
        return list.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> getById(Long id) {
        return repo.findById(id).map(this::toMap).orElseThrow();
    }

    public Map<String,Object> save(Long id, Map<String,Object> body) {
        CustomerEntity c = id != null ? repo.findById(id).orElseThrow() : new CustomerEntity();
        c.setName((String)body.get("name"));
        c.setPhone((String)body.getOrDefault("phone",""));
        c.setEmail((String)body.getOrDefault("email",""));
        c.setAddress((String)body.getOrDefault("address",""));
        if (c.getCreatedAt() == null) c.setCreatedAt(LocalDateTime.now());
        return toMap(repo.save(c));
    }

    public void delete(Long id) { repo.deleteById(id); }

    public Map<String,Object> importExcel(MultipartFile file) {
        try {
            List<Map<String,Object>> results = ExcelImportUtil.importCustomers(file.getInputStream());
            int imported = 0;
            List<String> errors = new ArrayList<>();
            for (Map<String,Object> row : results) {
                if (row.containsKey("error")) { errors.add((String)row.get("error")); continue; }
                CustomerEntity c = new CustomerEntity();
                c.setName((String)row.get("name")); c.setPhone((String)row.getOrDefault("phone",""));
                c.setEmail((String)row.getOrDefault("email","")); c.setAddress((String)row.getOrDefault("address",""));
                c.setCreatedAt(LocalDateTime.now()); repo.save(c); imported++;
            }
            return Map.of("imported", imported, "errors", errors);
        } catch (Exception e) { throw new RuntimeException("Import failed: " + e.getMessage()); }
    }

    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] data = ExcelExportUtil.exportCustomers(repo.findAll());
            return download(data, "customers.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try {
            byte[] data = PdfExportUtil.exportCustomers(repo.findAll());
            return download(data, "customers.pdf", "application/pdf");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private Map<String,Object> toMap(CustomerEntity c) {
        return Map.of("id",c.getId(),"name",c.getName(),"phone",nvl(c.getPhone()),
            "email",nvl(c.getEmail()),"address",nvl(c.getAddress()));
    }

    private String nvl(String s) { return s == null ? "" : s; }
    private ResponseEntity<byte[]> download(byte[] data, String name, String ct) {
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+name)
            .contentType(MediaType.parseMediaType(ct)).body(data);
    }
}

// ── Product Service ────────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;

    public List<Map<String,Object>> getAll(String q) {
        List<ProductEntity> list = (q == null || q.isBlank()) ? repo.findAll() :
            repo.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(q, q);
        return list.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String,Object> getById(Long id) { return repo.findById(id).map(this::toMap).orElseThrow(); }

    public Map<String,Object> save(Long id, Map<String,Object> body) {
        ProductEntity p = id != null ? repo.findById(id).orElseThrow() : new ProductEntity();
        p.setName((String)body.get("name"));
        p.setCategory((String)body.getOrDefault("category",""));
        p.setUnit((String)body.getOrDefault("unit","Unit"));
        p.setPrice(toDouble(body.get("price")));
        p.setStockQty(toDouble(body.getOrDefault("stockQty", body.getOrDefault("stock_qty", 0))));
        p.setSgstPercent(toDouble(body.getOrDefault("sgstPercent", body.getOrDefault("sgst_percent", 0))));
        p.setCgstPercent(toDouble(body.getOrDefault("cgstPercent", body.getOrDefault("cgst_percent", 0))));
        if (p.getCreatedAt() == null) p.setCreatedAt(LocalDateTime.now());
        return toMap(repo.save(p));
    }

    public void delete(Long id) { repo.deleteById(id); }

    public Map<String,Object> importExcel(MultipartFile file) {
        try {
            List<Map<String,Object>> results = ExcelImportUtil.importProducts(file.getInputStream());
            int imported = 0; List<String> errors = new ArrayList<>();
            for (Map<String,Object> row : results) {
                if (row.containsKey("error")) { errors.add((String)row.get("error")); continue; }
                ProductEntity p = new ProductEntity();
                p.setName((String)row.get("name")); p.setCategory((String)row.getOrDefault("category",""));
                p.setUnit((String)row.getOrDefault("unit","Unit")); p.setPrice(toDouble(row.get("price")));
                p.setStockQty(toDouble(row.getOrDefault("stockQty",0)));
                p.setSgstPercent(toDouble(row.getOrDefault("sgstPercent",0)));
                p.setCgstPercent(toDouble(row.getOrDefault("cgstPercent",0)));
                p.setCreatedAt(LocalDateTime.now()); repo.save(p); imported++;
            }
            return Map.of("imported", imported, "errors", errors);
        } catch (Exception e) { throw new RuntimeException("Import failed: " + e.getMessage()); }
    }

    public ResponseEntity<byte[]> exportExcel() {
        try { byte[] d = ExcelExportUtil.exportProducts(repo.findAll());
            return dl(d,"products.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> exportPdf() {
        try { byte[] d = PdfExportUtil.exportProducts(repo.findAll()); return dl(d,"products.pdf","application/pdf"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private Map<String,Object> toMap(ProductEntity p) {
        return Map.of("id",p.getId(),"name",p.getName(),"category",nvl(p.getCategory()),
            "unit",p.getUnit(),"price",p.getPrice(),"stockQty",p.getStockQty(),
            "sgstPercent",p.getSgstPercent(),"cgstPercent",p.getCgstPercent(),
            "totalGstPercent",p.getSgstPercent()+p.getCgstPercent());
    }

    double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number)o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; }
    }
    private String nvl(String s) { return s == null ? "" : s; }
    private ResponseEntity<byte[]> dl(byte[] d, String n, String ct) {
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+n)
            .contentType(MediaType.parseMediaType(ct)).body(d);
    }
}

// ── Invoice Service ────────────────────────────────────────────────────────────
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

// ── Payment Service ────────────────────────────────────────────────────────────
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

// ── Report Service ─────────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor
public class ReportService {
    private final com.buildmat.dao.ReportQueryService qSvc;

    public Object salesSummary(String from, String to) { return qSvc.salesSummary(from,to); }
    public Object outstanding(String asOf) { return qSvc.outstanding(asOf); }
    public Object customerSales(String from, String to) { return qSvc.customerSales(from,to); }
    public Object productSales(String from, String to) { return qSvc.productSales(from,to); }
    public Object gst(String from, String to) { return qSvc.gst(from,to); }
    public Object paymentCollection(String from, String to) { return qSvc.paymentCollection(from,to); }

    public ResponseEntity<byte[]> exportExcel(String type, String from, String to, String asOf) {
        try {
            byte[] data = ExcelExportUtil.exportReport(type, from, to, asOf, qSvc);
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+type+".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(data);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public ResponseEntity<byte[]> exportPdf(String type, String from, String to, String asOf) {
        try {
            byte[] data = PdfExportUtil.exportReport(type, from, to, asOf, qSvc);
            return ResponseEntity.ok().header("Content-Disposition","attachment; filename="+type+".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(data);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}

// ── User Service ───────────────────────────────────────────────────────────────
@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public List<Map<String,Object>> getAll() { return repo.findAll().stream().map(this::toMap).collect(Collectors.toList()); }

    public Map<String,Object> create(Map<String,Object> body) {
        if (repo.existsByUsername((String)body.get("username"))) throw new RuntimeException("Username already exists");
        UserEntity u = UserEntity.builder()
            .username(((String)body.get("username")).trim().toLowerCase())
            .fullName((String)body.get("fullName"))
            .role((String)body.getOrDefault("role","USER"))
            .active((Boolean)body.getOrDefault("active",true))
            .passwordHash(encoder.encode((String)body.get("password")))
            .createdAt(LocalDateTime.now()).build();
        return toMap(repo.save(u));
    }

    public Map<String,Object> update(Long id, Map<String,Object> body) {
        UserEntity u = repo.findById(id).orElseThrow();
        u.setFullName((String)body.get("fullName"));
        u.setRole((String)body.getOrDefault("role", u.getRole()));
        u.setActive((Boolean)body.getOrDefault("active", u.isActive()));
        return toMap(repo.save(u));
    }

    public void changePassword(Long id, String newPassword) {
        UserEntity u = repo.findById(id).orElseThrow();
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
    }

    public void delete(Long id) { repo.deleteById(id); }

    private Map<String,Object> toMap(UserEntity u) {
        return Map.of("id",u.getId(),"username",u.getUsername(),"fullName",u.getFullName(),
            "role",u.getRole(),"active",u.isActive());
    }
}
