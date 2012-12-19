/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.oxbeef.apitools.core.ant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.jdt.core.Signature;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This task can be used to convert the report generated by the apitooling.apifreeze task
 * into an html report.
 */
public class APIFreezeReportConversionTask extends Task {
	static final class ConverterDefaultHandler extends DefaultHandler {
		private static final String API_BASELINE_DELTAS = "Added and removed bundles"; //$NON-NLS-1$
		private String[] arguments;
		private List argumentsList;
		private String componentID;
		private boolean debug;
		private int flags;
		private String key;
		private String kind;
		private Map map;
		private String typename;
		private int elementType;

		public ConverterDefaultHandler(boolean debug) {
			this.map = new HashMap();
			this.debug = debug;
		}
		public void endElement(String uri, String localName, String name)
			throws SAXException {
			if (IApiXmlConstants.DELTA_ELEMENT_NAME.equals(name)) {
				Entry entry = new Entry(
						this.flags,
						this.elementType,
						this.key,
						this.typename,
						this.arguments,
						this.kind);
				Object object = this.map.get(this.componentID);
				if (object != null) {
					((List) object).add(entry);
				} else {
					ArrayList value = new ArrayList();
					value.add(entry);
					this.map.put(componentID, value);
				}
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS.equals(name)) {
				if (this.argumentsList != null && this.argumentsList.size() != 0) {
					this.arguments = new String[this.argumentsList.size()];
					this.argumentsList.toArray(this.arguments);
				}
			}
		}

		public Map getEntries() {
			return this.map;
		}
		/*
		 * Only used in debug mode
		 */
		private void printAttribute(Attributes attributes, String name) {
			System.out.println("\t" + name + " = " + String.valueOf(attributes.getValue(name))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		public void startElement(String uri, String localName,
				String name, Attributes attributes) throws SAXException {
			if (IApiXmlConstants.DELTA_ELEMENT_NAME.equals(name)) {
				if (this.debug) {
					System.out.println("name : " + name); //$NON-NLS-1$
					/*<delta
					 *  compatible="true"
					 *  componentId="org.eclipse.equinox.p2.ui_0.1.0"
					 *  element_type="CLASS_ELEMENT_TYPE"
					 *  flags="25"
					 *  key="schedule(Lorg/eclipse/equinox/internal/provisional/p2/ui/operations/ProvisioningOperation;Lorg/eclipse/swt/widgets/Shell;I)Lorg/eclipse/core/runtime/jobs/Job;"
					 *  kind="ADDED"
					 *  oldModifiers="9"
					 *  newModifiers="9"
					 *  restrictions="0"
					 *  type_name="org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner"/>
					 */
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPATIBLE);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE);
					printAttribute(attributes, IApiXmlConstants.ATTR_FLAGS);
					printAttribute(attributes, IApiXmlConstants.ATTR_KEY);
					printAttribute(attributes, IApiXmlConstants.ATTR_KIND);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_NEW_MODIFIERS);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_OLD_MODIFIERS);
					printAttribute(attributes, IApiXmlConstants.ATTR_RESTRICTIONS);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_TYPE_NAME);
				}
				final String value = attributes.getValue(IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
				if (value == null) {
					// removed or added bundles
					this.componentID = API_BASELINE_DELTAS;
				} else {
					this.componentID = value;
				}
				this.flags = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_FLAGS));
				this.elementType = Util.getDeltaElementTypeValue(attributes.getValue(IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE));
				this.typename = attributes.getValue(IApiXmlConstants.ATTR_NAME_TYPE_NAME);
				this.key = attributes.getValue(IApiXmlConstants.ATTR_KEY);
				this.kind = attributes.getValue(IApiXmlConstants.ATTR_KIND);
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS.equals(name)) {
				if (this.argumentsList == null) {
					this.argumentsList = new ArrayList();
				} else {
					this.argumentsList.clear();
				}
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENT.equals(name)) {
				this.argumentsList.add(attributes.getValue(IApiXmlConstants.ATTR_VALUE));
			}
		}
	}
	static class Entry {
		String[] arguments;
		int flags;
		int elementType;
		String key;
		String typeName;
		String kind;

		private static final String ADDED = "ADDED"; //$NON-NLS-1$
		private static final String REMOVED = "REMOVED"; //$NON-NLS-1$

		public Entry(
				int flags,
				int elementType,
				String key,
				String typeName,
				String[] arguments,
				String kind) {
			this.flags = flags;
			this.key = key.replace('/', '.');
			if (typeName != null) {
				this.typeName = typeName.replace('/', '.');
			}
			this.arguments = arguments;
			this.kind = kind;
			this.elementType = elementType;
		}
		
		public String getDisplayString() {
			StringBuffer buffer = new StringBuffer();
			if(this.typeName != null && this.typeName.length() != 0) {
				buffer.append(this.typeName);
				switch(this.flags) {
					case IDelta.API_METHOD_WITH_DEFAULT_VALUE :
					case IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE :
					case IDelta.API_METHOD :
					case IDelta.METHOD :
					case IDelta.METHOD_WITH_DEFAULT_VALUE :
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						int indexOf = this.key.indexOf('(');
						if (indexOf == -1) {
							return null;
						}
						int index = indexOf;
						String selector = key.substring(0, index);
						String descriptor = key.substring(index, key.length());
						buffer.append('#');
						buffer.append(Signature.toString(descriptor, selector, null, false, true));
						break;
					case IDelta.API_CONSTRUCTOR :
					case IDelta.CONSTRUCTOR :
						indexOf = key.indexOf('(');
						if (indexOf == -1) {
							return null;
						}
						index = indexOf;
						selector = key.substring(0, index);
						descriptor = key.substring(index, key.length());
						buffer.append('#');
						buffer.append(Signature.toString(descriptor, selector, null, false, false));
						break;
					case IDelta.FIELD :
					case IDelta.API_FIELD :
					case IDelta.ENUM_CONSTANT :
					case IDelta.API_ENUM_CONSTANT :
						buffer.append('#');
						buffer.append(this.key);
						break;
					case IDelta.TYPE_MEMBER :
					case IDelta.API_TYPE :
					case IDelta.REEXPORTED_TYPE :
					case IDelta.REEXPORTED_API_TYPE :
						buffer.append('.');
						buffer.append(this.key);
						break;
					case IDelta.DEPRECATION :
						switch(this.elementType) {
							case IDelta.ANNOTATION_ELEMENT_TYPE :
							case IDelta.INTERFACE_ELEMENT_TYPE :
							case IDelta.ENUM_ELEMENT_TYPE :
							case IDelta.CLASS_ELEMENT_TYPE :
								buffer.append('.');
								buffer.append(this.key);
								break;
							case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
								indexOf = key.indexOf('(');
								if (indexOf == -1) {
									return null;
								}
								index = indexOf;
								selector = key.substring(0, index);
								descriptor = key.substring(index, key.length());
								buffer.append('#');
								buffer.append(Signature.toString(descriptor, selector, null, false, false));
								break;
							case IDelta.METHOD_ELEMENT_TYPE :
								indexOf = key.indexOf('(');
								if (indexOf == -1) {
									return null;
								}
								index = indexOf;
								selector = key.substring(0, index);
								descriptor = key.substring(index, key.length());
								buffer.append('#');
								buffer.append(Signature.toString(descriptor, selector, null, false, true));
								break;
							case IDelta.FIELD_ELEMENT_TYPE :
								buffer.append('#');
								buffer.append(this.key);
						}
				}
			} else {
				switch(this.flags) {
					case IDelta.MAJOR_VERSION :
						buffer.append(NLS.bind(
								Messages.deltaReportTask_entry_major_version,
								this.arguments));
						break;
					case IDelta.MINOR_VERSION :
						buffer.append(NLS.bind(
								Messages.deltaReportTask_entry_minor_version,
								this.arguments));
						break;
					case IDelta.API_BASELINE_ELEMENT_TYPE :
						buffer.append(this.key);
						break;
				}
			}
			return CommonUtilsTask.convertToHtml(String.valueOf(buffer));
		}
		public String getDisplayKind() {
			if (ADDED.equals(this.kind)) {
				return Messages.AddedElement;
			} else if (REMOVED.equals(this.kind)) {
				return Messages.RemovedElement;
			}
			return Messages.ChangedElement;
		}
	}
	boolean debug;

	private String htmlFileLocation;
	private String xmlFileLocation;

	private void dumpEndEntryForComponent(StringBuffer buffer, String componentID) {
		buffer.append(NLS.bind(Messages.deltaReportTask_endComponentEntry, componentID));
	}
	private void dumpEntries(Map entries, StringBuffer buffer) {
		dumpHeader(buffer);
		Set entrySet = entries.entrySet();
		List allEntries = new ArrayList();
		for (Iterator iterator = entrySet.iterator(); iterator.hasNext(); ) {
			allEntries.add(iterator.next());
		}
		Collections.sort(allEntries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry entry1 = (Map.Entry) o1;
				Map.Entry entry2 = (Map.Entry) o2;
				return ((String) entry1.getKey()).compareTo((String) entry2.getKey());
			}
		});
		for (Iterator iterator = allEntries.iterator(); iterator.hasNext(); ) {
			Map.Entry mapEntry = (Map.Entry) iterator.next();
			String key = (String) mapEntry.getKey();
			Object value = mapEntry.getValue();
			dumpEntryForComponent(buffer, key);
			if (value instanceof List) {
				List values = (List) value;
				Collections.sort(values, new Comparator() {
					public int compare(Object o1, Object o2) {
						Entry entry1 = (Entry) o1;
						Entry entry2 = (Entry) o2;
						String typeName1 = entry1.typeName;
						String typeName2 = entry2.typeName;
						if (typeName1 == null) {
							if (typeName2 == null) {
								return entry1.key.compareTo(entry2.key);
							}
							return -1;
						} else if (typeName2 == null) {
							return 1;
						}
						if (!typeName1.equals(typeName2)) {
							return typeName1.compareTo(typeName2);
						}
						return entry1.key.compareTo(entry2.key);
					}
				});
				if (debug) {
					System.out.println("Entries for " + key); //$NON-NLS-1$
				}
				for (Iterator iterator2 = ((List)value).iterator(); iterator2.hasNext(); ) {
					Entry entry = (Entry) iterator2.next();
					if (debug) {
						if (entry.typeName != null) {
							System.out.print(entry.typeName);
							System.out.print('#');
						}
						System.out.println(entry.key);
					}
					dumpEntry(buffer, entry);
				}
			}
			dumpEndEntryForComponent(buffer, key);
		}
		dumpFooter(buffer);
	}
	private void dumpEntry(StringBuffer buffer, Entry entry) {
		buffer.append(NLS.bind(Messages.deltaReportTask_entry, entry.getDisplayKind(), entry.getDisplayString()));
	}
	private void dumpEntryForComponent(StringBuffer buffer, String componentID) {
		buffer.append(NLS.bind(Messages.deltaReportTask_componentEntry, componentID));
	}

	private void dumpFooter(StringBuffer buffer) {
		buffer.append(Messages.deltaReportTask_footer);
	}
	private void dumpHeader(StringBuffer buffer) {
		buffer.append(Messages.deltaReportTask_header);
	}
	/**
	 * Run the ant task
	 */
	public void execute() throws BuildException {
		if (this.xmlFileLocation == null) {
			throw new BuildException(Messages.deltaReportTask_missingXmlFileLocation);
		}
		if (this.debug) {
			System.out.println("xmlFileLocation : " + this.xmlFileLocation); //$NON-NLS-1$
			System.out.println("htmlFileLocation : " + this.htmlFileLocation); //$NON-NLS-1$
		}
		File file = new File(this.xmlFileLocation);
		if (!file.exists()) {
			throw new BuildException(
					NLS.bind(Messages.deltaReportTask_missingXmlFile, this.xmlFileLocation));
		}
		if (file.isDirectory()) {
			throw new BuildException(
					NLS.bind(Messages.deltaReportTask_xmlFileLocationMustBeAFile, this.xmlFileLocation));
		}
		File outputFile = null;
		if (this.htmlFileLocation == null) {
			int index = this.xmlFileLocation.lastIndexOf('.');
			if (index == -1
					|| !this.xmlFileLocation.substring(index).toLowerCase().equals(".xml")) { //$NON-NLS-1$
				throw new BuildException(Messages.deltaReportTask_xmlFileLocationShouldHaveAnXMLExtension);
			}
			this.htmlFileLocation = extractNameFromXMLName(index);
			if (this.debug) {
				System.out.println("output name :" + this.htmlFileLocation); //$NON-NLS-1$
			}
			outputFile = new File(this.htmlFileLocation);
		} else {
			// check if the htmlFileLocation is a file and not a directory
			int index = this.htmlFileLocation.lastIndexOf('.');
			if (index == -1
					|| !this.htmlFileLocation.substring(index).toLowerCase().equals(".html")) { //$NON-NLS-1$
				throw new BuildException(Messages.deltaReportTask_htmlFileLocationShouldHaveAnHtmlExtension);
			}
			outputFile = new File(this.htmlFileLocation);
			if (outputFile.exists()) {
				// if the file already exist, we check that this is a file
				if (outputFile.isDirectory()) {
					throw new BuildException(
							NLS.bind(Messages.deltaReportTask_hmlFileLocationMustBeAFile, outputFile.getAbsolutePath()));
				}
			} else {
				File parentFile = outputFile.getParentFile();
				if (!parentFile.exists()) {
					if (!parentFile.mkdirs()) {
						throw new BuildException(
								NLS.bind(Messages.errorCreatingParentReportFile, parentFile.getAbsolutePath()));
					}
				}
			}
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		if (parser == null) {
			throw new BuildException(Messages.deltaReportTask_couldNotCreateSAXParser);
		}
		try {
			ConverterDefaultHandler defaultHandler = new ConverterDefaultHandler(this.debug);
			parser.parse(file, defaultHandler);
			StringBuffer buffer = new StringBuffer();
			dumpEntries(defaultHandler.getEntries(), buffer);
			writeOutput(buffer);
		} catch (SAXException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
	}
	private String extractNameFromXMLName(int index) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.xmlFileLocation.substring(0, index)).append(".html"); //$NON-NLS-1$
		return String.valueOf(buffer);
	}
	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	/**
	 * Set the path of the html file to generate.
	 * 
	 * <p>The location is set using an absolute path.</p>
	 * 
	 * <p>This is optional. If not set, the html file name is retrieved from the xml file
	 * name by replacing ".xml" in ".html".</p>
	 * 
	 * @param htmlFilePath the path of the html file to generate
	 */
	public void setHtmlFile(String htmlFilePath) {
		this.htmlFileLocation = htmlFilePath;
	}
	/**
	 * Set the path of the xml file to convert to html.
	 * 
	 * <p>The path is set using an absolute path.</p>
	 * 
	 * @param xmlFilePath the path of the xml file to convert to html
	 */
	public void setXmlFile(String xmlFilePath) {
		this.xmlFileLocation = xmlFilePath;
	}
	private void writeOutput(StringBuffer buffer) throws IOException {
		FileWriter writer = null;
		BufferedWriter bufferedWriter = null;
		try {
			writer = new FileWriter(this.htmlFileLocation);
			bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(String.valueOf(buffer));
		} finally {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
		}
	}
}
