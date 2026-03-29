package com.buildmat.util;

import com.buildmat.dao.ReportQueryService;
import com.buildmat.model.*;
import com.itextpdf.kernel.colors.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.borders.*;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class PdfExportUtil {

    private static final DeviceRgb BRAND   = new DeviceRgb(37,99,235);
    private static final DeviceRgb DARK    = new DeviceRgb(26,35,50);
    private static final DeviceRgb ALTBG   = new DeviceRgb(239,246,255);
    private static final DeviceRgb MIDGRAY = new DeviceRgb(107,114,128);
    private static final DeviceRgb TOTALBG = new DeviceRgb(219,234,254);
    private static final DeviceRgb GREEN   = new DeviceRgb(22,163,74);
    private static final DeviceRgb RED     = new DeviceRgb(220,38,38);
    private static final DeviceRgb ORANGE  = new DeviceRgb(217,119,6);
    private static final NumberFormat INR  = NumberFormat.getCurrencyInstance(new Locale("en","IN"));

    // ── Invoice PDF ────────────────────────────────────────────────────────────
    public static byte[] generateInvoicePdf(InvoiceEntity inv) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document(new PdfDocument(new PdfWriter(bos)), PageSize.A4);
        doc.setMargins(36,40,36,40);
        PdfFont bold = bold(), reg = reg();

        // Header
        Table ht = new Table(UnitValue.createPercentArray(new float[]{60,40})).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        Cell lc = new Cell().setBorder(Border.NO_BORDER);
        lc.add(new Paragraph("BuildMat Supplies").setFont(bold).setFontSize(18).setFontColor(BRAND));
        lc.add(new Paragraph("Building Material Supplier | GST No: 27ABCDE1234F1Z5").setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
        lc.add(new Paragraph("Ph: +91 98765 43210 | billing@buildmat.in").setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
        Cell rc = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        rc.add(new Paragraph("TAX INVOICE").setFont(bold).setFontSize(20).setFontColor(DARK));
        rc.add(new Paragraph(inv.getInvoiceNumber()).setFont(bold).setFontSize(13).setFontColor(BRAND));
        rc.add(new Paragraph("Date: "+inv.getInvoiceDate()).setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
        if (inv.getDueDate()!=null) rc.add(new Paragraph("Due: "+inv.getDueDate()).setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
        DeviceRgb sc = "PAID".equals(inv.getStatus())?GREEN:"PARTIAL".equals(inv.getStatus())?ORANGE:RED;
        rc.add(new Paragraph(inv.getStatus()).setFont(bold).setFontSize(11).setFontColor(sc));
        ht.addCell(lc); ht.addCell(rc);
        doc.add(ht);
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1.5f)).setMarginTop(6).setMarginBottom(10));

        // Bill To
        if (inv.getCustomer()!=null) {
            Table bt = new Table(UnitValue.createPercentArray(new float[]{50,50})).useAllAvailableWidth().setBorder(Border.NO_BORDER);
            Cell btc = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(new DeviceRgb(239,246,255)).setPadding(12);
            btc.add(new Paragraph("BILL TO").setFont(bold).setFontSize(9).setFontColor(BRAND));
            btc.add(new Paragraph(inv.getCustomer().getName()).setFont(bold).setFontSize(13).setFontColor(DARK));
            if (inv.getCustomer().getPhone()!=null && !inv.getCustomer().getPhone().isBlank())
                btc.add(new Paragraph("Ph: "+inv.getCustomer().getPhone()).setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
            if (inv.getCustomer().getAddress()!=null && !inv.getCustomer().getAddress().isBlank())
                btc.add(new Paragraph(inv.getCustomer().getAddress()).setFont(reg).setFontSize(9).setFontColor(MIDGRAY));
            bt.addCell(btc); bt.addCell(new Cell().setBorder(Border.NO_BORDER));
            doc.add(bt); doc.add(new Paragraph(" "));
        }

        // Items table
        boolean showGst = inv.getIncludeGst();
        float[] cw = showGst ? new float[]{4,28,9,10,12,9,9,13,6} : new float[]{5,35,12,18,15,15};
        String[] hdrCols = showGst ? new String[]{"#","Description","Unit","Qty","Unit Price","SGST%","CGST%","GST Amt","Total"} : new String[]{"#","Description","Unit","Qty","Unit Price","Total"};
        Table items = new Table(UnitValue.createPercentArray(cw)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : hdrCols) items.addHeaderCell(new Cell().setBackgroundColor(BRAND).setPadding(6).setBorder(Border.NO_BORDER)
            .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)));
        int rn=1;
        for (InvoiceItemEntity item : inv.getItems()) {
            boolean alt = rn++%2==0;
            DeviceRgb bg = alt ? ALTBG : new DeviceRgb(255,255,255);
            if (showGst) {
                double gstAmt = item.getTotal()*(item.getSgstPercent()+item.getCgstPercent())/100;
                addItemCells(items, bg, bold, reg, String.valueOf(rn-1), item.getProductName(), nvl(item.getUnit()),
                    fmt(item.getQuantity()), INR.format(item.getUnitPrice()),
                    item.getSgstPercent()+"%", item.getCgstPercent()+"%", INR.format(gstAmt), INR.format(item.getTotal()));
            } else {
                addItemCells(items, bg, bold, reg, String.valueOf(rn-1), item.getProductName(), nvl(item.getUnit()),
                    fmt(item.getQuantity()), INR.format(item.getUnitPrice()), INR.format(item.getTotal()));
            }
        }
        doc.add(items); doc.add(new Paragraph(" "));

        // Totals
        Table tots = new Table(UnitValue.createPercentArray(new float[]{60,40})).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        Cell notesCell = new Cell().setBorder(Border.NO_BORDER);
        if (inv.getNotes()!=null&&!inv.getNotes().isBlank()) {
            notesCell.add(new Paragraph("Notes").setFont(bold).setFontSize(9).setFontColor(MIDGRAY));
            notesCell.add(new Paragraph(inv.getNotes()).setFont(reg).setFontSize(9).setFontColor(DARK));
        }
        tots.addCell(notesCell);
        Table ti = new Table(UnitValue.createPercentArray(new float[]{50,50})).useAllAvailableWidth().setBorder(new SolidBorder(new DeviceRgb(229,231,235),1));
        addTotRow(ti, reg, bold, "Subtotal", INR.format(inv.getSubtotal()), false);
        if (showGst) {
            addTotRow(ti, reg, bold, "SGST", INR.format(inv.getSgstAmount()), false);
            addTotRow(ti, reg, bold, "CGST", INR.format(inv.getCgstAmount()), false);
            addTotRow(ti, reg, bold, "Total GST", INR.format(inv.getTaxAmount()), false);
        }
        addTotRow(ti, reg, bold, "Grand Total", INR.format(inv.getTotalAmount()), true);
        addTotRow(ti, reg, bold, "Paid", INR.format(inv.getPaidAmount()), false);
        addTotRow(ti, reg, bold, "Balance Due", INR.format(inv.getTotalAmount()-inv.getPaidAmount()), false);
        tots.addCell(new Cell().setBorder(Border.NO_BORDER).add(ti));
        doc.add(tots);

        // Footer
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginTop(8).setMarginBottom(4));
        doc.add(new Paragraph("Thank you for your business! This is a computer-generated invoice.")
            .setFont(reg).setFontSize(7).setFontColor(MIDGRAY).setTextAlignment(TextAlignment.CENTER));
        doc.close(); return bos.toByteArray();
    }

    // ── Export lists ───────────────────────────────────────────────────────────
    public static byte[] exportCustomers(java.util.List<CustomerEntity> list) throws Exception {
        return simpleListPdf("Customers", "Total: "+list.size(), false,
            new String[]{"#","Name","Phone","Email","Address"},
            new float[]{6,26,15,22,31},
            list.stream().map(c -> new String[]{c.getId().toString(),c.getName(),nvl(c.getPhone()),nvl(c.getEmail()),nvl(c.getAddress())}).collect(Collectors.toList()));
    }

    public static byte[] exportProducts(java.util.List<ProductEntity> list) throws Exception {
        return simpleListPdf("Products", "Total: "+list.size(), true,
            new String[]{"#","Name","Category","Unit","Price","Stock","SGST%","CGST%"},
            new float[]{4,26,14,8,12,10,8,8},
            list.stream().map(p -> new String[]{p.getId().toString(),p.getName(),nvl(p.getCategory()),nvl(p.getUnit()),
                INR.format(p.getPrice()),fmt(p.getStockQty()),p.getSgstPercent()+"%",p.getCgstPercent()+"%"}).collect(Collectors.toList()));
    }

    public static byte[] exportInvoices(java.util.List<InvoiceEntity> list) throws Exception {
        return simpleListPdf("Invoices", "Total: "+list.size(), true,
            new String[]{"Invoice #","Customer","Date","Total","Paid","Balance","Status"},
            new float[]{14,20,10,13,12,13,8},
            list.stream().map(i -> new String[]{i.getInvoiceNumber(),i.getCustomer()!=null?i.getCustomer().getName():"",
                i.getInvoiceDate().toString(),INR.format(i.getTotalAmount()),INR.format(i.getPaidAmount()),
                INR.format(i.getTotalAmount()-i.getPaidAmount()),i.getStatus()}).collect(Collectors.toList()));
    }

    public static byte[] exportPayments(java.util.List<PaymentEntity> list) throws Exception {
        return simpleListPdf("Payments", "Total: "+list.size(), true,
            new String[]{"Date","Invoice #","Customer","Amount","Method","Reference"},
            new float[]{12,15,22,15,12,24},
            list.stream().map(p -> new String[]{p.getPaymentDate().toString(),
                p.getInvoice().getInvoiceNumber(),p.getInvoice().getCustomer()!=null?p.getInvoice().getCustomer().getName():"",
                INR.format(p.getAmount()),nvl(p.getMethod()),nvl(p.getReference())}).collect(Collectors.toList()));
    }

    public static byte[] exportReport(String type, String from, String to, String asOf, ReportQueryService q) throws Exception {
        return switch (type) {
            case "outstanding" -> {
                var rows = q.outstanding(asOf);
                yield simpleListPdf("Outstanding Invoices","As of "+asOf,true,
                    new String[]{"Invoice #","Customer","Balance Due","Status","Days Overdue"},
                    new float[]{18,22,18,12,10},
                    rows.stream().map(r -> new String[]{s(r,"invoice_number"),s(r,"customer_name"),
                        fmtAmt(r.get("balance_due")),s(r,"status"),s(r,"days_overdue")}).collect(Collectors.toList()));
            }
            case "customer-sales" -> {
                var rows = q.customerSales(from,to);
                yield simpleListPdf("Customer Sales",from+" to "+to,true,
                    new String[]{"Customer","Phone","Invoices","Total Amount","Paid","Outstanding"},
                    new float[]{24,14,10,16,14,14},
                    rows.stream().map(r -> new String[]{s(r,"customer_name"),s(r,"phone"),s(r,"invoice_count"),
                        fmtAmt(r.get("total_amount")),fmtAmt(r.get("paid_amount")),fmtAmt(r.get("outstanding"))}).collect(Collectors.toList()));
            }
            case "product-sales" -> {
                var rows = q.productSales(from,to);
                yield simpleListPdf("Product Sales",from+" to "+to,true,
                    new String[]{"Product","Category","Total Qty","Total Sales","GST"},
                    new float[]{28,16,14,22,16},
                    rows.stream().map(r -> new String[]{s(r,"product_name"),s(r,"category"),s(r,"total_qty"),
                        fmtAmt(r.get("total_sales")),fmtAmt(r.get("gst_collected"))}).collect(Collectors.toList()));
            }
            default -> new byte[0];
        };
    }

    private static byte[] simpleListPdf(String title, String subtitle, boolean landscape,
                                         String[] cols, float[] widths, java.util.List<String[]> rows) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PageSize ps = landscape ? PageSize.A4.rotate() : PageSize.A4;
        Document doc = new Document(new PdfDocument(new PdfWriter(bos)), ps);
        doc.setMargins(36,40,36,40);
        PdfFont bold = bold(), reg = reg();

        // Header
        doc.add(new Paragraph("BuildMat Supplies — " + title + " | " + subtitle)
            .setFont(bold).setFontSize(14).setFontColor(BRAND).setMarginBottom(4));
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1)).setMarginBottom(8));

        // Table
        Table t = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth().setBorder(Border.NO_BORDER);
        for (String h : cols) t.addHeaderCell(new Cell().setBackgroundColor(BRAND).setPadding(6).setBorder(Border.NO_BORDER)
            .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE)));
        for (int i=0; i<rows.size(); i++) {
            boolean alt = i%2==0;
            for (String v : rows.get(i))
                t.addCell(new Cell().setBackgroundColor(alt?ALTBG:ColorConstants.WHITE).setPadding(5).setBorder(Border.NO_BORDER)
                    .add(new Paragraph(v==null?"":v).setFont(reg).setFontSize(8).setFontColor(DARK)));
        }
        doc.add(t);
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginTop(8));
        doc.add(new Paragraph("BuildMat Billing — Computer generated").setFont(reg).setFontSize(7).setFontColor(MIDGRAY).setTextAlignment(TextAlignment.CENTER));
        doc.close(); return bos.toByteArray();
    }

    private static void addItemCells(Table t, DeviceRgb bg, PdfFont bold, PdfFont reg, String... vals) {
        for (String v : vals) t.addCell(new Cell().setBackgroundColor(bg).setPadding(5).setBorder(Border.NO_BORDER)
            .add(new Paragraph(v).setFont(reg).setFontSize(8).setFontColor(DARK)));
    }

    private static void addTotRow(Table t, PdfFont reg, PdfFont bold, String label, String value, boolean highlight) {
        DeviceRgb bg = highlight ? BRAND : new DeviceRgb(255,255,255);
        DeviceRgb fg = highlight ? new DeviceRgb(255,255,255) : DARK;
        PdfFont font = highlight ? bold : reg;
        float size = highlight ? 12 : 9;
        t.addCell(new Cell().setBackgroundColor(bg).setPadding(7).setBorder(Border.NO_BORDER).add(new Paragraph(label).setFont(font).setFontSize(size).setFontColor(fg)));
        t.addCell(new Cell().setBackgroundColor(bg).setPadding(7).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).add(new Paragraph(value).setFont(font).setFontSize(size).setFontColor(fg)));
    }

    private static PdfFont bold() throws Exception { return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD); }
    private static PdfFont reg() throws Exception { return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA); }
    private static String nvl(String s) { return s==null?"":s; }
    private static String fmt(double v) { return v==(long)v?String.valueOf((long)v):String.valueOf(v); }
    private static String s(Map<String,Object> m, String k) { Object v=m.get(k); return v==null?"":v.toString(); }
    private static String fmtAmt(Object o) { if(o==null) return "0"; try{return INR.format(((Number)o).doubleValue());}catch(Exception e){return o.toString();} }
}
