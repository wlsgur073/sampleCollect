package com.abilsys.oa.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            String filePath = "D:/file/chs21_all.csv";
//            List<Map<String, Object>> excelData = readExcelByXSSF(filePath);
//            List<Map<String, Object>> excelData = readExcelByStream(filePath);

            Map<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("filePath", "D:/file/test.csv");
            tmpMap.put("dataBeginRow", 1);
            tmpMap.put("dataBeginColumn", 1);

            List<Map<String, Object>> csvData  = readCsvFile(tmpMap);

            // 읽어온 데이터 확인
            for (Map<String, Object> row : csvData ) {
                System.out.println(row);
            }


        } catch (IOException e) {
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

    public static List<Map<String, Object>> readExcelByXSSF(String filePath) throws IOException {
        List<Map<String, Object>> dataList = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트를 읽어옴

            // 헤더 추출
            Row headerRow = sheet.getRow(0);
            int columnCount = headerRow.getPhysicalNumberOfCells();
            List<String> headers = new ArrayList<>();
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                Cell cell = headerRow.getCell(colIndex);
                headers.add(cell.getStringCellValue());
            }

            // 데이터 추출
            int rowCount = sheet.getPhysicalNumberOfRows();
            for (int rowIndex = 1; rowIndex < rowCount; rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                Map<String, Object> rowData = new HashMap<>();

                for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                    Cell cell = dataRow.getCell(colIndex);
                    String header = headers.get(colIndex);

                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.put(header, cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            rowData.put(header, cell.getNumericCellValue());
                            break;
                        // 다른 데이터 타입에 대한 처리도 추가 가능
                    }
                }

                dataList.add(rowData);
            }
        }

        return dataList;
    }

    public List<Map<String, Object>> readCsvFile(Map<String, Object> params) throws IOException {
        String filePath = String.valueOf(params.get("filePath"));
        int dataBeginRow = (int) params.get("dataBeginRow");
        int dataBeginColumn = (int) params.get("dataBeginColumn");

        List<Map<String, Object>> result = new ArrayList<>();

        // CSVFormat.DEFAULT.withHeader() 대신에 withSkipHeaderRecord(true)를 사용하여 헤더를 스킵합니다.
        CSVFormat csvFormat = CSVFormat.newFormat(',').withQuote('"').withRecordSeparator('\n').withHeader();

        try (CSVParser csvParser = new CSVParser(new FileReader(filePath), csvFormat)) {
            for (CSVRecord csvRecord : csvParser.getRecords()) {
                // Skip rows before dataBeginRow
                if (csvRecord.getRecordNumber() < dataBeginRow) {
                    continue;
                }

                Map<String, Object> rowMap = new HashMap<>();
                int columnIndex = dataBeginColumn - 1; // Adjust for 0-based indexing

                for (String header : csvParser.getHeaderMap().keySet()) {
                    if (columnIndex < csvRecord.size()) {
                        rowMap.put(header, csvRecord.get(columnIndex));
                    } else {
                        rowMap.put(header, null); // Handle cases where the column index is out of bounds
                    }
                    columnIndex++;
                }

                result.add(rowMap);
            }
        }

        return result;
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
                return cell.getNumericCellValue();
            // 다른 데이터 타입에 대한 처리도 추가 가능
            default:
                return null;
        }
    }

}
