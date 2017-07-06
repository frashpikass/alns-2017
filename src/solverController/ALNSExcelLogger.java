/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solverController;

import java.awt.Font;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * This class allows to log the output of the ALNS algorithm to an Excel (XLSX)
 * file.
 *
 * @author Frash
 */
public class ALNSExcelLogger {

    /**
     * Path to the xlsx file which will store the results of our ALNS run
     */
    private String filePath;

    /**
     * Name of the current instance
     */
    private final String instanceName;

    /**
     * A <code>FileOutputStream</code> linked to the output file
     */
    private FileOutputStream fos;

    /**
     * Index of the next row to write in the current sheet
     */
    private int currentRowNumber;

    /**
     * Object that stores the current Excel workbook
     */
    private Workbook wb;

    /**
     * Object that stores the current Excel sheet
     */
    private Sheet sheet;

    /**
     * Style for header cells.
     */
    private CellStyle headerStyle;

    /**
     * Style for data cells
     */
    private CellStyle dataCellStyle;

    /**
     * Constant which holds the values of headers for the ALNS log file
     */
    private final static String[] HEADERS = {
        "Segment", "Iteration", "Time",
        "Destroy Heuristic", "DWeight", "Repair Heuristic", "RWeight", "Repaired?",
        "Temperature",
        "Sim. Ann. Barrier",
        "q",
        "xOld", "xOldObj",
        "xNew", "xNewObj", "Accepted?", "Worse but accepted?", "Infeasible & Discarded?",
        "xBest", "xBestObj",
        "xBestInSegments", "xBestInSegmentsObj",
        "Updated Cluster Roulette", "Cluster Roulette avg. p", "Nerf occurrences",
        "Comment"
    };

    /**
     * Constructor for an ALNS Excel Logger object.
     *
     * @param filePath path to the output file
     * @param instanceName name of the current instance
     * @throws FileNotFoundException if the path to the output file is not found
     */
    public ALNSExcelLogger(String filePath, String instanceName) throws FileNotFoundException, IOException {
        // Create/open the Excel file
        this.filePath = filePath;
        this.instanceName = instanceName;
        fos = new FileOutputStream(filePath);
        wb = new XSSFWorkbook();
        sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(instanceName));

        // Setup the current row number to the first row
        currentRowNumber = 0;

        // Setup cell styles
        headerStyle = wb.createCellStyle();
        dataCellStyle = wb.createCellStyle();

        // Create a bold font for the header and a normal font for data cells
        org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
        //boldFont.setFontName("Sans-serif");
        boldFont.setBold(true);
        headerStyle.setFont(boldFont);

//        org.apache.poi.ss.usermodel.Font normalFont = wb.createFont();
//        normalFont.setFontName("Sans-serif");
//        dataCellStyle.setFont(normalFont);
        // Setup border style for header
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        // Setup border style for other cells
        dataCellStyle.setBorderLeft(BorderStyle.THIN);
        dataCellStyle.setBorderRight(BorderStyle.THIN);
        dataCellStyle.setBorderTop(BorderStyle.THIN);
        dataCellStyle.setBorderBottom(BorderStyle.THIN);
        // Setup font styles

        // Set background color, bold font, and center alignment for header style
        headerStyle.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(boldFont);
        //headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Write the header
        writeHeader();
    }

    /**
     * Write a logfile header to the Excel file.
     */
    private void writeHeader() throws FileNotFoundException, IOException {
        Row r = sheet.createRow(currentRowNumber);

        // Column number
        int i = 0;

        // Write all header columns in the header row
        for (String header : HEADERS) {
            CellUtil.createCell(r, i, header, headerStyle);
            i++;
        }

        // Freeze the first row for better readability
        sheet.createFreezePane(0, 1);

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            wb.write(fileOut);
        }

        // Increase the row number
        currentRowNumber++;
    }

    /**
     * Write a row.
     *
     * @param logLine an array of strings to put in a row
     * @throws FileNotFoundException if the file is not found (duh)
     * @throws IOException if there are problems with the file
     */
    public void writeRow(String[] logLine) throws FileNotFoundException, IOException {
        Row r = sheet.createRow(currentRowNumber);

        // Column number
        int i = 0;

        // Write all header columns in the header row
        for (String data : logLine) {
            CellUtil.createCell(r, i, data, dataCellStyle);
            i++;
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            wb.write(fileOut);
        }

        // Increase the row number
        currentRowNumber++;
    }

    /**
     * Resize columns to fit all contents
     */
    private void autoSizeColumns() {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Closes the FileOutputStream gracefully by making sure every buffered
     * write is physically written to disk.
     *
     * @throws IOException if something goes wrong
     */
    public void close() throws IOException {
        autoSizeColumns();
        fos.flush();
        fos.close();
    }

    /**
     * Main method for testing
     *
     * @param args unused
     * @throws FileNotFoundException if it's impossible to open the file
     * @throws IOException if opening/closing the file goes awry
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        ALNSExcelLogger logger = new ALNSExcelLogger("excelLoggerTest.xlsx", "pizza");
        logger.close();
    }
}
