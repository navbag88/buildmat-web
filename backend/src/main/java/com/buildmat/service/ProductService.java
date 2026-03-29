package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
