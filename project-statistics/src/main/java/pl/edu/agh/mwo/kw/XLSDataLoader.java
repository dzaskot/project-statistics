package pl.edu.agh.mwo.kw;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class XLSDataLoader implements DataLoader{

    @Override
    public Set<Employee> loadDataFromFiles(String folderPath) {
        List<File> files = loadFilesFromPath(folderPath);
        Map<String, Employee> employeesMap = new HashMap<>();

        for(File file: files){
            if(file.length() != 0) {
                String name = FilenameUtils.removeExtension(file.getName());
                Set<Report> reports = readXLSFile(file);
                employeesMap.computeIfAbsent(name, key ->
                        new Employee(name))
                        .getReports().addAll(reports);
            }
        }

        return new HashSet<>(employeesMap.values());
    }

    private List<File> loadFilesFromPath(String folderPath){
        List<File> files = new ArrayList<>();
        File directory = new File(folderPath);
        if(directory.exists()) {
            File[] fileArray = directory.listFiles();
            if (fileArray != null)
                for (File file : fileArray) {
                    if (file.isFile() && FilenameUtils.isExtension(file.getAbsolutePath(), "xls")) {
                        files.add(file);
                    } else if (file.isDirectory()) {
                        files.addAll(loadFilesFromPath(file.getAbsolutePath()));
                    }
                }
        }
        return files;
    }

    private Set<Report> readXLSFile(File file) {
        Set<Report> reports = new HashSet<>();

        try{
            InputStream xslStream = new FileInputStream(file);
            POIFSFileSystem fs = new POIFSFileSystem(xslStream);
                HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true);
                 //Workbook workbook = WorkbookFactory.create(xslStream);
                for (Sheet sheet : wb) {
                    // Iterator<Row> rowIterator = sheet.rowIterator();
                    // rowIterator.next(); //skip first row
                    for (Row row : sheet) {
                        if (row.getRowNum() == 0) continue;
                        Cell dataCell = row.getCell(0);
                        Cell hoursCell = row.getCell(2);

                        if (dataCell == null || hoursCell == null
                                || dataCell.getCellType() == CellType.BLANK
                                || hoursCell.getCellType() == CellType.BLANK) {
                            continue;
                        }
                        //DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        LocalDate date;
                        try {
                            date = row.getCell(0).getLocalDateTimeCellValue().toLocalDate();
                            //date = LocalDateTime.parse(row.getCell(0).getStringCellValue(), dateFormatter);
                        } catch (DateTimeParseException ex) {
                            continue;
                        }

                        String task = row.getCell(1).getStringCellValue();

                        double workingHours;
                        try {
                            workingHours = row.getCell(2).getNumericCellValue();
                        } catch (IllegalStateException | NumberFormatException ex) {
                            continue;
                        }
                        Report report = new Report();
                        report.setProject(sheet.getSheetName());
                        report.setDate(date);
                        report.setTask(task);
                        report.setWorkingHours(workingHours);
                        reports.add(report);
                    }
                    wb.close();
                    xslStream.close();
                }
            }
            catch (IOException e) {
                //e.printStackTrace();
            }
            return reports;
        }
    }
