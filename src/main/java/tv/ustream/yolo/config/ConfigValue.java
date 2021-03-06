package tv.ustream.yolo.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author bandesz
 */
public class ConfigValue<T> implements IConfigEntry<T>
{

    private final boolean required;

    private final T defaultValue;

    private List<T> allowedValues = null;

    private final Class<T> type;

    private List<Class> allowedTypes = new ArrayList<Class>();

    private boolean configPatternAllowed = false;

    public ConfigValue(final Class<T> type)
    {
        this(type, true, null);
    }

    public ConfigValue(final Class<T> type, final boolean required, final T defaultValue)
    {
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.allowedTypes.add(type);
    }

    public ConfigValue setAllowedValues(final List<T> allowedValues)
    {
        this.allowedValues = allowedValues;

        return this;
    }

    public ConfigValue setAllowedTypes(final List<Class> types)
    {
        this.allowedTypes = types;

        return this;
    }

    public ConfigValue allowConfigPattern()
    {
        configPatternAllowed = true;

        return this;
    }

    public T parse(final String name, final Object value) throws ConfigException
    {
        if (!isValueValid(value))
        {
            throw new ConfigException(
                    name + " field is missing or invalid, value definition: " + this.getDescription("") + ""
            );
        }

        return !isEmpty(value) ? type.cast(value) : defaultValue;
    }

    private boolean isEmpty(Object value)
    {
        if (value == null)
        {
            return true;
        }
        if (value instanceof String && "".equals(value))
        {
            return true;
        }
        if (value instanceof Collection && ((Collection) value).isEmpty())
        {
            return true;
        }

        if (value instanceof Map && ((Map) value).isEmpty())
        {
            return true;
        }

        return false;
    }

    private boolean isValueValid(Object value)
    {
        if (required && isEmpty(value))
        {
            return false;
        }
        else if (value == null)
        {
            return true;
        }

        return isTypeAllowed(value) && (allowedValues == null || allowedValues.contains(value));
    }

    private boolean isTypeAllowed(Object value)
    {
        if (value instanceof String && ConfigPattern.applicable(value) && !configPatternAllowed)
        {
            return false;
        }
        for (Class clazz : allowedTypes)
        {
            if (clazz.isInstance(value))
            {
                return true;
            }
        }
        return false;
    }

    public String getDescription(String indent)
    {
        String types = "";
        for (int i = 0; i < allowedTypes.size(); i++)
        {
            types = types + (i > 0 ? "|" : "") + allowedTypes.get(i).getSimpleName();
        }

        return String.format(
                "%s%s%s%s%s%n",
                types,
                required ? ", required" : "",
                !required && !isEmpty(defaultValue) ? ", default: " + defaultValue : "",
                allowedValues != null ? ", allowed values: " + allowedValues : "",
                configPatternAllowed ? ", pattern allowed" : ""
        );
    }

    public static ConfigValue<String> createString()
    {
        return new ConfigValue<String>(String.class);
    }

}
