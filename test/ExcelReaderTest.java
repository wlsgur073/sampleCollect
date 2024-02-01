package com.abilsys.oa.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class ExcelReaderTest {

    long startTime;
    long endTime;

    @Before
    public void before() {
        startTime = System.nanoTime();
    }

    @Test
    public void printTest() {
        try {

//            String filePath = "D:/file/test.xlsx";
//            String filePath = "D:/file/chs21_all.csv";
//            String filePath = "D:/file/[지역사회건강조사] 테이블 정보.xls";

//            List<Map<String, Object>> excelData = readExcelByStream(filePath);

            Map<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("filePath", "D:\\file\\배치관리 테스트\\배치용파일.xlsx");
            tmpMap.put("dataBeginRow", 0);
            tmpMap.put("dataBeginColumn", 0);

            List<Map<String, Object>> excelData = readExcel(tmpMap);
//            List<Map<String, Object>> csvData  = readCSV(tmpMap);

            System.out.println("csvData.size() = " + excelData.size());
            // 읽어온 데이터 확인
            for (Map<String, Object> row : excelData ) {
                System.out.println("row = " + row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void after() {
        endTime = System.nanoTime();

        // 경과 시간 계산 (나노초 단위)
        long elapsedTime = endTime - startTime;
        // 경과 시간 출력 (밀리초 단위로 변환)
        System.out.println("Elapsed Time: " + elapsedTime / 1_000_000 + " milliseconds");

    }

    public static List<Map<String, Object>> readExcel(Map<String, Object> params) throws IOException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String filePath = String.valueOf(params.get("filePath"));

        // null 이거나 0의 값을 가지고 있다면 1로 설정
//        int dataBeginRow = (int) params.get("dataBeginRow");
//        dataBeginRow = (0 >= dataBeginRow) ? 1 : dataBeginRow;
//        int dataBeginColumn = (int) params.get("dataBeginColumn");
//        dataBeginColumn = (0 >= dataBeginColumn) ? 1 : dataBeginColumn - 1;

        int dataBeginRow = (params.containsKey("dataBeginRow")) ? (int) params.get("dataBeginRow") - 1 : 0;
        dataBeginRow = Math.max(dataBeginRow, 0);

        int dataBeginColumn = (params.containsKey("dataBeginColumn")) ? (int) params.get("dataBeginColumn") - 1 : 0;
        dataBeginColumn = Math.max(dataBeginColumn, 0);

        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            Workbook workbook;
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream); // .xlsx 파일
            } else if (filePath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fileInputStream); // .xls 파일
            } else {
                throw new IllegalArgumentException("Unsupported file format");
            }

            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트를 읽어옴

            // 헤더 추출
            Row headerRow = sheet.getRow(0);
            int columnCount = headerRow.getPhysicalNumberOfCells();
            List<String> headers = new ArrayList<>();
            for (int colIndex = dataBeginColumn; colIndex < columnCount; colIndex++) {
                Cell cell = headerRow.getCell(colIndex);
                headers.add(cell.getStringCellValue());
            }

            // 데이터 추출
            int rowCount = sheet.getPhysicalNumberOfRows();
//            int startRowIndex = (dataBeginRow == 1) ? dataBeginRow + 1 : dataBeginRow;
            for (int rowIndex = dataBeginRow + 1; rowIndex < rowCount; rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                Map<String, Object> rowData = new HashMap<>();

                for (int colIndex = dataBeginColumn; colIndex < columnCount; colIndex++) {
                    Cell cell = dataRow.getCell(colIndex);
                    String header = headers.get(colIndex - dataBeginColumn);

                    rowData.put(header, getCellValue(cell));
                }

                dataList.add(rowData);
            }
        }

        return dataList;
    }

    public List<Map<String, Object>> readCSV(Map<String, Object> params) throws IOException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String filePath = String.valueOf(params.get("filePath"));

        int dataBeginRow = (int) params.get("dataBeginRow");
        dataBeginRow = (0 >= dataBeginRow) ? 1 : dataBeginRow;
        int dataBeginColumn = (int) params.get("dataBeginColumn");
        dataBeginColumn = (0 >= dataBeginColumn) ? 0 : dataBeginColumn - 1;

        CSVFormat csvFormat = CSVFormat.newFormat(',').withQuote('"').withRecordSeparator('\n').withHeader();
        try (CSVParser csvParser = new CSVParser(new FileReader(filePath), csvFormat)) {
            for (CSVRecord csvRecord : csvParser.getRecords()) {
                // Skip rows before dataBeginRow
//                System.out.println("csvRecord.getRecordNumber() = " + csvRecord.getRecordNumber());
                if (csvRecord.getRecordNumber() < dataBeginRow - 1) {
                    continue;
                }

                Map<String, Object> rowMap = new HashMap<>();
                int columnIndex = dataBeginColumn; // Adjust for 0-based indexing

                Set<String> headerSet = csvParser.getHeaderMap().keySet();
                List<String> headerList = new ArrayList<>(headerSet);
                for (int i = columnIndex; i < headerList.size(); i++) {
                    String header = headerList.get(i);
                    rowMap.put(header, convertToNumeric(csvRecord.get(columnIndex)));
                    columnIndex++;
                }

//                for (String header : csvParser.getHeaderMap().keySet()) {
//                    if (columnIndex < csvRecord.size()) {
////                        rowMap.put(header, csvRecord.get(columnIndex));
//                        rowMap.put(header, convertToNumeric(csvRecord.get(columnIndex)));
//                    }
//                    columnIndex++;
//                }

                dataList.add(rowMap);
            }
        }

        return dataList;
    }

    public static List<Map<String, Object>> readExcelByStream(String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(filePath)))) {
            Sheet sheet = workbook.getSheetAt(0);

            // 헤더 추출
            Row headerRow = sheet.getRow(0);
            List<String> headers = StreamSupport.stream(headerRow.spliterator(), false)
                    .map(Cell::getStringCellValue)
                    .collect(Collectors.toList());

            // 데이터 추출
            int rowCount = sheet.getPhysicalNumberOfRows();
            return IntStream.range(1, rowCount)
                    .parallel() // 멀티스레딩 적용
                    .mapToObj(rowIndex -> {
                        Row dataRow = sheet.getRow(rowIndex);
                        return extractRowData(headers, dataRow);
                    })
                    .collect(Collectors.toList());
        }
    }

    private static Map<String, Object> extractRowData(List<String> headers, Row dataRow) {
        return IntStream.range(0, headers.size())
                .boxed()
                .collect(Collectors.toMap(
                        headers::get,
                        colIndex -> {
                            Cell cell = dataRow.getCell(colIndex);
                            return getCellValue(cell);
                        }
                ));
    }
    private static Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date dateValue = cell.getDateCellValue();
                    return formatDate(dateValue);
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Check if the value is an integer
                    long round = Math.round(numericValue);
                    if (numericValue == Math.round(numericValue)) {
                        return (int) numericValue; // If it's an integer, return as int
                    } else {
                        return numericValue; // If it's a double, return as double
                    }
                }
            case FORMULA:
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                DataFormatter dataFormatter = new DataFormatter();
                return dataFormatter.formatCellValue(evaluator.evaluateInCell(cell));
            case BOOLEAN:
                return String.valueOf(cell.getNumericCellValue());
            case BLANK:
                return "";
            case ERROR:
                return String.valueOf(cell.getErrorCellValue());
            default:
                return null;
        }
    }

    public static Object convertToNumeric(String value) {
        try {
            // 괄호로 둘러싸인 형태이고 숫자로 시작하는지 확인
            if (value.matches("\\(\\d+(\\.\\d+)?\\)")) {
                // 괄호 안의 숫자 부분만 추출하고 부호를 반전시킴
                String numericPart = value.replaceAll("\\D", "");
                return Double.parseDouble(numericPart) * -1;
            } else {
                // 쉼표를 제거하고 숫자로 변환
                String numericValue = value.replaceAll(",", "");
                if (numericValue.contains(".")) {
                    return Double.parseDouble(numericValue);
                } else {
                    return Integer.parseInt(numericValue);
                }
            }
        } catch (NumberFormatException e) {
            // 숫자 또는 날짜로 변환 실패 시 원래 문자열 반환
            return value;
        }
    }

    private static String formatDate(Date date) {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            return sdf.format(date);
        } else {
            return ""; // 값이 없는 경우 빈 문자열 반환
        }
    }

    @Test
    public void test() {
        String filePath = "/app7/was/icassFile/handling/classfiles/20240201/18d6353586e3.xlsx"; // 실제 파일 경로로 변경해야 함
        File file = new File(filePath);
        boolean exists = file.exists();
        System.out.println("파일이 존재하는가? " + exists);
    }
}
