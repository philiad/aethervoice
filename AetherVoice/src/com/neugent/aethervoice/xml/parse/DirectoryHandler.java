package com.neugent.aethervoice.xml.parse;

import java.util.ArrayList;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
 
 
public class DirectoryHandler extends DefaultHandler{
 
        // ===========================================================
        // Fields
        // ===========================================================     
        private boolean in_DirectoryServiceDef2 = false;
        private boolean in_Category = false;
        private boolean in_City = false;
        private boolean in_CompanyName = false;
        private boolean in_FullAddress = false;
        private boolean in_Id = false;
        private boolean in_ListingCode = false;
        private boolean in_SubCategory = false;
        private boolean in_Telephone = false;
        private boolean in_TotalCount = false;
        
        private boolean first_record = false;
        
        private DirectoryDataset directory;
        private ArrayList<DirectoryDataset> all_directory; 
        
        private String totalRecords;
        
 
        // ===========================================================
        // Getter & Setter
        // ===========================================================
 
        public ArrayList<DirectoryDataset> getParsedData() {        		
            return this.all_directory;
        }
        
        public String getTotalRecords() {
        	return this.totalRecords;
        }
 
        // ===========================================================
        // Methods
        // ===========================================================
        @Override
        public void startDocument() throws SAXException {
        	// Nothing to do
        }
 
        @Override
        public void endDocument() throws SAXException {
        	// Nothing to do
        }
 
        
        /** Gets be called on opening tags like:
         * <tag>
         * Can provide attribute(s), when xml was like:
         * <tag attribute="attributeValue">*/
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        	
        		if (localName.equals("DirectoryService_SearchResponse")) {        			
        			all_directory = new ArrayList<DirectoryDataset>();
        			
        		} else if (localName.equals("DirectoryService_SearchResult")) {        			
        			
        		} else if (localName.equals("DirectoryServiceDef2")) {        			
        			directory = new DirectoryDataset();  
        			this.in_DirectoryServiceDef2 = true;                        
                        
                }else if (localName.equals("Category")) {  
                	directory.setCategory(atts.getValue(0));
                	this.in_Category = true;
                        
                } else if (localName.equals("City")) {
                	this.in_City = true;
                		
                } else if (localName.equals("CompanyName")) {
                	this.in_CompanyName = true;
            			
                } else if (localName.equals("FullAddress")) {
                	this.in_FullAddress = true;
            			
                } else if (localName.equals("Id")) {                	
                	this.in_Id = true;                		
                
                } else if (localName.equals("ListingCode")) {                	
                	this.in_ListingCode = true;  
                	
                } else if (localName.equals("SubCategory")) {                	
                	this.in_SubCategory = true;
                	
                } else if (localName.equals("Telephone")) {                	
                	this.in_Telephone = true;
                	
                } else if (localName.equals("TotalCount")) {  
                	if (!first_record) {
                		this.in_TotalCount = true;
                		this.first_record = true;
                	}
                	
                	
                }	 
        		

        }
       
        /** Gets be called on closing tags like:
         * </tag> */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
                
                if (localName.equals("DirectoryServiceDef2")) {                  		
                	all_directory.add(directory);
                    this.in_DirectoryServiceDef2 = false;
                    
                }else if (localName.equals("Category")) {                	
                	this.in_Category = false;
                        
                } else if (localName.equals("City")) {
                	this.in_City = false;
                		
                } else if (localName.equals("CompanyName")) {
                	this.in_CompanyName = false;
            			
                } else if (localName.equals("FullAddress")) {
                	this.in_FullAddress = false;
            			
                } else if (localName.equals("Id")) {                	
                	this.in_Id = false;                		
                
                } else if (localName.equals("ListingCode")) {                	
                	this.in_ListingCode = false;  
                	
                } else if (localName.equals("SubCategory")) {                	
                	this.in_SubCategory = false;
                	
                } else if (localName.equals("Telephone")) {                	
                	this.in_Telephone = false;
                	
                } else if (localName.equals("TotalCount")) {                	
                	this.in_TotalCount = false;
                	
                }
        }
       
        /** Gets be called on the following structure:
         * <tag>characters</tag> */
        @Override
        public void characters(char ch[], int start, int length) {
        		if (in_Category) {        			
        			//directory.setCategory(new String(ch, start, length)); 
        			
        		} else  if (in_City) {
        			directory.setCity(new String(ch, start, length).trim());  
        			
        		} else if (in_CompanyName) {
        			directory.setCompanyName(new String(ch, start, length).trim()); 
        			
        		} else  if (in_FullAddress) {
        			directory.setAddress(new String(ch, start, length).trim());  
        			
        		} else  if (in_Id) {
        			directory.setId(new String(ch, start, length).trim());
        			
        		} else  if (in_ListingCode) {
        			directory.setListingCode(new String(ch, start, length).trim());  
        		        			
        		} else  if (in_SubCategory) {
        			directory.setSubCategory(new String(ch, start, length).trim());
        			
        		} else  if (in_Telephone) {
        			directory.setTelephone(new String(ch, start, length).trim());
        			
        		} else  if (in_TotalCount) {
        			totalRecords = (new String(ch, start, length).trim()); 
        			
        		} 
        }
}
