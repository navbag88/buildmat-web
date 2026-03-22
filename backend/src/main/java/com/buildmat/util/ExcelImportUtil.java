package com.buildmat.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.*;

public class ExcelImportUtil {

    public static List<Map<String,Object>> importCustomers(InputStream is) throws Exception {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int[] idx = detectCols(sheet.getRow(0), new String[]{"name","phone","mobile","email","address"});
            for (int r=1; r<=sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row==null||isEmpty(row)) continue;
                String name = str(row, idx[0]);
                if (name.isEmpty()) { result.add(Map.of("error","Row "+(r+1)+": Name required")); continue; }
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("name",name); m.put("phone",str(row,idx[1])); m.put("email",str(row,idx[3])); m.put("address",str(row,idx[4]));
                result.add(m);
            }
        }
        return result;
    }

    public static List<Map<String,Object>> importProducts(InputStream is) throws Exception {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int[] idx = detectCols(sheet.getRow(0), new String[]{"name","category","unit","price","stock","sgst","cgst"});
            for (int r=1; r<=sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row==null||isEmpty(row)) continue;
                String name = str(row, idx[0]);
                if (name.isEmpty()) { result.add(Map.of("error","Row "+(r+1)+": Name required")); continue; }
                double price = num(row, idx[3]);
                if (price<=0) { result.add(Map.of("error","Row "+(r+1)+" ("+name+"): Price must be > 0")); continue; }
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("name",name); m.put("category",str(row,idx[1])); m.put("unit",str(row,idx[2]).isEmpty()?"Unit":str(row,idx[2]));
                m.put("price",price); m.put("stockQty",num(row,idx[4])); m.put("sgstPercent",num(row,idx[5])); m.put("cgstPercent",num(row,idx[6]));
                result.add(m);
            }
        }
        return result;
    }

    private static int[] detectCols(Row hdr, String[] targets) {
        int[] idx = new int[targets.length]; Arrays.fill(idx,-1);
        if (hdr==null) return idx;
        for (Cell c : hdr) {
            String h = str(c).toLowerCase().replaceAll("[^a-z]","");
            for (int i=0; i<targets.length; i++) {
                if (h.contains(targets[i]) && idx[i]==-1) idx[i]=c.getColumnIndex();
            }
        }
        return idx;
    }

    private static String str(Row row, int col) {
        if (col<0||row==null) return "";
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return c==null?"":str(c);
    }

    private static String str(Cell c) {
        if (c==null) return "";
        return switch(c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> { double v=c.getNumericCellValue(); yield v==(long)v?String.valueOf((long)v):String.valueOf(v); }
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> "";
        };
    }

    private static double num(Row row, int col) {
        if (col<0||row==null) return 0;
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c==null) return 0;
        return switch(c.getCellType()) {
            case NUMERIC -> c.getNumericCellValue();
            case STRING -> { try { yield Double.parseDouble(c.getStringCellValue().trim()); } catch(Exception e) { yield 0; } }
            default -> 0;
        };
    }

    private static boolean isEmpty(Row row) {
        for (int i=row.getFirstCellNum(); i<row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c!=null && c.getCellType()!=CellType.BLANK && !str(c).isEmpty()) return false;
        }
        return true;
    }
}
