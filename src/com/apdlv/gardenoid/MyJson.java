package com.apdlv.gardenoid;

public class MyJson 
{
    public MyJson()
    {
	first = true;
    }
    
    public MyJson(String key, String val)
    {
	first = true;
	add(key,val);
    }
    
    public MyJson(String key, long val)
    {
	first = true;
	add(key,val);
    }
    
    public MyJson(String key, boolean val)
    {
	first = true;
	add(key,val);
    }

    public MyJson add(String key, String val)
    {
	addDelimiter();
	buf.append("\"").append(escape(key)).append("\" : ").append(nullOrEscaped(val));
	return this;
    }

    public MyJson add(String key, long l)
    {
	addDelimiter();
	buf.append("\"").append(escape(key)).append("\" : ").append(l);
	return this;
    }
    
    
    public MyJson add(String key, boolean b)
    {
	addDelimiter();
	buf.append("\"").append(escape(key)).append("\" : ").append(b ? "true" : "false");
	return this;
    }
    

    public MyJson addJson(String json)
    {
	addDelimiter();
	buf.append(json);
	return this;
    };
    

    public MyJson addJson(String name, String json)
    {
	addDelimiter();
	buf.append(" \"").append(escape(name)).append("\" : ").append(json);
	return this;
    };
    

    
    public MyJson open()
    {
	first = true;
	return this;
    }
    
    public MyJson close()
    {
	buf.append("}");
	return this;
    }
    
    public String toString()
    {
	return buf.toString() + " " + terminators;
    }
    
    private void addDelimiter()
    {
	if (first)
	{
	    buf.append(" {");
	    terminators = "}" + terminators;
	}
	else
	{
	    buf.append(", ");	    
	}
	first = false;
    }
    
    
    private static String escape(String key)
    {
	if (null==key) return "null";
	key = key.replaceAll("\\\\", "\\\\");
	key = key.replaceAll("\"", "\\\"");
	return key;
    }

    
    private static String nullOrEscaped(String val)
    {	
	return null==val ? "null" : "\"" + escape(val) + "\"";
    }

    
    public static String nullOrInDoubleQuotes(String arg)
    {
	    if (null==arg) return "null";
	    return "\"" + arg + "\"";
    }

    public JArray array(String name)
    {

	return new JArray(this, name);	
    }
    
    private boolean first;
    private StringBuilder buf = new StringBuilder();
    private String terminators = "";
    public boolean array = false;
    
    public class JArray
    {
	private MyJson parent;
	private boolean first = true;
	
	public JArray(MyJson parent, String name)
	{
	    this.parent = parent;
	    parent.addDelimiter();
	    parent.buf.append(' ').append(nullOrEscaped(name)).append(" : [");
	    parent.terminators = "]" + parent.terminators;
	    parent.array = true; // inside array now
	}
	
	public JArray add(String elem)
	{
	    if (!first) parent.buf.append(",");
	    parent.buf.append(nullOrEscaped(elem));
	    return this;
	}

	public JArray add(long l)
        {
	    if (!first) parent.buf.append(",");
	    parent.buf.append(l);
	    return this;
        }

	public JArray addJson(String json)
        {
	    parent.buf.append(first ? " " : ", ");
	    parent.buf.append(json); 
	    first=false;
	    return this;	    
        }

	public JArray br()
        {
	    parent.buf.append('\n');
	    return this;	    	    
        }
	
	public void done()
	{
	    parent.buf.append("]");
	    parent.array = false;
	}
    }

}
