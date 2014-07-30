
package com.venista.geoloc;


import java.io.IOException;

import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;


public class DNSLocalizer
{	
	public DNSLocalizer(int timeoutMillies)
	{
		this.timeoutMillies = timeoutMillies;
	}


	public String getCountryCodeByHost(String hostName) throws IOException
	{
		String top = topLevelDomain(hostName);		
		if (null!=top)
		{
			String city = Capitals.getCapital(top.toUpperCase());
			return null==city ? null : top;
		}		
		return null;
	}


	public String getCountryCodeByIP(String ipAddress) throws IOException
	{
		if (null!=ipAddress)
		{
			String hostName = reverseDns(ipAddress);
			if (null!=hostName)
			{
				return getCountryCodeByHost(hostName);
			}
		}
		return null;
	}


	private String topLevelDomain(String fullyQualifiedDomain)
	{
		if (null==fullyQualifiedDomain)
			return null;

		if (fullyQualifiedDomain.endsWith("."))
			fullyQualifiedDomain = fullyQualifiedDomain.substring(0,fullyQualifiedDomain.length()-1);

		int dotPos = fullyQualifiedDomain.lastIndexOf('.');
		return (dotPos<1) ? null : fullyQualifiedDomain.substring(dotPos+1);
	}


	private String reverseDns(String hostIp) throws IOException 
	{
		try
		{
			ExtendedResolver res = new ExtendedResolver();
			res.setTimeout(timeoutMillies/1000, timeoutMillies%1000);

			Name name  = ReverseMap.fromAddress(hostIp);
			int type   = Type.PTR;
			int dclass = DClass.IN;

			Record rec = Record.newRecord(name, type, dclass);

			Message query    = Message.newQuery(rec);
			Message response = res.send(query);

			Record[] answers = response.getSectionArray(Section.ANSWER);
			return (0==answers.length) ? null : answers[0].rdataToString();
		}
		catch (Error ex)
		{
			System.out.println("Error: " + ex);
		}
		return null;
	}


	private int timeoutMillies;
}
