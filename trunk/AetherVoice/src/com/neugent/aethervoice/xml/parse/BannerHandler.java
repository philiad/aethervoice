package com.neugent.aethervoice.xml.parse;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
 
 
public class BannerHandler extends DefaultHandler{
 
        // ===========================================================
        // Fields
        // ===========================================================       
        private boolean in_AndroidTelPadAdBannerDef = false;
        private boolean in_AdBannerId = false;
        private boolean in_Description = false;
        private boolean in_ImageURL = false;
        private boolean in_FileName = false;
        private boolean in_PageURL = false;       
        
       
        private BannerDataset banners;
        
        private Vector<BannerDataset> all_banners;       
        
 
        // ===========================================================
        // Getter & Setter
        // ===========================================================
 
        public Vector<BannerDataset> getParsedData() {        		
                return this.all_banners;
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
        	
        		if (localName.equals("DialerAdBanner_GetAllResponse")) {        			
        			all_banners = new Vector<BannerDataset>();
        			
        		} else if (localName.equals("DialerAdBanner_GetAllResult")) {        			
        			
        		} else if (localName.equals("AndroidTelPadAdBannerDef")) {        			
        			banners = new BannerDataset();  
        			this.in_AndroidTelPadAdBannerDef = true;                        
                        
                }else if (localName.equals("AdBannerId")) {                		              		
                	this.in_AdBannerId = true;
                        
                } else if (localName.equals("Description")) {
                	this.in_Description = true;
                		
                } else if (localName.equals("FileName")) {
                	this.in_FileName = true;
            			
                } else if (localName.equals("ImageURL")) {
                	this.in_ImageURL = true;
            			
                } else if (localName.equals("PageURL")) {
                	//banners.setPage_url(atts.getValue(0));
                	this.in_PageURL = true;                		
                }	 	

        }
       
        /** Gets be called on closing tags like:
         * </tag> */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
                
                if (localName.equals("AndroidTelPadAdBannerDef")) {                  		
                		all_banners.add(banners);
                    	this.in_AndroidTelPadAdBannerDef = false;
                    
	            }else if (localName.equals("AdBannerId")) {	            		
	                    this.in_AdBannerId = false;
	                    
	            } else if (localName.equals("Description")) {
	            		this.in_Description = false;
	            		
	            } else if (localName.equals("FileName")) {
	        			this.in_FileName = false;
	        			
	            } else if (localName.equals("ImageURL")) {	            		
	        			this.in_ImageURL = false;
	        			
	            } else if (localName.equals("PageURL")) {            		
	        			this.in_PageURL = false;                		
	            }
        }
       
        /** Gets be called on the following structure:
         * <tag>characters</tag> */
        @Override
        public void characters(char ch[], int start, int length) {
        		if (in_AdBannerId) {        			
        			banners.setBannerid(new String(ch, start, length).trim());        			
        		} else  if (in_Description) {
        			banners.setDescription(new String(ch, start, length).trim());  
        		} else if (in_FileName) {
        			banners.setFilename(new String(ch, start, length).trim());  
        		} else  if (in_ImageURL) {
        			banners.setImage_url(new String(ch, start, length).trim());  
        		} else  if (in_PageURL) {
        			banners.setPage_url(new String(ch, start, length).trim());   
        		} 
        }
}
