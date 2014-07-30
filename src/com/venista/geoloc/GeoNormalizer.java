

package com.venista.geoloc;

import java.text.Normalizer;

// http://mindprod.com/jgloss/accents.html
public class GeoNormalizer
{
	public static void main(String args[])
	{
		String strings[] =
		{
				"de l'Hôspital et de Mot  ", "Müller", "Ärger", "áéíóúÁÉÍÓÚçêšĉĝĥĵŝû", "L'viv"
		};

		for (String s : strings)
		{
			String r = removeAccents(s);			
			String g = normalizeCityName(s);			
			System.out.println("'" + s + "' -> '" + r + "' -> '" + g + "'");
		}
	
	}

	
	public static String normalizeCityName(String accented)
	{
		String toLower    = removeAccents(accented).toLowerCase();
		
		// Any special character replaced by space, sequences of spaces by one space
		String woSpecials = toLower.replaceAll("[^a-z]", " ").replaceAll("[ ]+", " ");
		
		String result = "";
		String split1[] = woSpecials.split(" +");		
		for (String s : split1)
		{
			if (s.length()>2)
			{
				if (result.length()>0)
					result += " ";
				result += s;
			}
		}
		
		// Quotes removed, e.g. "L'viv" becomes "Lviv". After that, same replacements as above.
		String woQuotes   = toLower.replaceAll("[']", "").replaceAll("[^a-z]", " ").replaceAll("[ ]+", " ");
		
		String split2[] = woQuotes.split(" +");		
		for (String s : split2)
		{
			if (s.length()>2 && result.indexOf(s)<0)
			{
				if (result.length()>0)
					result += " ";
				result += s;
			}	
		}
		
		return result;
	}
	
	
	public static String removeAccents( String accented )
	{
		// convert accented chars to equivalent unaccented char + dead char accent pair.
		// See http://www.unicode.org/unicode/reports/tr15/tr15-23.html no understand the NFD transform.
		final String normalized = Normalizer.normalize(accented, Normalizer.Form.NFD );
		
		// remove the dead char accents, leaving just the unaccented chars.
		// Stripped string should have the same length as the original accented String.
		StringBuilder sb = new StringBuilder(accented.length());
		
		for (int i=0; i<normalized.length(); i++)
		{
			char c = normalized.charAt( i );
			if (Character.getType(c) != Character.NON_SPACING_MARK )
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
