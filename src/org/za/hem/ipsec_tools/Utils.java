package org.za.hem.ipsec_tools;

import java.lang.StringBuffer;

public class Utils {

	public static <T> String join(T[] array, String delimiter) {
	    if (array.length == 0)
	    	return "";
	    int i = 0;
	    StringBuffer buffer = new StringBuffer(array[i++].toString());
	    for (; i < array.length; i++) {
	    	buffer.append(delimiter).append(array[i++]);
	    }
	    return buffer.toString();
	}
}
