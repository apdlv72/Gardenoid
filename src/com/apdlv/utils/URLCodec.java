package com.apdlv.utils;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLCodec 
{
	
	public static String urlEncoded(String text) 
	{
		if (null==text)
			return null;
		try { return null==text ? null : URLEncoder.encode(text, "UTF-8"); }
		catch (UnsupportedEncodingException e) { e.printStackTrace(); }
		return text;
	}

	
	public static final String encodeUTF8(String s)
	{
		if (null==s)
			return null;
		try { return URLEncoder.encode(s,"UTF-8"); }
		catch (UnsupportedEncodingException usex) { usex.printStackTrace(); }	
		return null;
	}
	
	public static final String encodeASCII(String s)
	{
		if (null==s)
			return null;
		try { return URLEncoder.encode(s,"US-ascii"); }
		catch (UnsupportedEncodingException usex) { usex.printStackTrace(); }	
		return null;
	}
	

	public static final String encode(byte[] bytes, boolean spaceAsPlus) 
	{
		StringBuffer sb = new StringBuffer();
		for (int n=bytes.length, i=0; i<n; i++)
		{
			int b = (int)bytes[i];
			if (' '==b)
			{
				sb.append(spaceAsPlus ? "+" : "%20");
			} 
			else if (b<' ' || 127<b || '%'==b || '+'==b || '/'==b || '!'==b || '#'==b) 
			{
				sb.append(String.format("%%%02X",(byte)b));
			}
			else sb.append((char)b);
		}
		return sb.toString();
	}
	
	public static final String encode(byte[] bytes) { return encode(bytes,true); }

	public static final byte[] decodeUrl(byte[] bytes) throws IllegalArgumentException
	{
		if (bytes == null) 
			return null;
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		for (int i = 0; i < bytes.length; i++) 
		{
			int b = bytes[i];
			if (b == '+') 
			{
				buffer.write(' ');
			} 
			else if (b == '%') 
			{
				try 
				{
					int u = Character.digit((char)bytes[++i], 16);
					int l = Character.digit((char)bytes[++i], 16);
					
					if (u == -1 || l == -1) 
						throw new IllegalArgumentException("Invalid URL encoding");
					
					buffer.write((char)((u << 4) + l));
				} 
				catch(ArrayIndexOutOfBoundsException e) 
				{
					throw new IllegalArgumentException("Invalid URL encoding");
				}
			} 
			else 
			{
				buffer.write(b);
			}
		}
		return buffer.toByteArray();
	} 
}


