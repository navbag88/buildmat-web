package com.buildmat.util;

import com.buildmat.dao.ReportQueryService;
import com.buildmat.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelExportUtil {

    // ── Customers ──────────────────────────────────────────────────────────────
    public static byte[] exportCustomers(List<CustomerEntity> list) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Customers");
            CellStyle hdr = hdrStyle(wb), def = wb.createCellStyle(), alt = altStyle(wb), tot = totStyle(wb);
            addTitle(s, wb, "Customers — Total: " + list.size(), 5);
            hdrRow(s.createRow(2), hdr, "ID","Name","Phone","Email","Address");
            setColWidths(s, 2000, 7000, 4500, 6000, 9000);
            for (int i=0; i<list.size(); i++) {
                CustomerEntity c = list.get(i); Row r = s.createRow(i+3); CellStyle st = i%2==0?alt:def;
                strRow(r, st, String.valueOf(c.getId()), c.getName(), nvl(c.getPhone()), nvl(c.getEmail()), nvl(c.getAddress()));
            }
            strRow(s.createRow(list.size()+3), tot, "Total", String.valueOf(list.size()),"","","");
            return toBytes(wb);
        }
    }

    // ── Products ───────────────────────────────────────────────────────────────
    public static byte[] exportProducts(List<ProductEntity> list) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Products");
            CellStyle hdr = hdrStyle(wb), def = wb.createCellStyle(), alt = altStyle(wb), tot = totStyle(wb), num = numStyle(wb), numAlt = numAltStyle(wb);
            addTitle(s, wb, "Products — Total: " + list.size(), 8);
            hdrRow(s.createRow(2), hdr, "ID","Name","Category","Unit","Price","Stock","SGST%","CGST%");
            setColWidths(s, 2000, 7000, 4000, 3000, 4000, 3500, 3000, 3000);
            for (int i=0; i<list.size(); i++) {
                ProductEntity p = list.get(i); Row r = s.createRow(i+3); CellStyle st = i%2==0?alt:def; CellStyle ns = i%2==0?numAlt:num;
                strCells(r, st, 0, String.valueOf(p.getId()), p.getName(), nvl(p.getCategory()), nvl(p.getUnit()));
                numCells(r, ns, 4, p.getPrice(), p.getStockQty(), p.getSgstPercent(), p.getCgstPercent());
            }
            return toBytes(wb);
        }
    }

    // ── Invoices ───────────────────────────────────────────────────────────────
    public static byte[] exportInvoices(List<InvoiceEntity> list) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Invoices");
            CellStyle hdr = hdrStyle(wb), def = wb.createCellStyle(), alt = altStyle(wb), tot = totStyle(wb), num = numStyle(wb), numAlt = numAltStyle(wb), totNum = totNumStyle(wb);
            addTitle(s, wb, "Invoices — Total: " + list.size(), 10);
            hdrRow(s.createRow(2), hdr, "Invoice #","Customer","Date","Due Date","Subtotal","GST","Total","Paid","Balance","Status");
            setColWidths(s, 4500, 6000, 3500, 3500, 4000, 4000, 4500, 4000, 4000, 3500);
            double subT=0,gstT=0,totT=0,paidT=0,balT=0;
            for (int i=0; i<list.size(); i++) {
                InvoiceEntity inv = list.get(i); Row r = s.createRow(i+3); CellStyle st = i%2==0?alt:def; CellStyle ns = i%2==0?numAlt:num;
                strCells(r, st, 0, inv.getInvoiceNumber(), inv.getCustomer()!=null?inv.getCustomer().getName():"", inv.getInvoiceDate().toString(), inv.getDueDate()!=null?inv.getDueDate().toString():"");
                double gst = inv.getIncludeGst()?inv.getTaxAmount():0;
                double bal = inv.getTotalAmount()-inv.getPaidAmount();
                numCells(r, ns, 4, inv.getSubtotal(), gst, inv.getTotalAmount(), inv.getPaidAmount(), bal);
                strCell(r, 9, inv.getStatus(), st);
                subT+=inv.getSubtotal(); gstT+=gst; totT+=inv.getTotalAmount(); paidT+=inv.getPaidAmount(); balT+=bal;
            }
            Row tr = s.createRow(list.size()+3);
            strCells(tr, tot, 0, "TOTAL", String.valueOf(list.size()),"","");
            numCells(tr, totNumStyle(wb), 4, subT, gstT, totT, paidT, balT);
            return toBytes(wb);
        }
    }

    // ── Payments ───────────────────────────────────────────────────────────────
    public static byte[] exportPayments(List<PaymentEntity> list) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Payments");
            CellStyle hdr = hdrStyle(wb), def = wb.createCellStyle(), alt = altStyle(wb), tot = totStyle(wb), num = numStyle(wb), numAlt = numAltStyle(wb);
            addTitle(s, wb, "Payments — Total: " + list.size(), 7);
            hdrRow(s.createRow(2), hdr, "Date","Invoice #","Customer","Amount","Method","Reference","Notes");
            setColWidths(s, 3500, 4000, 6000, 4500, 3500, 5000, 5000);
            double total = 0;
            for (int i=0; i<list.size(); i++) {
                PaymentEntity p = list.get(i); Row r = s.createRow(i+3); CellStyle st = i%2==0?alt:def; CellStyle ns = i%2==0?numAlt:num;
                strCells(r, st, 0, p.getPaymentDate().toString(), p.getInvoice().getInvoiceNumber(),
                    p.getInvoice().getCustomer()!=null?p.getInvoice().getCustomer().getName():"");
                numCells(r, ns, 3, p.getAmount());
                strCells(r, st, 4, nvl(p.getMethod()), nvl(p.getReference()), nvl(p.getNotes()));
                total += p.getAmount();
            }
            Row tr = s.createRow(list.size()+3); strCells(tr,tot,0,"TOTAL","","");
            numCells(tr, totNumStyle(wb), 3, total);
            return toBytes(wb);
        }
    }

    // ── Report export dispatcher ───────────────────────────────────────────────
    public static byte[] exportReport(String type, String from, String to, String asOf, ReportQueryService q) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            switch (type) {
                case "sales-summary" -> {
                    var data = q.salesSummary(from, to);
                    Sheet s = wb.createSheet("Sales Summary");
                    addTitle(s, wb, "Sales Summary: "+from+" to "+to, 9);
                    hdrRow(s.createRow(2), hdrStyle(wb), "Period","Invoices","Subtotal","SGST","CGST","Total GST","Total","Paid","Outstanding");
                    var rows = (List<Map<String,Object>>) ((Map<?,?>)data).get("rows");
                    fillMapRows(s, wb, rows, false, "period","invoice_count","subtotal","sgst","cgst","total_gst","total_amount","paid_amount","outstanding");
                }
                case "outstanding" -> {
                    var rows = q.outstanding(asOf);
                    Sheet s = wb.createSheet("Outstanding");
                    addTitle(s, wb, "Outstanding as of "+asOf, 9);
                    hdrRow(s.createRow(2), hdrStyle(wb), "Invoice #","Customer","Phone","Inv Date","Due Date","Total","Paid","Balance","Status");
                    fillMapRows(s, wb, rows, false, "invoice_number","customer_name","phone","invoice_date","due_date","total_amount","paid_amount","balance_due","status");
                }
                case "customer-sales" -> {
                    var rows = q.customerSales(from, to);
                    Sheet s = wb.createSheet("Customer Sales");
                    addTitle(s, wb, "Customer Sales: "+from+" to "+to, 6);
                    hdrRow(s.createRow(2), hdrStyle(wb), "Customer","Phone","Invoices","Subtotal","GST","Total");
                    fillMapRows(s, wb, rows, false, "customer_name","phone","invoice_count","subtotal","gst_amount","total_amount");
                }
                case "product-sales" -> {
                    var rows = q.productSales(from, to);
                    Sheet s = wb.createSheet("Product Sales");
                    addTitle(s, wb, "Product Sales: "+from+" to "+to, 7);
                    hdrRow(s.createRow(2), hdrStyle(wb), "Product","Category","Unit","Total Qty","Avg Price","Total Sales","GST");
                    fillMapRows(s, wb, rows, false, "product_name","category","unit","total_qty","avg_price","total_sales","gst_collected");
                }
                case "gst" -> {
                    var data = q.gst(from, to);
                    Sheet s = wb.createSheet("GST Report");
                    addTitle(s, wb, "GST Report: "+from+" to "+to, 7);
                    hdrRow(s.createRow(2), hdrStyle(wb), "Period","Invoices","Taxable","SGST","CGST","Total GST","Total with GST");
                    fillMapRows(s, wb, (List<Map<String,Object>>)((Map<?,?>)data).get("monthly"), false,
                        "period","invoice_count","taxable_value","sgst_amount","cgst_amount","total_gst","total_with_gst");
                }
                case "payment-collection" -> {
                    var data = q.paymentCollection(from, to);
                    Sheet s = wb.createSheet("Payments");
                    addTitle(s, wb, "Payment Collection: "+from+" to "+to, 6);
                    hdrRow(s.createRow(2), hdrStyle(wb), "Date","Invoice #","Customer","Amount","Method","Reference");
                    fillMapRows(s, wb, (List<Map<String,Object>>)((Map<?,?>)data).get("rows"), false,
                        "payment_date","invoice_number","customer_name","amount","method","reference");
                }
            }
            return toBytes(wb);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private static void fillMapRows(Sheet s, Workbook wb, List<Map<String,Object>> rows, boolean hasTotal, String... keys) {
        CellStyle def = wb.createCellStyle(), alt = altStyle(wb), num = numStyle(wb), numAlt = numAltStyle(wb);
        Set<String> amtKeys = Set.of("subtotal","sgst","cgst","total_gst","total_amount","paid_amount","outstanding","total","gst_amount","balance_due","taxable_value","sgst_amount","cgst_amount","total_with_gst","total_qty","avg_price","total_sales","gst_collected","amount");
        for (int i=0; i<rows.size(); i++) {
            Row r = s.createRow(i+3); Map<String,Object> row = rows.get(i);
            boolean isAlt = i%2==0;
            for (int j=0; j<keys.length; j++) {
                Object v = row.get(keys[j]);
                Cell c = r.createCell(j);
                if (v instanceof Number && amtKeys.contains(keys[j])) {
                    c.setCellValue(((Number)v).doubleValue()); c.setCellStyle(isAlt?numAlt:num);
                } else {
                    c.setCellValue(v==null?"":v.toString()); c.setCellStyle(isAlt?alt:def);
                }
            }
        }
    }

    private static void addTitle(Sheet s, Workbook wb, String title, int cols) {
        Row r = s.createRow(0); CellStyle ts = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)13); f.setColor(IndexedColors.DARK_BLUE.getIndex()); ts.setFont(f);
        Cell c = r.createCell(0); c.setCellValue(title); c.setCellStyle(ts);
        s.addMergedRegion(new CellRangeAddress(0,0,0,cols-1));
        s.createRow(1);
    }

    private static void hdrRow(Row row, CellStyle style, String... vals) {
        for (int i=0; i<vals.length; i++) { Cell c = row.createCell(i); c.setCellValue(vals[i]); c.setCellStyle(style); }
        row.setHeightInPoints(20);
    }

    private static void strRow(Row row, CellStyle style, String... vals) {
        for (int i=0; i<vals.length; i++) { Cell c = row.createCell(i); c.setCellValue(vals[i]); c.setCellStyle(style); }
    }

    private static void strCells(Row row, CellStyle style, int start, String... vals) {
        for (int i=0; i<vals.length; i++) { Cell c = row.createCell(start+i); c.setCellValue(vals[i]); c.setCellStyle(style); }
    }

    private static void numCells(Row row, CellStyle style, int start, double... vals) {
        for (int i=0; i<vals.length; i++) { Cell c = row.createCell(start+i); c.setCellValue(vals[i]); c.setCellStyle(style); }
    }

    private static void strCell(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private static void setColWidths(Sheet s, int... widths) {
        for (int i=0; i<widths.length; i++) s.setColumnWidth(i, widths[i]);
    }

    private static byte[] toBytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); wb.write(bos); return bos.toByteArray();
    }

    private static CellStyle hdrStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); f.setFontHeightInPoints((short)10); s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER); s.setBorderBottom(BorderStyle.THIN); return s;
    }
    private static CellStyle altStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex()); s.setFillPattern(FillPatternType.SOLID_FOREGROUND); return s;
    }
    private static CellStyle totStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex()); s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); s.setFont(f); s.setBorderTop(BorderStyle.MEDIUM); return s;
    }
    private static CellStyle numStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle(); DataFormat df = wb.createDataFormat(); s.setDataFormat(df.getFormat("#,##0.00")); s.setAlignment(HorizontalAlignment.RIGHT); return s;
    }
    private static CellStyle numAltStyle(Workbook wb) {
        CellStyle s = numStyle(wb); s.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex()); s.setFillPattern(FillPatternType.SOLID_FOREGROUND); return s;
    }
    private static CellStyle totNumStyle(Workbook wb) {
        CellStyle s = numStyle(wb); s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex()); s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); s.setFont(f); s.setBorderTop(BorderStyle.MEDIUM); return s;
    }
    private static String nvl(String s) { return s==null?"":s; }
}
