
package com.venista.geoloc;

import java.util.Map;
import java.util.TreeMap;

public class YahooCountry
{
	
	public static String getISOCountryCode(String yahooCountryCode)
	{
		if (null==yahooCountryCode)
			return null;
		
		if (yahooCountryCode.startsWith("US"))
			return "US, " + yahooCountryCode.substring(2);
		
		String isoCountryCode = yahooToIso.get(yahooCountryCode);
		return null==isoCountryCode ? yahooCountryCode : isoCountryCode;
	}
	
	
	public static String getYahooCountryCode(String isoCountryCode)	
	{
		String yahooCountryCode;
		
		if (null != (yahooCountryCode=remapped.get(isoCountryCode)))
		{
			return yahooCountryCode;
		}
		
		yahooCountryCode = null==isoCountryCode ? null : isoToYahoo.get(isoCountryCode);
		return null==yahooCountryCode ? isoCountryCode : yahooCountryCode;
	}	 

	/*
	public static Set<String> getYahooCountryCodes()
	{
		return yahooToIso.keySet();
	}
	*/
	
	
	public static boolean isRemapped(String isoCountryCode)
	{
		return null!=isoCountryCode && remapped.containsKey(isoCountryCode);
	}
	
	
	public static boolean differs(String isoCountryCode)
	{
		return null!=isoCountryCode && isoToYahoo.containsKey(isoCountryCode.toUpperCase());
	}
	
	
	private static final Map<String,String> yahooToIso = new TreeMap<String,String>();	
	private static final Map<String,String> isoToYahoo = new TreeMap<String,String>();	
	private static final Map<String,String> remapped     = new TreeMap<String,String>();		
	static
	{
		// these ones are mapped to the country 
		remapped.put("UM", "US");    // USA Minor Outlying Islands             -> USA
		remapped.put("JE", "UK");    // Jersey                                 -> UK
		remapped.put("IO", "UK");    // British Indian Ocean Territory         -> UK		
		remapped.put("GG", "UK");    // Guernsey                               -> UK
		remapped.put("GS", "UK");    // South Georgia & South Sandwich Islands -> UK
		remapped.put("IO", "UK");    // British Indian Ocean Territory         -> UK		
		remapped.put("FK", "UK");    // Falkland Islands                       -> UK
		remapped.put("MS", "UK");    // Montserrat, Plymouth/Plymouth          -> UK		
		remapped.put("SH", "UK");    // Saint Helena (Jamestown)               -> UK 
		remapped.put("CX", "AS");    // Christmas Island (Flying Fish Cove)    -> Australia
		remapped.put("HM", "AS");    // Heard Island and McDonald Islands      -> Australia
		remapped.put("NF", "AS");    // Norfolk Island (Kingston)              -> Australia		
		remapped.put("TF", "FR");    // French Southern Territories            -> France		
		remapped.put("PM", "FR");    // Saint Pierre and Miquelon              -> France
		remapped.put("WF", "FR");    // Wallis and Futuna Islands              -> France
		remapped.put("SJ", "NO");    // Svalbard and Jan Mayen Islands         -> Norway
		remapped.put("BV", "NO");    // Bouvet Island                          -> Norway 
		remapped.put("FO", "DA");    // Faroe Islands                          -> Danmark          
		remapped.put("TK", "NZ");    // Tokelau                                -> New Zealand
		remapped.put("VC", "ST");    // Saint Vincent & Grenadines             -> Saint Lucia
		remapped.put("HK", "CH");    // Hong Kong                              -> China
		remapped.put("MO", "CH");    // Macau (Macau)                          -> China
		remapped.put("ME", "YI");    // Montenegro (Podgorica)                 -> Serbia and Montenegro
		remapped.put("WS", "USAS");  // (West)-Samoa (Apia)                    -> American Samoa
		remapped.put("VA", "IT");    // Vatican                                -> Italy

		// these ones were are handled specially as US territories
		isoToYahoo.put("GU", "USGU");  // Guam                                   -> USA
		isoToYahoo.put("VI", "USVI");  // Virgin Islands (US)                    -> USA
		isoToYahoo.put("PW", "USPW");  // Palau                                  -> USA
		isoToYahoo.put("FM", "USFM");  // Palikir [USFM0004]                     -> USA	
		isoToYahoo.put("AS", "USAS");  // Pago Pago/Pago Pago [USAS0001]         -> USA
		isoToYahoo.put("MH", "USMH");  // Majuro/Majuro [USMH0002]               -> USA
		isoToYahoo.put("MP", "USMP");  // Saipan/Saipan [USMP0001]               -> USA
		isoToYahoo.put("PN", "USMD");  // Adamstown/Adamstown [USMD0007]         -> USA
		isoToYahoo.put("PR", "USPR");  // Puerto Rico                            -> USA
		
		// these ones cannot be derived by comparing capital cities defined in Capitals and cities in weather locations: 		
		isoToYahoo.put("AG", "AC"); // Antigua and Barbuda
		isoToYahoo.put("AI", "AV"); // Anguilla (The Valley)
		isoToYahoo.put("AN", "NT"); // Netherlands Antilles (Willemstad) 
		isoToYahoo.put("AQ", "AY"); // Antarctica
		isoToYahoo.put("BH", "BA"); // Bahrain (Al-Manamah)
		isoToYahoo.put("BN", "BX"); // Brunei Darussalam (Bandar Seri Begawan)
		isoToYahoo.put("CC", "KT"); // Cocos (Keeling) Islands (Cocos Island Airport)
		isoToYahoo.put("CK", "CW"); // Cook Islands
		isoToYahoo.put("CL", "CI"); // Chile (Santiago) (There's a Santiago also e.g. in Brasilia)
		isoToYahoo.put("CR", "CS"); // Costa Rica (San Jose) (There's a San Jose also in Argentina)
		isoToYahoo.put("EH", "WI"); // Western Sahara (El Aaiún)         
		isoToYahoo.put("GB", "UK"); // Great Britain (London) (There's a London in Canada also that would cause confusion below) 
		isoToYahoo.put("GD", "GJ"); // Grenada
		isoToYahoo.put("KM", "CN"); // Comoros (Hahaya)
		isoToYahoo.put("MC", "MN"); // Monaco
		isoToYahoo.put("MM", "BM"); // Mauritius (Port Louis)		
		isoToYahoo.put("MU", "MP"); // Mauritius (Port Louis)		
		isoToYahoo.put("PF", "FP"); // Polynesia (French)                     
		isoToYahoo.put("TT", "TD"); // Trinidad and Tobago
		isoToYahoo.put("VU", "NH"); // Vanuatu
		isoToYahoo.put("YE", "YM"); // Yemen
		isoToYahoo.put("LK", "CE"); // Sri Lanka, Colombo/Colombo (There's a Colombo in Brazil also)	
		isoToYahoo.put("KY", "CJ"); // Cayman Islands, (Georgetown)
		isoToYahoo.put("ST", "TP"); // Sao Tome and Principe (Sao Tome)		
		isoToYahoo.put("YT", "MF"); // Mayotte (Dzaoudzi)
		
		// these ones were computed automatically by the Test controller based on a comparison of capital cities
		isoToYahoo.put("VN", "VM"); // Vietnam, Hanoi/Hanoi [VMXX0006]
		isoToYahoo.put("DZ", "AG"); // Algeria, Algiers/Algiers [AGXX0001]
		isoToYahoo.put("VG", "VI"); // Virgin Islands (British), Road Town/Road Town [VIXX0001]
		isoToYahoo.put("DM", "DO"); // Dominica, Roseau/Roseau [DOXX0002]
		isoToYahoo.put("DO", "DR"); // Dominican Republic, Santo Domingo/Santo Domingo [DRXX0009]
		isoToYahoo.put("DE", "GM"); // Germany, Berlin/Berlin [GMXX0007]
		isoToYahoo.put("DK", "DA"); // Denmark, Copenhagen/Copenhagen [DAXX0009]
		isoToYahoo.put("UA", "UP"); // Ukraine, Kiev/Kiev [UPXX0016]
		isoToYahoo.put("ES", "SP"); // Spain, Madrid/Madrid [SPXX0050]
		isoToYahoo.put("EE", "EN"); // Estonia, Tallinn/Tallinn [ENXX0004]
		isoToYahoo.put("GE", "GG"); // Georgia, Tbilisi/Tbilisi [GGXX0004]
		isoToYahoo.put("GF", "FG"); // French Guiana, Cayenne/Cayenne [FGXX0001]
		isoToYahoo.put("GA", "GB"); // Gabon, Libreville/Libreville [GBXX0004]
		isoToYahoo.put("GW", "PU"); // Guinea Bissau, Bissau/Bissau [PUXX0001]
		isoToYahoo.put("GQ", "EK"); // Equatorial Guinea, Malabo/Malabo [EKXX0003]
		isoToYahoo.put("GN", "GV"); // Guinea, Conakry/Conakry [GVXX0002]
		isoToYahoo.put("GM", "GA"); // Gambia, Banjul/Banjul [GAXX0001]
		isoToYahoo.put("AT", "AU"); // Austria, Vienna/Vienna [AUXX0025]
		isoToYahoo.put("AW", "AA"); // Aruba, Oranjestad/Oranjestad [AAXX0001]
		isoToYahoo.put("AU", "AS"); // Australia, Canberra/Canberra [ASXX0023]
		isoToYahoo.put("AZ", "AJ"); // Azerbaijan, Baku/Baku [AJXX0001]
		isoToYahoo.put("BA", "BK"); // Bosnia-Herzegovina, Sarajevo/Sarajevo [BKXX0004]
		isoToYahoo.put("PT", "PO"); // Portugal, Lisbon/Lisbon [POXX0016]
		isoToYahoo.put("AD", "AN"); // Andorra, Andorra/Andorra [ANXX0001]
		isoToYahoo.put("AG", "AC"); // Antigua and Barbuda, Saint John's/Saint John's [ACXX0002]
		isoToYahoo.put("PS", "WE"); // Palestinian territories, Ramallah/Ramallah [WEXX0005]
		isoToYahoo.put("PY", "PA"); // Paraguay, Asuncion/Asuncion [PAXX0001]
		isoToYahoo.put("BW", "BC"); // Botswana, Gaborone/Gaborone [BCXX0001]
		isoToYahoo.put("TG", "TO"); // Togo, Lome/Lome [TOXX0001]
		isoToYahoo.put("BY", "BO"); // Belarus, Minsk/Minsk [BOXX0005]
		isoToYahoo.put("TD", "CD"); // Chad, Ndjamena/Ndjamena [CDXX0003]
		isoToYahoo.put("BS", "BF"); // Bahamas, Nassau/Nassau [BFXX0005]
		isoToYahoo.put("TJ", "TI"); // Tajikistan, Dushanbe/Dushanbe [TIXX0001]
		isoToYahoo.put("TO", "TN"); // Tonga, Nuku'alofa/Nuku'alofa [TNXX0001]
		isoToYahoo.put("TN", "TS"); // Tunisia, Tunis/Tunis [TSXX0010]
		isoToYahoo.put("TM", "TX"); // Turkmenistan, Ashgabat Keshi/Ashgabat Keshi [TXXX0018]
		isoToYahoo.put("TR", "TU"); // Turkey, Ankara/Ankara [TUXX0002]
		isoToYahoo.put("BZ", "BH"); // Belize, Belmopan/Belmopan [BHXX0002]
		isoToYahoo.put("BF", "UV"); // Burkina Faso, Ouagadougou/Ouagadougou [UVXX0001]
		isoToYahoo.put("BG", "BU"); // Bulgaria, Sofia/Sofia [BUXX0005]
		isoToYahoo.put("SV", "ES"); // El Salvador, San Salvador/San Salvador [EXXX0001]		
		isoToYahoo.put("BI", "BY"); // Burundi, Bujumbura/Bujumbura [BYXX0001]
		isoToYahoo.put("SZ", "WZ"); // Swaziland, Mbabane/Mbabane [WZXX0001]
		isoToYahoo.put("BD", "BG"); // Bangladesh, Dhaka/Dhaka [BGXX0003]
		isoToYahoo.put("BO", "BL"); // Bolivia, La Paz/La Paz [BLXX0006]
		isoToYahoo.put("BJ", "BN"); // Benin, Porto-Novo/Porto-Novo [BNXX0002]
		isoToYahoo.put("TC", "TK"); // Turks and Caicos Islands, Grand Turk/Grand Turk [TKXX0001]
		isoToYahoo.put("BM", "BD"); // Bermuda, Hamilton/Hamilton [BDXX0002]
		isoToYahoo.put("SD", "SU"); // Sudan, Khartoum/Khartoum [SUXX0002]
		isoToYahoo.put("CZ", "EZ"); // Czech Rep., Prague/Prague [EZXX0012]
		isoToYahoo.put("SC", "SE"); // Seychelles, Victoria/Victoria [SEXX0001]
		isoToYahoo.put("SE", "SW"); // Sweden, Stockholm/Stockholm [SWXX0031]
		isoToYahoo.put("SG", "SN"); // Singapore, Singapore/Singapore [SNXX0006]
		isoToYahoo.put("SK", "LO"); // Slovakia, Bratislava/Bratislava [LOXX0001]
		isoToYahoo.put("SN", "SG"); // Senegal, Dakar/Dakar [SGXX0001]
		isoToYahoo.put("SR", "NS"); // Suriname, Paramaribo/Paramaribo [NSXX0002]
		isoToYahoo.put("CI", "IV"); // Ivory Coast, Abidjan/Abidjan [IVXX0001]
		isoToYahoo.put("RS", "YI"); // Serbia, Belgrade/Belgrade [YIXX0005]
		isoToYahoo.put("CG", "CF"); // Congo, Brazzaville/Brazzaville [CFXX0001]
		isoToYahoo.put("CH", "SZ"); // Switzerland, Bern/Bern [SZXX0006]
		isoToYahoo.put("RU", "RS"); // Russia, Moscow/Moscow [RSXX0063]
		isoToYahoo.put("CF", "CT"); // Central African Republic, Bangui/Bangui [CTXX0001]
		isoToYahoo.put("CD", "CG"); // Congo, Dem. Republic, Kinshasa/Kinshasa [CGXX0005]
		isoToYahoo.put("CN", "CH"); // China, Beijing/Beijing [CHXX0008]
		isoToYahoo.put("SB", "BP"); // Solomon Islands, Honiara/Honiara [BPXX0001]
		isoToYahoo.put("LV", "LG"); // Latvia, Riga/Riga [LGXX0004]
		isoToYahoo.put("LT", "LH"); // Lithuania, Vilnius/Vilnius [LHXX0005]
		isoToYahoo.put("LS", "LT"); // Lesotho, Maseru/Maseru [LTXX0001]
		isoToYahoo.put("LR", "LI"); // Liberia, Monrovia/Monrovia [LIXX0002]
		isoToYahoo.put("MG", "MA"); // Madagascar, Antananarivo/Antananarivo [MAXX0002]
		isoToYahoo.put("MA", "MO"); // Morocco, Rabat/Rabat [MOXX0007]
		isoToYahoo.put("MW", "MI"); // Malawi, Lilongwe/Lilongwe [MIXX0002]
		isoToYahoo.put("MN", "MG"); // Mongolia, Ulan Bator/Ulan Bator [MGXX0003]
		isoToYahoo.put("MQ", "MB"); // Martinique (French), Fort-de-France/Fort-de-France [MBXX0001]
		isoToYahoo.put("NG", "NI"); // Nigeria, Lagos/Lagos [NIXX0012]
		isoToYahoo.put("NI", "NU"); // Nicaragua, Managua/Managua [NUXX0004]
		isoToYahoo.put("NA", "WA"); // Namibia, Windhoek/Windhoek [WAXX0004]
		isoToYahoo.put("NE", "NG"); // Niger, Niamey/Niamey [NGXX0003]
		isoToYahoo.put("OM", "MU"); // Oman, Muscat/Muscat [MUXX0003]
		isoToYahoo.put("PH", "RP"); // Philippines, Manila/Manila [RPXX0017]
		isoToYahoo.put("PG", "PP"); // Papua New Guinea, Port Moresby/Port Moresby [PPXX0004]
		isoToYahoo.put("PA", "PM"); // Panama, Panama City/Panama City [PMXX0004]
		isoToYahoo.put("ZA", "SF"); // South Africa, Pretoria/Pretoria [SFXX0044]
		isoToYahoo.put("HN", "HO"); // Honduras, Tegucigalpa/Tegucigalpa [HOXX0008]
		isoToYahoo.put("HT", "HA"); // Haiti, Port-au-Prince/Port-au-Prince [HAXX0004]
		isoToYahoo.put("ZM", "ZA"); // Zambia, Lusaka/Lusaka [ZAXX0004]
		isoToYahoo.put("ZW", "ZI"); // Zimbabwe, Harare/Harare [ZIXX0004]
		isoToYahoo.put("IE", "EI"); // Ireland, Dublin/Dublin [EIXX0014]
		isoToYahoo.put("IL", "IS"); // Israel, Jerusalem/Jerusalem [ISXX0010]
		isoToYahoo.put("IQ", "IZ"); // Iraq, Baghdad/Baghdad [IZXX0008]
		isoToYahoo.put("IS", "IC"); // Iceland, Reykjavik/Reykjavik [ICXX0002]
		isoToYahoo.put("JP", "JA"); // Japan, Tokyo/Tokyo [JAXX0085]
		isoToYahoo.put("KI", "KR"); // Kiribati, Tarawa/Tarawa [KRXX0002]
		isoToYahoo.put("KH", "CB"); // Cambodia, Phnom Penh/Phnom Penh [CBXX0001]
		isoToYahoo.put("KP", "KN"); // Korea-North, Pyongyang/Pyongyang [KNXX0006]
		isoToYahoo.put("KR", "KS"); // Korea-South, Seoul/Seoul [KSXX0037]
		isoToYahoo.put("KN", "SC"); // Saint Kitts & Nevis Anguilla, Basseterre/Basseterre [SCXX0001]
		isoToYahoo.put("KW", "KU"); // Kuwait, Kuwait City/Kuwait City [KUXX0003]
		isoToYahoo.put("LC", "ST"); // Saint Lucia, Castries/Castries [STXX0001]
		isoToYahoo.put("LB", "LE"); // Lebanon, Beirut/Beirut [LEXX0003]
		isoToYahoo.put("LI", "LS"); // Liechtenstein, Vaduz/Vaduz [LSXX0002]
		
		// create also inverse mapping
		for (String key : isoToYahoo.keySet())
		{
			String invKey = isoToYahoo.get(key);
			String invVal = key;
			
			if (yahooToIso.containsKey(invKey))
			{
				String other = yahooToIso.get(invKey); 
				System.err.println("---- YahooCountry: ambigious mapping for " + invKey + " to " + invVal + " / " + other + " ----");
			}
			yahooToIso.put(invKey, invVal);
		}		
	}
	
	
	public static String getMainCountryIsoCode(String isoCode)
	{
		if (null==isoCode) 
			return null;
		
		String yahooCode = remapped.get(isoCode);
		if (null==yahooCode)
			return null;
		
		String mainCountryCode = getISOCountryCode(yahooCode);
		return mainCountryCode;
	}
	
}
