/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
 *  http://exist-db.org
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Global table of mime types. This singleton class maintains a list
 * of mime types known to the system. It is used to look up the
 * mime type for a specific file extension and to check if a file
 * is an XML or binary resource.
 * 
 * The mime type table is read from a file "mime-types.xml",
 * which should reside in the directory identified by the system
 * property "exist.home". If no such file is found, the class tries
 * to load the default map from the org.exist.util package via the 
 * class loader.
 * 
 * @author wolf
 */
public class MimeTable {
    
    private static final String FILE_LOAD_FAILED_ERR = "Failed to load mime-type table from ";
    private static final String LOAD_FAILED_ERR = "Failed to load mime-type table from class loader";
    private static final String MIME_TYPES_XML_DEFAULT = "org/exist/util/mime-types.xml";
    private static final String MIME_TYPES_XML = "mime-types.xml";
    
    private final static Logger LOG = Logger.getLogger(MimeTable.class);
    
    private static MimeTable instance = null;
    
    /**
     * Returns the singleton.
     * 
     * @return
     */
    public static MimeTable getInstance() {
        if (instance == null)
            instance = new MimeTable();
        return instance;
    }
    
    private Map mimeTypes = new TreeMap();
    private Map extensions = new TreeMap();
    
    public MimeTable() {
        load();
    }
     
    public MimeType getContentTypeFor(String fileName) {
        String ext = getExtension(fileName);
        return ext == null ? null : (MimeType) extensions.get(ext);
    }
    
    public boolean isXMLContent(String fileName) {
        String ext = getExtension(fileName);
        if (ext == null)
            return false;
        MimeType type = (MimeType) extensions.get(ext);
        if (type == null)
            return false;
        return type.getType() == MimeType.XML;
    }
    
    private String getExtension(String fileName) {
        File f = new File(fileName);
        fileName = f.getName();
        int p = fileName.lastIndexOf('.');
        if (p < 0 || p + 1 == fileName.length())
            return null;
        return fileName.substring(p);
    }
    
    private void load() {
        boolean loaded = false;
        String home = System.getProperty("exist.home");
        if (home != null) {
            File f = new File(home + File.separatorChar + MIME_TYPES_XML);
            if (f.canRead()) {
                try {
                    LOG.debug("Loading mime table from file " + f.getAbsolutePath());
                    loadMimeTypes(new FileInputStream(f));
                    loaded = true;
                } catch (FileNotFoundException e) {
                    LOG.warn(FILE_LOAD_FAILED_ERR + f.getAbsolutePath(), e);
                } catch (ParserConfigurationException e) {
                    LOG.warn(FILE_LOAD_FAILED_ERR + f.getAbsolutePath(), e);
                } catch (SAXException e) {
                    LOG.warn(FILE_LOAD_FAILED_ERR + f.getAbsolutePath(), e);
                } catch (IOException e) {
                    LOG.warn(FILE_LOAD_FAILED_ERR + f.getAbsolutePath(), e);
                }
            }
        }
        if (!loaded) {
            LOG.debug("Loading mime table");
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream is = cl.getResourceAsStream(MIME_TYPES_XML_DEFAULT);
            if (is == null) {
                LOG.warn(LOAD_FAILED_ERR);
            }
            try {
                loadMimeTypes(is);
            } catch (ParserConfigurationException e) {
                LOG.warn(LOAD_FAILED_ERR, e);
            } catch (SAXException e) {
                LOG.warn(LOAD_FAILED_ERR, e);
            } catch (IOException e) {
                LOG.warn(LOAD_FAILED_ERR, e);
            }
        }
    }

    /**
     * @param stream
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws IOException 
     */
    private void loadMimeTypes(InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        InputSource src = new InputSource(stream);
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setContentHandler(new MimeTableHandler());
        reader.parse(new InputSource(stream));
    }

    private class MimeTableHandler extends DefaultHandler {

        private static final String EXTENSIONS = "extensions";
        private static final String DESCRIPTION = "description";
        private static final String MIME_TYPE = "mime-type";
        
        private MimeType mime = null;
        private FastStringBuffer charBuf = new FastStringBuffer(6, 15, 5);
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            if (MIME_TYPE.equals(qName)) {
                String name = attributes.getValue("name");
                if (name == null || name.length() == 0) {
                    LOG.warn("No name specified for mime-type");
                    return;
                }
                int type = MimeType.BINARY;
                String typeAttr = attributes.getValue("type");
                if (typeAttr != null && "xml".equals(typeAttr))
                    type = MimeType.XML;
                mime = new MimeType(name, type);
                mimeTypes.put(name, mime);
            }
            charBuf.setLength(0);
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (MIME_TYPE.equals(qName)) {
                mime = null;
            } else if (DESCRIPTION.equals(qName)) {
                if (mime != null) {
                    String description = charBuf.getNormalizedString(FastStringBuffer.SUPPRESS_BOTH);
                    mime.setDescription(description);
                }
            } else if (EXTENSIONS.equals(qName)) {
                if (mime != null) {
                    String extList = charBuf.getNormalizedString(FastStringBuffer.SUPPRESS_BOTH);
                    StringTokenizer tok = new StringTokenizer(extList, ", ");
                    while (tok.hasMoreTokens()) {
                        String ext = tok.nextToken();
                        if (!extensions.containsKey(ext))
                            extensions.put(ext, mime);
                    }
                }
            }
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            charBuf.append(ch, start, length);
        }
    }
    
    public static void main(String[] args) {
        MimeTable table = MimeTable.getInstance();
        MimeType type = table.getContentTypeFor("samples/xquery/fibo.svg");
        if (type == null) {
            System.out.println("Not found!");
        } else {
            System.out.println(type.getName());
            System.out.println(type.getDescription());
        }
    }
}
