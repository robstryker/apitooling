package org.eclipse.pde.apitools.ant.internal;

import org.eclipse.pde.apitools.ant.util.IStyleSheetProvider;
import org.eclipse.pde.apitools.ant.util.XMLMemento;

public abstract class RootReport extends AbstractReport implements IStyleSheetProvider {
	private String entityName;
	private String sheetPath = null;
	
	public RootReport(String entityName) {
		this.entityName = entityName;
	}
	/*
	 * This should only be called on root elements.
	 * Root elements should override this and provide their own 
	 * root name
	 */
	public XMLMemento generateMemento() {
		XMLMemento xml = XMLMemento.createWriteRoot(entityName);
		
		if( this instanceof IStyleSheetProvider) {
			String s = ((IStyleSheetProvider)this).getStyleSheetPath();
			if( s != null ) {
				xml.addProcessingInstruction("xml-stylesheet",
						"type=\"text/xsl\" href=\"" + s + "\"");
			}
		}
		fillMemento(xml);
		return xml;
	}
	public String getStyleSheetPath() {
		return sheetPath;
	}
	
	public void setStyleSheetPath(String path) {
		this.sheetPath = path;
	}
}
