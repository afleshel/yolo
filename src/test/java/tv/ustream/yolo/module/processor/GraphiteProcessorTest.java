package tv.ustream.yolo.module.processor;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tv.ustream.yolo.client.GraphiteClient;
import tv.ustream.yolo.config.ConfigException;
import tv.ustream.yolo.config.ConfigPattern;
import tv.ustream.yolo.module.ModuleFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author bandesz
 */
public class GraphiteProcessorTest
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private GraphiteClient graphiteClient;

    private GraphiteProcessor processor;

    @Before
    public void setUp() throws ConfigException
    {
        graphiteClient = mock(GraphiteClient.class);

        processor = createProcessorMock("host1", 1234, "");
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void emptyHostShouldThrowException() throws ConfigException
    {
        thrown.expect(ConfigException.class);

        createProcessor(null, 1234);
    }

    @Test
    public void processShouldSendMetrics()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();

        processor.process(parserOutput, createProcessParams("key", 5D));

        verify(graphiteClient).sendMetrics("key", 5D);
    }

    @Test
    public void processShouldAddParamsToKey()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("p1", "v1");

        ConfigPattern key = new ConfigPattern("some.#p1#.key");

        processor.process(parserOutput, createProcessParams(key, 5D));

        verify(graphiteClient).sendMetrics("some.v1.key", 5D);
    }

    @Test
    public void processShouldUseValueFromParameters()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("v1", "5");

        ConfigPattern value = new ConfigPattern("#v1#");

        processor.process(parserOutput, createProcessParams("key", value));

        verify(graphiteClient).sendMetrics("key", 5D);
    }

    @Test
    public void processShouldUseDynamicKeyAndValue()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("p1", "v1");
        parserOutput.put("v1", "5");

        ConfigPattern key = new ConfigPattern("some.#p1#.key");
        ConfigPattern value = new ConfigPattern("#v1#");

        processor.process(parserOutput, createProcessParams(key, value));

        verify(graphiteClient).sendMetrics("some.v1.key", 5D);
    }

    @Test
    public void processShouldSendMultipleKeys()
    {
        Map<String, Object> params = new HashMap<String, Object>();

        Map<String, Object> key1 = new HashMap<String, Object>();
        key1.put("type", "gauge");
        key1.put("key", new ConfigPattern("some.#p1#.key"));
        key1.put("value", 1D);
        key1.put("multiplier", 1D);

        Map<String, Object> key2 = new HashMap<String, Object>();
        key2.put("type", "timer");
        key2.put("key", new ConfigPattern("someother.#p1#.key"));
        key2.put("value", 2D);
        key2.put("multiplier", 1D);

        params.put("keys", Arrays.<Map>asList(key1, key2));

        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("p1", "v1");

        processor.process(parserOutput, params);

        verify(graphiteClient).sendMetrics("some.v1.key", 1D);
        verify(graphiteClient).sendMetrics("someother.v1.key", 2D);
    }

    @Test
    public void processShouldUseMultiplier()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();

        processor.process(parserOutput, createProcessParams("key", 5D, 10D, null));

        verify(graphiteClient).sendMetrics("key", 50D);
    }

    @Test
    public void processShouldUseCustomTimestamp()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("ts", "1234567890");

        processor.process(parserOutput, createProcessParams("key", 5D, 1D, new ConfigPattern("#ts#")));

        verify(graphiteClient).sendMetrics("key", 5D, 1234567890);
    }

    @Test
    public void processShouldHandleByteValues()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("v1", "5M");

        ConfigPattern value = new ConfigPattern("#v1#");

        processor.process(parserOutput, createProcessParams("key", value));

        verify(graphiteClient).sendMetrics("key", 5D * 1024 * 1024);
    }

    @Test
    public void processShouldNotSendWhenKeyParamIsMissing()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("p2", "key");

        ConfigPattern key = new ConfigPattern("some.#p1#.key");

        processor.process(parserOutput, createProcessParams(key, 1D));

        verifyNoMoreInteractions(graphiteClient);
    }

    @Test
    public void processShouldNotSendWhenValueParamIsMissing()
    {
        Map<String, Object> parserOutput = new HashMap<String, Object>();
        parserOutput.put("v2", "5M");

        ConfigPattern value = new ConfigPattern("#v1#");

        processor.process(parserOutput, createProcessParams("key", value));

        verifyNoMoreInteractions(graphiteClient);
    }

    @Test
    public void stopShouldStopClient()
    {
        processor.stop();

        verify(graphiteClient).stop();
    }

    private Map<String, Object> createProcessParams(Object key, Object value)
    {
        return createProcessParams(key, value, 1D, null);
    }

    private Map<String, Object> createProcessParams(Object key, Object value, Double multiplier, Object timestamp)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> key1 = new HashMap<String, Object>();
        key1.put("key", key);
        key1.put("value", value);
        key1.put("multiplier", multiplier);
        key1.put("timestamp", timestamp);
        params.put("keys", Arrays.<Map>asList(key1));
        return params;
    }

    private GraphiteProcessor createProcessor(String host, Integer port) throws ConfigException
    {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("class", GraphiteProcessor.class.getCanonicalName());
        config.put("host", host);
        config.put("port", port.doubleValue());
        config.put("flushTimeMs", 1000);

        return (GraphiteProcessor) new ModuleFactory().createProcessor("x", config);
    }

    private GraphiteProcessor createProcessorMock(String host, Integer port, String prefix) throws ConfigException
    {
        GraphiteProcessor processor = new GraphiteProcessor()
        {
            @Override
            protected GraphiteClient createClient(String host, int port, long flushTimeMs, String prefix)
            {
                return graphiteClient;
            }
        };

        Map<String, Object> config = new HashMap<String, Object>();
        config.put("class", GraphiteProcessor.class.getCanonicalName());
        config.put("host", host);
        config.put("port", port.doubleValue());
        config.put("flushTimeMs", 1000);
        config.put("prefix", prefix);

        processor.setUpModule(config);

        return processor;
    }

}
