package com.buildmat.service;

import com.buildmat.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

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
