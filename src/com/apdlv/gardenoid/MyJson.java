package com.apdlv.gardenoid;

public class MyJson 
{    
    public static String nullOrEscapedInDoubleQuotes(String val)
    {	
	return null==val ? "null" : "\"" + escape(val) + "\"";
    }

    private static String escape(String key)
    {
	if (null==key) return "null";
	key = key.replaceAll("\\\\", "\\\\");
	key = key.replaceAll("\"", "\\\"");
	return key;
    }
}
