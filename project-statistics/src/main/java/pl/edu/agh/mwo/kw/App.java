package pl.edu.agh.mwo.kw;

import org.apache.commons.cli.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class App
{
    public static void main( String[] args ){

        Options options = generateOptions();
        boolean guard = true;
        while (guard) {
            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();
            CommandLine cmd;

            try{
                cmd = parser.parse(options, args);
                String inputPath = cmd.getOptionValue('i');

                if(!cmd.hasOption('i')) {
                    System.out.println("Missing required input option with correct path");
                    formatter.printHelp("statistics", options);
                    System.exit(1);
                }

                if((!cmd.hasOption('d') && !cmd.hasOption('e') && !cmd.hasOption('m'))){
                    System.out.println("Missing ranking type option");
                    formatter.printHelp("statistics", options);
                    System.exit(1);
                }

                List<Ranking> rankings = new LinkedList<>();
                DataLoader loader = new XLSDataLoader();
                Set<Employee> employees = loader.loadDataFromFiles(inputPath);

                if (employees.isEmpty()) {
                    System.out.println("ERROR: Incorrect path or no *.xls files in selected folder.");
                    System.exit(1);
                }
                if (cmd.hasOption("day")) {
                    rankings.add(new RankingOfWorkingDays(employees));
                }
                if (cmd.hasOption("month")) {
                    rankings.add(new RankingOfMonths(employees));
                }
                if (cmd.hasOption("employee")) {
                    rankings.add(new RankingOfEmployees(employees));
                }

                rankings.forEach(Ranking::printRanking);

                if(cmd.hasOption("export") && !cmd.getOptionValue("x").isEmpty()) {
                    try {
                        Path exportPathWithFileName = Paths.get(cmd.getOptionValue("x"));
                        Path exportPath = exportPathWithFileName.getRoot();
                        if (Files.isWritable(exportPath)) {
                            HSSFWorkbook workbook = new HSSFWorkbook();
                            for (Ranking ranking : rankings) {
                                workbook = ranking.exportRanking(workbook);
                            }
                            writeXLS(workbook, exportPathWithFileName.toString());
                            }
                        else {
                            printError();
                            System.exit(1);
                        }
                    }
                    catch (InvalidPathException ex) {
                        System.out.println(ex.getMessage());
                        System.exit(1);
                    }
                }
                guard = false;
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
                formatter.printHelp("statistics", options);
                System.exit(1);
            }
        }
    }

    private static Options generateOptions(){
        final Options options = new Options();
        Option input = new Option("i", "input", true, "Input files path");
        //input.setRequired(true);
        options.addOption(input);
        Option export = new Option("x", "export", true, "Export file path");
        input.setRequired(false);
        options.addOption(export);
        options.addOption("e", "employee", false, "Print employees by working hours in projects")
                .addOption("m", "month", false, "Print ranking of working hours in months")
                .addOption("h", "help", false, "Display help information")
                .addOption("d", "day", false, "Print ten the most busiest days ranking");
        return options;
    }

    private static void printError(){
        System.out.println("ERROR: Wrong path. Try again.");
    }

    private static void writeXLS(HSSFWorkbook workbook, String path){
        try {
            FileOutputStream out =
                    new FileOutputStream(new File(path));
            workbook.write(out);
            out.close();
            System.out.println("Excel file written successfully.");

        } catch (IOException e) {
            System.out.println("Excel file writing failed.");
        }
    }

}
