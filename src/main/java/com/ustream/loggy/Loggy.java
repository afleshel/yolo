package com.ustream.loggy;

import com.ustream.loggy.config.ConfigException;
import com.ustream.loggy.config.ConfigUtils;
import com.ustream.loggy.handler.DataHandler;
import com.ustream.loggy.handler.FileHandler;
import com.ustream.loggy.module.ModuleFactory;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Map;

/**
 * @author bandesz <bandesz@ustream.tv>
 */
public class Loggy
{

    private final Options cliOptions = new Options();

    private Map<String, Object> config;

    private String configPath;

    private String filePath;

    private Boolean debug;

    private Boolean readWholeFile;

    private Boolean reopenFile;

    private FileHandler fileHandler;

    private DataHandler dataHandler;

    public Loggy()
    {
        buildCliOptions();
    }

    private void buildCliOptions()
    {
        cliOptions.addOption("help", false, "print this message");

        Option file = new Option("file", true, "path to logfile");
        file.setArgName("path");
        cliOptions.addOption(file);

        Option config = new Option("config", true, "path to config file");
        config.setArgName("path");
        cliOptions.addOption(config);

        cliOptions.addOption("debug", false, "print debugging information");

        cliOptions.addOption("whole", false, "tail file from the beginning");

        cliOptions.addOption("reopen", false, "reopen file between reading the chunks");
    }

    private void parseCliOptions(String[] args)
    {
        CommandLine cli;
        try
        {
            cli = new PosixParser().parse(cliOptions, args);
        }
        catch (ParseException exp)
        {
            exitWithError("Error: " + exp.getMessage(), true);
            return;
        }

        if (cli.hasOption("help"))
        {
            printHelp();
            System.exit(0);
        }

        configPath = cli.getOptionValue("config");

        if (null == configPath || configPath.isEmpty())
        {
            exitWithError("config parameter is missing!", true);
        }

        filePath = cli.getOptionValue("file");
        if (null == filePath || filePath.isEmpty())
        {
            exitWithError("file parameter is missing!", true);
        }

        debug = cli.hasOption("debug");

        readWholeFile = cli.hasOption("whole");

        reopenFile = cli.hasOption("reopen");
    }

    private void readConfig()
    {
        try
        {
            config = ConfigUtils.getConfigFromFile(configPath);
        }
        catch (IOException e)
        {
            exitWithError("Failed to open configuration file: " + e.getMessage(), false);
        }
    }

    private void initDataHandler()
    {
        dataHandler = new DataHandler(new ModuleFactory(), debug);

        try
        {
            Map<String, Object> processors = ConfigUtils.getObjectMap(config, "processors");
            for (String name : processors.keySet())
            {
                dataHandler.addProcessor(name, processors.get(name));
            }

            Map<String, Object> parsers = ConfigUtils.getObjectMap(config, "parsers");
            for (String name : parsers.keySet())
            {
                dataHandler.addParser(name, parsers.get(name));
            }
        }
        catch (ConfigException e)
        {
            exitWithError("Configuration error: " + e.getMessage(), false);
        }
        catch (Exception e)
        {
            exitWithError("Error: " + e.getMessage(), false);
        }
    }

    private void startFileHandler()
    {
        fileHandler = new FileHandler(dataHandler, filePath, readWholeFile, reopenFile, debug);

        fileHandler.start();
    }

    private void run(String[] args)
    {
        parseCliOptions(args);

        readConfig();

        initDataHandler();

        startFileHandler();
    }

    private void printHelp()
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("loggy", cliOptions);
    }

    private void exitWithError(String message, Boolean printHelp)
    {
        System.out.println(message);
        if (printHelp)
        {
            printHelp();
        }
        System.exit(1);
    }

    public static void main(String[] args)
    {
        Loggy loggy = new Loggy();
        loggy.run(args);
    }

}
