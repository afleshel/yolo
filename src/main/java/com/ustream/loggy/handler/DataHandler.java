package com.ustream.loggy.handler;

import com.ustream.loggy.config.ConfigException;
import com.ustream.loggy.config.ConfigPattern;
import com.ustream.loggy.config.ConfigUtils;
import com.ustream.loggy.module.ModuleFactory;
import com.ustream.loggy.module.parser.IParser;
import com.ustream.loggy.module.processor.IProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bandesz <bandesz@ustream.tv>
 */
public class DataHandler implements ILineHandler
{

    private ModuleFactory moduleFactory;

    private Boolean debug;

    private final Map<String, IParser> parsers = new HashMap<String, IParser>();

    private final Map<String, Map<String, Object>> processorParams = new HashMap<String, Map<String, Object>>();

    private final Map<String, IProcessor> processors = new HashMap<String, IProcessor>();

    private final Map<String, String> transitions = new HashMap<String, String>();

    public DataHandler(ModuleFactory moduleFactory, Boolean debug)
    {
        this.moduleFactory = moduleFactory;
        this.debug = debug;
    }

    public void addProcessor(String name, Object data) throws Exception
    {
        Map<String, Object> config = ConfigUtils.castObjectMap(data);
        IProcessor processor = moduleFactory.create((String) config.get("class"), config, debug);
        processors.put(name, processor);
        if (debug)
        {
            System.out.format("Adding %s processor: %s\n", name, data);
        }
    }

    public void addParser(String name, Object data) throws Exception
    {
        Map<String, Object> config = ConfigUtils.castObjectMap(data);
        IParser parser = moduleFactory.create((String) config.get("class"), config, debug);
        parsers.put(name, parser);

        if (debug)
        {
            System.out.format("Adding %s parser: %s\n", name, data);
        }

        String processorName = (String) config.get("processor");

        if (null == processorName || !processors.containsKey(processorName))
        {
            throw new ConfigException("Config error in " + name + " parser: processor missing or does not exist");
        }

        Map<String, Object> params = ConfigUtils.castObjectMap(config.get("processorParams"));

        if (params != null)
        {
            processorParams.put(name, ConfigPattern.processMap(params));

            processors.get(processorName).validateProcessorParams(parser.getOutputParameters(), params);
        }

        transitions.put(name, processorName);
    }

    public void handle(String line)
    {
        Boolean match = false;
        for (String parserName : parsers.keySet())
        {
            if (!match || parsers.get(parserName).runAlways())
            {
                Map<String, String> parserParams = parsers.get(parserName).parse(line);
                if (parserParams != null)
                {
                    match = true;
                    processors.get(transitions.get(parserName)).process(parserParams, processorParams.get(parserName));
                }
            }
        }
    }

}
