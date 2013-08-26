package tv.ustream.loggy;

import com.google.gson.Gson;
import org.apache.commons.cli.*;
import tv.ustream.loggy.config.ConfigException;
import tv.ustream.loggy.config.ConfigGroup;
import tv.ustream.loggy.handler.FileHandler;
import tv.ustream.loggy.module.ModuleChain;
import tv.ustream.loggy.module.ModuleFactory;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * @author bandesz
 */
public class Loggy
{

    private final Options cliOptions = new Options();

    private Map<String, Object> config;

    private String configPath;

    private String filePath;

    private Boolean debug = false;

    private Boolean readWholeFile;

    private Boolean reopenFile;

    private ModuleChain moduleChain;

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

        cliOptions.addOption("listModules", false, "list available modules");
    }

    private void parseCliOptions(String[] args) throws Exception
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

        if (cli.hasOption("listModules"))
        {
            ModuleFactory.printAvailableModules();
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

    private ConfigGroup getMainConfig()
    {
        ConfigGroup config = new ConfigGroup();
        config.addConfigValue("processors", Map.class);
        config.addConfigValue("parsers", Map.class);
        return config;
    }

    private void readConfig() throws ConfigException
    {
        try
        {
            config = (Map<String, Object>) new Gson().fromJson(new FileReader(configPath), Map.class);
        }
        catch (IOException e)
        {
            exitWithError("Failed to open configuration file: " + e.getMessage(), false);
        }

        getMainConfig().parseValues("[root]", config);
    }

    @SuppressWarnings("unchecked")
    private void initModuleChain() throws Exception
    {
        moduleChain = new ModuleChain(new ModuleFactory(), debug);

        try
        {
            Map<String, Object> processors = (Map<String, Object>) config.get("processors");
            for (String name : processors.keySet())
            {
                moduleChain.addProcessor(name, (Map<String, Object>) processors.get(name));
            }

            Map<String, Object> parsers = (Map<String, Object>) config.get("parsers");
            for (String name : parsers.keySet())
            {
                moduleChain.addParser(name, (Map<String, Object>) parsers.get(name));
            }
        }
        catch (ConfigException e)
        {
            exitWithError(e.getMessage(), false);
        }
    }

    private void startFileHandler()
    {
        FileHandler fileHandler = new FileHandler(moduleChain, filePath, readWholeFile, reopenFile, debug);

        fileHandler.start();
    }

    private void run(String[] args)
    {
        try
        {
            parseCliOptions(args);

            readConfig();

            initModuleChain();

            startFileHandler();
        }
        catch (Exception e)
        {
            if (!debug && !e.getMessage().isEmpty())
            {
                System.out.println(e.getMessage());
            }
            else
            {
                e.printStackTrace();
            }
        }
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
