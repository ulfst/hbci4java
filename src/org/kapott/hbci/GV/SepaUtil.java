package org.kapott.hbci.GV;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.structures.Value;

/**
 * Ein paar statische Hilfs-Methoden fuer die Generierung der SEPA-Nachrichten.
 */
public class SepaUtil
{
    private final static String DATE_PATTERN   = "yyyy-MM-dd'T'HH:mm:ss";
    private final static Pattern INDEX_PATTERN = Pattern.compile("\\w+\\[(\\d+)\\](\\..*)?");

    /**
     * Erzeugt ein neues XMLCalender-Objekt.
     * @param isoDate optional. Das zu verwendende Datum.
     * Wird es weggelassen, dann wird das aktuelle Datum (mit Uhrzeit) verwendet.
     * @return das XML-Calendar-Objekt.
     * @throws Exception
     */
    public static XMLGregorianCalendar createCalendar(String isoDate) throws Exception
    {
        if (isoDate == null)
        {
            SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
            isoDate = format.format(new Date());
        }
        
        DatatypeFactory df = DatatypeFactory.newInstance();
        return df.newXMLGregorianCalendar(isoDate);
    }


    /**
     * Ermittelt den maximalen Index aller indizierten Properties. Nicht indizierte Properties
     * werden ignoriert.
     * 
     * @param properties die Properties, mit denen gearbeitet werden soll
     * @return Maximaler Index, oder {@code null}, wenn keine indizierten Properties gefunden wurden
     */
    public static Integer maxIndex(Properties properties)
    {
        Integer max = null;
        for (String key : properties.stringPropertyNames())
        {
            Matcher m = INDEX_PATTERN.matcher(key);
            if (m.matches())
            {
                int index = Integer.parseInt(m.group(1));
                if (max == null || index > max)
                {
                    max = index;
                }
            }
        }
        return max;
    }

    /**
     * Liefert die Summe der Betr�ge aller Transaktionen. Bei einer Einzeltransaktion wird der
     * Betrag zur�ckgeliefert. Mehrfachtransaktionen m�ssen die gleiche W�hrung verwenden, da
     * eine Summenbildung sonst nicht m�glich ist.
     * 
     * @param sepaParams die Properties, mit denen gearbeitet werden soll
     * @param max Maximaler Index, oder {@code null} f�r Einzeltransaktionen
     * @return Summe aller Betr�ge
     */
    public static BigDecimal sumBtgValue(Properties sepaParams, Integer max)
    {
        if (max == null)
            return new BigDecimal(sepaParams.getProperty("btg.value"));

        BigDecimal sum = BigDecimal.ZERO;
        String curr = null;

        for (int index = 0; index <= max; index++)
        {
            sum = sum.add(new BigDecimal(sepaParams.getProperty(insertIndex("btg.value", index))));

            // Sicherstellen, dass alle Transaktionen die gleiche W�hrung verwenden
            String indexCurr = sepaParams.getProperty(insertIndex("btg.curr", index));
            if (curr != null)
            {
                if (!curr.equals(indexCurr)) {
                    throw new InvalidArgumentException("mixed currencies on multiple transactions");
                }
            }
            else
            {
                curr = indexCurr;
            }
        }
        return sum;
    }

    /**
     * Fuegt einen Index in den Property-Key ein. Wurde kein Index angegeben, wird der Key
     * unveraendert zurueckgeliefert.
     * 
     * @param key Key, der mit einem Index ergaenzt werden soll
     * @param index Index oder {@code null}, wenn kein Index gesetzt werden soll
     * @return Key mit Index
     */
    public static String insertIndex(String key, Integer index)
    {
        if (index == null)
            return key;

        int pos = key.indexOf('.');
        if (pos >= 0)
        {
            return key.substring(0, pos) + '[' + index + ']' + key.substring(pos);
        }
        else
        {
            return key + '[' + index + ']';
        }
    }

    /**
     * Liefert ein Value-Objekt mit den Summen des Auftrages.
     * @param properties Auftrags-Properties.
     * @return das Value-Objekt mit der Summe.
     */
    public static Value sumBtgValueObject(Properties properties)
    {
        Integer maxIndex = maxIndex(properties);
        BigDecimal btg = sumBtgValue(properties, maxIndex);
        String curr = properties.getProperty(insertIndex("btg.curr", maxIndex == null ? null : 0));
        return new Value(btg, curr);
    }
    
    /**
     * Liefert den Wert des Properties oder den Default-Wert.
     * Der Default-Wert wird nicht nur bei NULL verwendet sondern auch bei Leerstring.
     * @param props die Properties.
     * @param name der Name des Properties.
     * @param defaultValue der Default-Wert.
     * @return der Wert.
     */
    public static String getProperty(Properties props, String name, String defaultValue)
    {
        String value = props.getProperty(name);
        return value != null && value.length() > 0 ? value : defaultValue;
    }
}


