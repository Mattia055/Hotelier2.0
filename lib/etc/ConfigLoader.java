package lib.etc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader{

    private Properties attributes = new Properties();

    /*
     * Il costruttore lancia delle eccezioni al chiamante
     */
    public ConfigLoader(String path)throws FileNotFoundException,IOException {
        try(FileInputStream input = new FileInputStream(path)){

            attributes.load(input);

        } catch(FileNotFoundException e){
            throw new FileNotFoundException("Config file "+path+" not found");
        } catch(IOException e){
            throw new IOException();
        }
    }

    /**
     * Retrieves a string attribute from the properties.
     * @param key The property key.
     * @return The property value or null if the property is not found.
     */
    private String getStringAttribute(String key) {
        return attributes.getProperty(key);
    }

    public String getStringAttribute(String key, String defaultValue){
        String attr = getStringAttribute(key);
        if(attr == null){
            attr = defaultValue;
        }
        return attr;
    }

    /**
     * Retrieves an integer attribute from the properties.
     * @param key The property key.
     * @return The integer value of the property, or a default value if the property is not found or is invalid.
     */
    public int getIntAttribute(String key, int defaultValue) {
        String attr = getStringAttribute(key);
        if (attr == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(attr);
        } catch (NumberFormatException e) {
            return defaultValue; // Return default value on parse error
        }
    }

    /**
     * Retrieves a double attribute from the properties.
     * @param key The property key.
     * @return The double value of the property, or a default value if the property is not found or is invalid.
     */
    public double getDoubleAttribute(String key, double defaultValue) {
        String attr = getStringAttribute(key);
        if (attr == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(attr);
        } catch (NumberFormatException e) {
            return defaultValue; // Return default value on parse error
        }
    }

    /**
     * Retrieves a boolean attribute from the properties.
     * @param key The property key.
     * @return The boolean value of the property, or a default value if the property is not found or is invalid.
     */
    public boolean getBooleanAttribute(String key, boolean defaultValue) {
        String attr = getStringAttribute(key);
        if (attr == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(attr);
    }

    /**
     * Retrieves an integer attribute from the properties.
     * @param key The property key.
     * @return The integer value of the property, or a default value if the property is not found or is invalid.
     */
    public long getLongAttribute(String key, long defaultValue) {
        String attr = getStringAttribute(key);
        if (attr == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(attr); // Use Long.parseLong instead of long.parseLong
        } catch (NumberFormatException e) {
            return defaultValue; // Return default value or parse error
        }
    }

}