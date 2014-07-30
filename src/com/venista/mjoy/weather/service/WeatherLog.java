
package com.venista.mjoy.weather.service;


import java.io.File;
import java.io.IOException;

//import javax.servlet.ServletContext;
//
//import org.apache.log4j.DailyRollingFileAppender;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PatternLayout;
//import org.springframework.web.context.ServletContextAware;


public class WeatherLog //implements ServletContextAware
{
	public void setLogfile(String filename) throws IOException
	{		
//		if (null!=logger)
//		{
//			logger.info("*** Log file changed to '" + filename + "' ***");
//			logger.removeAllAppenders();
//		}
		this.filename = filename;
		init();
	}


//	@Override
//	public void setServletContext(ServletContext servletContext)
//	{
//		this.contextPath = servletContext.getContextPath();		
//		init();
//	}

	
	private void init()
	{		
		if (null==filename || null==contextPath)
		{
			return;
		}
		
		try
		{
			filename = filename.replaceAll("%c", contextPath);
			
			// make sure that parent directory(ies) exist(s)
			try	
			{ 
				(new File(filename)).getParentFile().mkdirs(); 
			}			
			catch (Exception ex) {}
	
//			if (!hasFileAppender(logger = Logger.getLogger("file://" + filename)))
//			{
//				logger.setAdditivity(false);
//				PatternLayout layout  = new PatternLayout("[%p] [%d] %m%n");
//			
//				DailyRollingFileAppender appender = new DailyRollingFileAppender(layout, filename, "'.'yyyy-MM-dd");		
//				logger.addAppender(appender);
//				
//				//logger.addAppender(new ConsoleAppender(layout));			
//				logger.setLevel(Level.ALL);
//			}
//			
//			logger.info("*** Logging starts [" + this.getClass().getSimpleName() + "] *** ");
			//logger.info("*** Created from " + CallerInfo.getCallerInfo());
		}
		catch (Exception e) 
		{
			throw new RuntimeException("Initialization of access log failed", e);
		}
	}
	
	
	/**
	 * Bean destroy method
	 */
	public void close()
	{
//		if (null!=logger)
//		{
//			logger.info("*** Logging ends [" + this.getClass().getSimpleName() + "] *** ");
//		}
	}
	
	
	public void log(String string)
	{
	    System.out.println(string);
//		synchronized (logger)
//		{
//			logger.info(string);
//		}
	}

	
	@SuppressWarnings("unchecked")
//	private static boolean hasFileAppender(Logger logger)
//	{
//		Enumeration apps = logger.getAllAppenders();
//
//		while (null!=apps && apps.hasMoreElements())
//		{
//			Object o = apps.nextElement();						
//			if (null!=o && (o instanceof DailyRollingFileAppender))
//			{
//				return true;
//			}
//		}
//		return false;
//	}
	
	
//	private Logger  logger;
	private String  contextPath;
	private String  filename;
}
