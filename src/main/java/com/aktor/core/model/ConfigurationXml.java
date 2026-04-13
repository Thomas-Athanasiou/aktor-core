package com.aktor.core.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfigurationXml implements Configuration
{
    private static final String ATTRIBUTE_KEY = "key";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_PATH = "path";

    private final Map<String, String> values = new LinkedHashMap<>();

    public ConfigurationXml(final Path path)
    {
        Objects.requireNonNull(path);
        try (InputStream input = Files.newInputStream(path))
        {
            parse(input);
        }
        catch (final Exception exception)
        {
            throw new IllegalStateException("Failed to load XML configuration file " + path + ".", exception);
        }
    }

    public ConfigurationXml(final InputStream inputStream)
    {
        Objects.requireNonNull(inputStream);
        try
        {
            parse(inputStream);
        }
        catch (final Exception exception)
        {
            throw new IllegalStateException("Failed to load XML configuration stream.", exception);
        }
    }

    @Override
    public String getString(final String key)
    {
        return values.get(key);
    }

    @Override
    public boolean has(final String key)
    {
        if (values.containsKey(key))
        {
            return true;
        }

        final String prefix = key + ".";
        for (final String candidate : values.keySet())
        {
            if (candidate.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] keys()
    {
        return values.keySet().toArray(new String[0]);
    }

    @Override
    public Long getLong(final String key)
    {
        final String value = getString(key);
        return value == null || value.isBlank() ? null : Long.parseLong(value.trim());
    }

    @Override
    public Integer getInteger(final String key)
    {
        final String value = getString(key);
        return value == null || value.isBlank() ? null : Integer.parseInt(value.trim());
    }

    @Override
    public Boolean getBoolean(final String key)
    {
        final String value = getString(key);
        return value == null || value.isBlank() ? null : Boolean.parseBoolean(value.trim());
    }

    @Override
    public Configuration getConfiguration(final String key)
    {
        return new ConfigurationSection(this, key);
    }

    private void parse(final InputStream inputStream) throws Exception
    {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(inputStream);
        final Element root = document.getDocumentElement();
        if (root != null)
        {
            parseElement(root, null);
        }
    }

    private void parseElement(final Element element, final String prefix)
    {
        final String currentPrefix = resolvePrefix(element, prefix);
        if (currentPrefix != null)
        {
            storeAttributes(element, currentPrefix);
        }

        final NodeList children = element.getChildNodes();
        boolean hasChildElement = false;
        for (int index = 0; index < children.getLength(); index++)
        {
            final Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                hasChildElement = true;
                parseElement((Element) child, currentPrefix);
            }
        }

        if (!hasChildElement && currentPrefix != null)
        {
            final String text = element.getTextContent();
            if (text != null)
            {
                final String value = text.trim();
                if (!value.isBlank())
                {
                    values.put(currentPrefix, value);
                }
            }
        }
    }

    private static String resolvePrefix(final Element element, final String prefix)
    {
        final String elementName = element.getTagName();
        if (elementName == null || elementName.isBlank() || "configuration".equals(elementName))
        {
            return prefix;
        }

        final String sectionKey;
        if ("entity".equals(elementName))
        {
            sectionKey = "entity." + requiredAttribute(element, "name", "key");
        }
        else if ("relation".equals(elementName))
        {
            sectionKey = "relation." + requiredAttribute(element, "field", "name", "key");
        }
        else if ("storage".equals(elementName))
        {
            sectionKey = "storage";
        }
        else
        {
            sectionKey = firstNonBlank(
                attributeValue(element, ATTRIBUTE_KEY),
                attributeValue(element, ATTRIBUTE_PATH),
                attributeValue(element, ATTRIBUTE_NAME),
                elementName
            );
        }

        return sectionKey == null ? prefix : join(prefix, sectionKey);
    }

    private void storeAttributes(final Element element, final String prefix)
    {
        if (!element.hasAttributes())
        {
            return;
        }
        for (int index = 0; index < element.getAttributes().getLength(); index++)
        {
            final Node attribute = element.getAttributes().item(index);
            if (attribute == null)
            {
                continue;
            }
            final String attributeName = attribute.getNodeName();
            final String attributeValue = attribute.getNodeValue();
            if (attributeName != null && !attributeName.isBlank()
                && attributeValue != null && !attributeValue.isBlank())
            {
                values.put(join(prefix, attributeName), attributeValue.trim());
            }
        }
    }

    private static String requiredAttribute(final Element element, final String... attributeNames)
    {
        for (final String attributeName : attributeNames)
        {
            final String value = attributeValue(element, attributeName);
            if (value != null && !value.isBlank())
            {
                return value.trim();
            }
        }
        throw new IllegalStateException("Missing required XML attribute for element: " + element.getTagName());
    }

    private static String attributeValue(final Element element, final String attributeName)
    {
        if (element.hasAttribute(attributeName))
        {
            return element.getAttribute(attributeName);
        }
        return null;
    }

    private static String firstNonBlank(final String... values)
    {
        for (final String value : values)
        {
            if (value != null && !value.isBlank())
            {
                return value.trim();
            }
        }
        return null;
    }

    private static String join(final String prefix, final String key)
    {
        if (prefix == null || prefix.isBlank())
        {
            return key;
        }
        return prefix + "." + key;
    }
}
