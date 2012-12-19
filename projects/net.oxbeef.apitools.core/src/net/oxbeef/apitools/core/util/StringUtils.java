package net.oxbeef.apitools.core.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {
	/**
	 * Parses a comma-separated list 
	 * and returns patterns as an array of Strings 
	 * or <code>null</code> if none.
	 * 
	 * @param patterns comma separated list or <code>null</code>
	 * @return individual patterns or <code>null</code>
	 */
	public static String[] parsePatterns(String patterns) {
		if (patterns == null || patterns.trim().length() == 0) {
			return null;
		}
		String[] strings = patterns.split(","); //$NON-NLS-1$
		List list = new ArrayList();
		for (int i = 0; i < strings.length; i++) {
			String pattern = strings[i].trim();
			if (pattern.length() > 0) {
				list.add(pattern);
			}
		}
		return (String[]) list.toArray(new String[list.size()]);
	}

	public static String convertToHtml(String s) {
		char[] contents = s.toCharArray();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = contents.length; i < max; i++) {
			char c = contents[i];
			switch (c) {
				case '<':
					buffer.append("&lt;"); //$NON-NLS-1$
					break;
				case '>':
					buffer.append("&gt;"); //$NON-NLS-1$
					break;
				case '\"':
					buffer.append("&quot;"); //$NON-NLS-1$
					break;
				case '&':
					buffer.append("&amp;"); //$NON-NLS-1$
					break;
				case '^':
					buffer.append("&and;"); //$NON-NLS-1$
					break;
				default:
					buffer.append(c);
			}
		}
		return String.valueOf(buffer);
	}

	
	// Require-Bundle: org.eclipse.jst.jee; bundle-version="1.0.401"
	public static String getRequiredBundleName(String s) {
		String pluginId = s.substring("Require-Bundle: ".length());
		int ind = pluginId.indexOf(";");
		if( ind != -1 ) {
			pluginId = pluginId.substring(0, ind);
		}
		return pluginId;
	}
	
	public static String indent(int x) {
		StringBuffer bf = new StringBuffer();
		for( int i = 0; i < x; i++ ) {
			bf.append(" ");
		}
		return bf.toString();
	}
}
