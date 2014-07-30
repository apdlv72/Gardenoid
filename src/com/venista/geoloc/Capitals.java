
package com.venista.geoloc;

import java.util.HashMap;
import java.util.Set;

public class Capitals
{
	/*
	public static void main(String args[])
	{
		for (String c : capitalByCountryCode.keySet())			
			System.out.println(c + ": " + capitalByCountryCode.get(c));
	}
	*/

	public static Set<String> getCountryCodes()
	{
		return capitalByCountryCode.keySet();
	}

	
	public static String getCapital(String countryCode)
	{
		return null==countryCode ? null : capitalByCountryCode.get(countryCode);
	}
	
	
	public static String getCountry(String countryCode)
	{
		return null==countryCode ? null : countryByCountryCode.get(countryCode);
	}
	
	
	//from: http://www.science.co.il/Internet-Codes.asp?s=capital
	private static final String capitals =
				//  ".123456789.123456789.123456789.123456789.123456789.123456789.123456789.123\n" +
					"Ivory Coast                            Abidjan             ci       225   \n" +
					"United Arab Emirates                   Abu Dhabi           ae       971   \n" +
					"Ghana                                  Accra               gh       233   \n" +
					"Pitcairn Island                        Adamstown           pn             \n" +
					"Ethiopia                               Addis Ababa         et       251   \n" +
					"Guam (USA)                             Agana               gu       1-671 \n" +
					"Algeria                                Algiers             dz       213   \n" +
				//	"Bahrain                                Al-Manamah          bh       973   \n" +
					"Bahrain                                Al Manama           bh       973   \n" +
					"Niue                                   Alofi               nu       683   \n" +
					"Jordan                                 Amman               jo       962   \n" +
					"Netherlands                            Amsterdam           nl       31    \n" +
					"Andorra                                Andorra             ad       376   \n" +
					"Turkey                                 Ankara              tr       90    \n" +
					"Madagascar                             Antananarivo        mg       261   \n" +
					"Samoa                                  Apia                ws       684   \n" +
					"Turkmenistan                           Ashgabat Keshi      tm       993   \n" +
					"Eritrea                                Asmara              er       291   \n" +
					"Kazakhstan                             Astana              kz       7     \n" +
					"Paraguay                               Asuncion            py       595   \n" +
					"Greece                                 Athens              gr       30    \n" +
					"Cook Islands                           Avarua              ck       682   \n" +
					"Iraq                                   Baghdad             iq       964   \n" +
					"Azerbaijan                             Baku                az       994   \n" +
					"Mali                                   Bamako              ml       223   \n" +
					"Brunei Darussalam                      Bandar Seri Begawan bn       673   \n" +
					"Thailand                               Bangkok             th       66    \n" +
					"Central African Republic               Bangui              cf       236   \n" +
					"Gambia                                 Banjul              gm       220   \n" +
					"Saint Kitts & Nevis Anguilla           Basseterre          kn       1-869 \n" +
					"Guadeloupe                             Basse-Terre         gp       590   \n" +
					"China                                  Beijing             cn       86    \n" +
					"Lebanon                                Beirut              lb       961   \n" +
					"Serbia                                 Belgrade            rs       381   \n" +
					"Belize                                 Belmopan            bz       501   \n" +
					"Germany                                Berlin              de       49    \n" +
					"Switzerland                            Bern                ch       41    \n" +
					"Kyrgyzstan                             Bishkek             kg       996   \n" +
					"Guinea Bissau                          Bissau              gw       245   \n" +
					"Colombia                               Bogota              co       57    \n" +
					"Brazil                                 Brasilia            br       55    \n" +
					"Slovakia                               Bratislava          sk       421   \n" +
					"Congo                                  Brazzaville         cg       242   \n" +
					"Barbados                               Bridgetown          bb       1-246 \n" +
					"Belgium                                Brussels            be       32    \n" +
				//	"European Union                         Brussels            eu             \n" +
					"Romania                                Bucharest           ro       40    \n" +
					"Hungary                                Budapest            hu       36    \n" +
					"Argentina                              Buenos Aires        ar       54    \n" +
					"Burundi                                Bujumbura           bi       257   \n" +
					"Egypt                                  Cairo               eg       20    \n" +
					"Australia                              Canberra            au       61    \n" +
					"Venezuela                              Caracas             ve       58    \n" +
					"Saint Lucia                            Castries            lc       1-758 \n" +
					"French Guiana                          Cayenne             gf       594   \n" +
					"Virgin Islands (USA)                   Charlotte Amalie    vi       1-340 \n" +
					"Sri Lanka                              Colombo             lk       94    \n" +
					"Guinea                                 Conakry             gn       224   \n" +
					"Denmark                                Copenhagen          dk       45    \n" +
					"Senegal                                Dakar               sn       221   \n" +
					"Syria                                  Damascus            sy       963   \n" +
					"Bangladesh                             Dhaka               bd       880   \n" +
					"Djibouti                               Djibouti            dj       253   \n" +
					"Tanzania                               Dodoma              tz       255   \n" +
					"Qatar                                  Doha                qa       974   \n" +
					"Isle of Man                            Douglas             im             \n" +
					"Ireland                                Dublin              ie       353   \n" +
					"Tajikistan                             Dushanbe            tj       992   \n" +
					"Mayotte                                Dzaoudzi            yt       269   \n" +
					"Western Sahara                         El Aaiun            eh             \n" +
					"Martinique (French)                    Fort-de-France      mq       596   \n" +
					"Sierra Leone                           Freetown            sl       232   \n" +
					"Tuvalu                                 Funafuti            tv       688   \n" +
					"Botswana                               Gaborone            bw       267   \n" +
					"Cayman Islands                         Georgetown          ky       1-345 \n" +
					"Guyana                                 Georgetown          gy       592   \n" +
					"Gibraltar                              Gibraltar           gi       350   \n" +
					"Greenland                              Godthab             gl       299   \n" +
					"Turks and Caicos Islands               Grand Turk          tc       1-649 \n" +
					"Guatemala                              Guatemala City      gt       502   \n" +
					"Bermuda                                Hamilton            bm       1-441 \n" +
					"Vietnam                                Hanoi               vn       84    \n" +
					"Zimbabwe                               Harare              zw       263   \n" +
					"Cuba                                   Havana              cu       53    \n" +
					"Finland                                Helsinki            fi       358   \n" +
					"Solomon Islands                        Honiara             sb       677   \n" +
					"Pakistan                               Islamabad           pk       92    \n" +
					"Indonesia                              Jakarta             id       62    \n" +
					"Saint Helena                           Jamestown           sh       290   \n" +
					"Israel                                 Jerusalem           il       972   \n" +
					"Afghanistan                            Kabul               af       93    \n" +
					"Uganda                                 Kampala             ug       256   \n" +
					"Nepal                                  Kathmandu           np       977   \n" +
					"Sudan                                  Khartoum            sd       249   \n" +
					"Ukraine                                Kiev                ua       380   \n" +
					"Rwanda                                 Kigali              rw       250   \n" +
					"Jamaica                                Kingston            jm       1-876 \n" +
					"Norfolk Island                         Kingston            nf       672   \n" +
					"Saint Vincent & Grenadines             Kingstown           vc       1-784 \n" +
					"Congo, Dem. Republic                   Kinshasa            cd       243   \n" +
					"Moldova                                Kishinev            md       373   \n" +
					"Palau                                  Koror               pw       680   \n" +
					"Malaysia                               Kuala Lumpur        my       60    \n" +
					"Kuwait                                 Kuwait City         kw       965   \n" +
					"Bolivia                                La Paz              bo       591   \n" +
					"Nigeria                                Lagos               ng       234   \n" +
					"Gabon                                  Libreville          ga       241   \n" +
					"Malawi                                 Lilongwe            mw       265   \n" +
					"Peru                                   Lima                pe       51    \n" +
					"Portugal                               Lisbon              pt       351   \n" +
					"Slovenia                               Ljubljana           si       386   \n" +
					"Togo                                   Lome                tg       228   \n" +
					"Great Britain                          London              gb       44    \n" +
				//	"U.K.                                   London              uk       44    \n" +
					"Svalbard and Jan Mayen Islands         Longyearbyen        sj             \n" +
					"Angola                                 Luanda              ao       244   \n" +
					"Zambia                                 Lusaka              zm       260   \n" +
					"Luxembourg                             Luxembourg          lu       352   \n" +
					"Macau                                  Macau               mo       853   \n" +
					"Spain                                  Madrid              es       34    \n" +
					"Marshall Islands                       Majuro              mh       692   \n" +
					"Equatorial Guinea                      Malabo              gq       240   \n" +
					"Maldives                               Male                mv       960   \n" +
					"Nicaragua                              Managua             ni       505   \n" +
					"Philippines                            Manila              ph       63    \n" +
					"Mozambique                             Maputo              mz       258   \n" +
					"Lesotho                                Maseru              ls       266   \n" +
					"Wallis and Futuna Islands              Mata-Utu            wf       681   \n" +
					"Swaziland                              Mbabane             sz       268   \n" +
					"Mexico                                 Mexico City         mx       52    \n" +
					"Belarus                                Minsk               by       375   \n" +
					"Somalia                                Mogadishu           so       252   \n" +
					"Monaco                                 Monaco              mc       377   \n" +
					"Liberia                                Monrovia            lr       231   \n" +
					"Uruguay                                Montevideo          uy       598   \n" +
					"Comoros                                Moroni              km       269   \n" +
					"Russia                                 Moscow              ru       7     \n" +
					"Oman                                   Muscat              om       968   \n" +
					"Kenya                                  Nairobi             ke       254   \n" +
					"Bahamas                                Nassau              bs       1-242 \n" +
					"Myanmar                                Naypyidaw           mm       95    \n" +
				//	"Chad                                   N'Djamena           td       235   \n" +
					"Chad                                   Ndjamena            td       235   \n" +
					"India                                  New Delhi           in       91    \n" +
					"Niger                                  Niamey              ne       227   \n" +
					"Cyprus                                 Nicosia             cy       357   \n" +
					"Antarctica                             None                aq       672   \n" +
					"Bouvet Island                          None                bv             \n" +
					"British Indian Ocean Territory         None                io             \n" +
					"French Southern Territories            None                tf             \n" +
					"Heard Island and McDonald Islands      None                hm             \n" +
					"South Georgia & South Sandwich Islands None                gs             \n" +
					"Tokelau                                None                tk       690   \n" +
				//	"USA Minor Outlying Islands             None                um             \n" +
					"Mauritania                             Nouakchott          mr       222   \n" +
					"New Caledonia (French)                 Noumea              nc       687   \n" +
					"Tonga                                  Nuku'alofa          to       676   \n" +
					"Aruba                                  Oranjestad          aw       297   \n" +
					"Norway                                 Oslo                no       47    \n" +
					"Canada                                 Ottawa              ca       1     \n" +
					"Burkina Faso                           Ouagadougou         bf       226   \n" +
					"American Samoa                         Pago Pago           as       684   \n" +
					"Micronesia                             Palikir             fm       691   \n" +
					"Panama                                 Panama City         pa       507   \n" +
					"Polynesia (French)                     Papeete             pf       689   \n" +
					"Suriname                               Paramaribo          sr       597   \n" +
					"France                                 Paris               fr       33    \n" +
					"Cambodia                               Phnom Penh          kh       855   \n" +
					"Montserrat                             Plymouth            ms       1-664 \n" +
					"Montenegro                             Podgorica           me       382   \n" +
				//	"Mauritius                              Port Louis          mu       230   \n" +
					"Mauritius                              Port-Louis          mu       230   \n" +
					"Papua New Guinea                       Port Moresby        pg       675   \n" +
				//	"Trinidad and Tobago                    Port of Spain       tt       1-868 \n" +
					"Trinidad and Tobago                    Port-of-Spain       tt       1-868 \n" +
					"Vanuatu                                Port Vila           vu       678   \n" +
					"Haiti                                  Port-au-Prince      ht       509   \n" +
					"Benin                                  Porto-Novo          bj       229   \n" +
					"Czech Rep.                             Prague              cz       420   \n" +
					"Cape Verde                             Praia               cv       238   \n" +
					"South Africa                           Pretoria            za       27    \n" +
					"Korea-North                            Pyongyang           kp       850   \n" +
					"Ecuador                                Quito               ec       593   \n" +
					"Morocco                                Rabat               ma       212   \n" +
					"Iceland                                Reykjavik           is       354   \n" +
					"Latvia                                 Riga                lv       371   \n" +
					"Saudi Arabia                           Riyadh              sa       966   \n" +
					"Virgin Islands (British)               Road Town           vg       1-284 \n" +
					"Italy                                  Rome                it       39    \n" +
					"Dominica                               Roseau              dm       1-767 \n" +
					"Jersey                                 Saint Helier        je             \n" +
					"Reunion (French)                       Saint-Denis         re       262   \n" +
					"Northern Mariana Islands               Saipan              mp       670   \n" +
					"Costa Rica                             San Jose            cr       506   \n" +
					"Puerto Rico                            San Juan            pr       1-787 \n" +
					"San Marino                             San Marino          sm       378   \n" +
					"El Salvador                            San Salvador        sv       503   \n" +
				//	"Yemen                                  San'a               ye       967   \n" +
					"Yemen                                  Sanaa               ye       967   \n" +
					"Chile                                  Santiago            cl       56    \n" +
					"Dominican Republic                     Santo Domingo       do       809   \n" +
					"Sao Tome and Principe                  Sao Tome            st       239   \n" +
					"Bosnia-Herzegovina                     Sarajevo            ba       387   \n" +
					"Korea-South                            Seoul               kr       82    \n" +
					"Singapore                              Singapore           sg       65    \n" +
					"Macedonia                              Skopje              mk       389   \n" +
					"Bulgaria                               Sofia               bg       359   \n" +
				//	"Grenada                                St. George's        gd       1-473 \n" +
					"Grenada                                Saint George's      gd       1-473 \n" +					
				//	"Antigua and Barbuda                    Saint Johns         ag       1-268 \n" +
					"Antigua and Barbuda                    Saint John's        ag       1-268 \n" +
				//	"Guernsey                               St. Peter Port      gg             \n" +
					"Guernsey                               Saint Peter Port    gg             \n" +
				//	"Saint Pierre and Miquelon              St. Pierre          pm       508   \n" +
					"Saint Pierre and Miquelon             	Saint Pierre        pm       508   \n" +
					"Falkland Islands                       Stanley             fk       500   \n" +
					"Sweden                                 Stockholm           se       46    \n" +
					"Fiji                                   Suva                fj       679   \n" +
					"Taiwan                                 Taipei              tw       886   \n" +
					"Estonia                                Tallinn             ee       372   \n" +
					"Kiribati                               Tarawa              ki       686   \n" +
					"Uzbekistan                             Tashkent            uz       998   \n" +
					"Georgia                                Tbilisi             ge       995   \n" +
					"Honduras                               Tegucigalpa         hn       504   \n" +
					"Iran                                   Tehran              ir       98    \n" +
					"Christmas Island                       The Settlement      cx       61    \n" +
					"Anguilla                               The Valley          ai       1-264 \n" +
					"Bhutan                                 Thimphu             bt       975   \n" +
					"Albania                                Tiran               al       355   \n" +
					"Japan                                  Tokyo               jp       81    \n" +
					"Faroe Islands                          Torshavn            fo       298   \n" +
					"Libya                                  Tripoli             ly       218   \n" +
					"Tunisia                                Tunis               tn       216   \n" +
					"Mongolia                               Ulan Bator          mn       976   \n" +
					"Liechtenstein                          Vaduz               li       423   \n" +
					"Malta                                  Valletta            mt       356   \n" +
					"Vatican                                Vatican City        va       39    \n" +
					"Hong Kong                              Victoria            hk       852   \n" +
					"Seychelles                             Victoria            sc       248   \n" +
					"Austria                                Vienna              at       43    \n" +
					"Laos                                   Vientiane           la       856   \n" +
					"Lithuania                              Vilnius             lt       370   \n" +
					"Poland                                 Warsaw              pl       48    \n" +
					"USA                                    Washington          us       1     \n" +
					"New Zealand                            Wellington          nz       64    \n" +
					"Cocos (Keeling) Islands                West Island         cc       61    \n" +
					"Netherlands Antilles                   Willemstad          an       599   \n" +
					"Namibia                                Windhoek            na       264   \n" +
					"Cameroon                               Yaounde             cm       237   \n" +
					"Nauru                                  Yaren               nr       674   \n" +
					"Armenia                                Yerevan             am       374   \n" +
					"Croatia                                Zagreb              hr       385   \n" +
					// added manually since it was missing in the above list
					"Palestinian territories                Ramallah            ps       970   \n";

	/*
	private static final HashMap<String,String> countryCodeByCapital = new HashMap<String,String>();
	*/
	private static final HashMap<String,String> countryByCountryCode = new HashMap<String,String>();
	private static final HashMap<String,String> capitalByCountryCode = new HashMap<String,String>();
	static 
	{
		String[] lines = capitals.split("\n");
		
		for (String line : lines)
		{
			String land = line.substring( 0,38).trim();
			String city = line.substring(39,58).trim();
			String code = line.substring(59,61).toUpperCase();
			
			capitalByCountryCode.put(code, city.equalsIgnoreCase("None") ? land : city);
			countryByCountryCode.put(code, land);
			/*
			countryByCapital.put    (city, land);
			countryCodeByCapital.put(city, code);
			*/
		}		
	}
}
