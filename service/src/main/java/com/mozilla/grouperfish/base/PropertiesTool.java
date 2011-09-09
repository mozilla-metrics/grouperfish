package com.mozilla.grouperfish.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;


public class PropertiesTool {

    public static Properties load(final Class<?> context, final String resourceName) {
        final Properties properties = new Properties();
        URL source = context.getResource(resourceName);
        if (source == null) return properties;

        InputStream stream = null;
        try {
            stream = source.openStream();
            properties.load(new InputStreamReader(stream, StreamTool.UTF8));
        }
        catch (IOException e) {
            throw new RuntimeException(String.format("Failed to load properties from '%s'...", resourceName), e);
        }
        finally {
            if (stream == null) return properties;
            try { stream.close(); }
            catch (IOException e) { throw new RuntimeException(e); }
        }
        return properties;
    }


}
