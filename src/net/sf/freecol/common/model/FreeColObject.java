/**
 *  Copyright (C) 2002-2012   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public abstract class FreeColObject {

    protected static Logger logger = Logger.getLogger(FreeColObject.class.getName());

    public static final int INFINITY = Integer.MAX_VALUE;
    public static final int UNDEFINED = Integer.MIN_VALUE;

    public static final String NO_ID = "NO_ID";

    public static final String ID_ATTRIBUTE = "ID";

    /**
     * XML tag name for value attribute.
     */
    protected static final String VALUE_TAG = "value";

    /**
     * XML tag name for ID attribute.
     */
    // this is what we use for the specification
    // TODO: standardize on this spelling
    public static final String ID_ATTRIBUTE_TAG = "id";

    /**
     * XML tag name for array elements.
     */
    protected static final String ARRAY_SIZE = "xLength";

    /**
     * XML attribute tag to denote partial updates.
     */
    protected static final String PARTIAL_ATTRIBUTE = "PARTIAL";

    /**
     * Unique identifier of an object
     */
    private String id;

    /** The <code>Specification</code> this object uses, which may be null. */
    private Specification specification;

    /** An optional property change container, allocated on demand. */
    private PropertyChangeSupport pcs = null;


    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    protected void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Describe <code>getSpecification</code> method here.
     *
     * @return a <code>Specification</code> value
     */
    public Specification getSpecification() {
        return specification;
    }

    /**
     * Sets the specification for this object. This method should only
     * ever be used by the object's constructor.
     *
     * @param specification a <code>Specification</code> value
     */
    protected void setSpecification(Specification specification) {
        this.specification = specification;
    }

    /**
     * Debugging tool, dump object XML to System.err.
     */
    public void dumpObject() {
        save(System.err);
    }

    /**
     * Writes the object to the given file.
     *
     * @param file the save file
     * @exception FileNotFoundException
     */
    public void save(File file) throws FileNotFoundException {
        save(new FileOutputStream(file));
    }

    /**
     * Writes the object to the given output stream
     *
     * @param out the OutputStream
     */
    public void save(OutputStream out) {
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xsw = null;
        try {
            xsw = xof.createXMLStreamWriter(out, "UTF-8");
            xsw.writeStartDocument("UTF-8", "1.0");
            this.toXML(xsw, null, true, true);
            xsw.writeEndDocument();
            xsw.flush();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception writing object.", e);
        } finally {
            try {
                if (xsw != null) xsw.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception closing save stream.", e);
            }
        }
    }

    /**
     * Serialize this FreeColObject to a string.
     *
     * @param player The <code>Player</code> this XML-representation
     *     should be made for, or null if <code>showAll == true</code>.
     * @param showAll Show all attributes.
     * @param toSavedGame Also show some extra attributes when saving the game.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize(Player player, boolean showAll,
                            boolean toSavedGame) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLOutputFactory xif = XMLOutputFactory.newInstance();
        XMLStreamWriter out = xif.createXMLStreamWriter(sw);
        this.toXML(out, player, showAll, toSavedGame);
        out.close();
        return sw.toString();
    }

    /**
     * Unserialize from XML to a FreeColObject.
     *
     * @param xml The xml serialized version of an object.
     * @param game The <code>Game</code> to add the object to.
     * @param returnClass The required object class.
     * @return The unserialized object.
     * @exception XMLStreamException if there are any problems reading
     *     from the stream.
     */
    public static <T extends FreeColObject> T unserialize(String xml,
        Game game, Class<T> returnClass) throws XMLStreamException {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader in = xif.createXMLStreamReader(new StringReader(xml));
        in.nextTag();

        T ret = null;
        try {
            Constructor<T> c = returnClass.getConstructor(Game.class,
                                                          String.class);
            ret = c.newInstance(game, (String)null);
        } catch (NoSuchMethodException nsme) { // Specific to getConstructor
            logger.log(Level.WARNING, "No constructor for "
                + returnClass.getName(), nsme);
        } catch (Exception e) { // Handles multiple fails from newInstance
            logger.log(Level.WARNING, "Failed to instantiate "
                + returnClass.getName(), e);
        }
        if (ret != null) ret.readFromXML(in);
        return ret;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Document document) {
        // since the player is null, showAll must be true
        return toXMLElement(null, document, true, false);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Player player, Document document) {
        return toXMLElement(player, document, true, false);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Player player, Document document,
                                boolean showAll, boolean toSavedGame) {
        return toXMLElement(player, document, showAll, toSavedGame, null);
    }

    /**
     * This method writes a partial XML-representation of this object to
     * an element using only the mandatory and specified fields.
     *
     * @param document The <code>Document</code>.
     * @param fields The fields to write.
     * @return An XML-representation of this object.
     */
    public Element toXMLElementPartial(Document document, String... fields) {
        return toXMLElement(null, document, true, false, fields);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @param fields An array of field names, which if non-null
     *               indicates this should be a partial write.
     * @return An XML-representation of this object.
     */
    public Element toXMLElement(Player player, Document document,
                                boolean showAll, boolean toSavedGame,
                                String[] fields) {
        try {
            StringWriter sw = new StringWriter();
            XMLOutputFactory xif = XMLOutputFactory.newInstance();
            XMLStreamWriter xsw = xif.createXMLStreamWriter(sw);
            if (fields == null) {
                toXML(xsw, player, showAll, toSavedGame);
            } else {
                toXMLPartialImpl(xsw, fields);
            }
            xsw.close();

            DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
            Document tempDocument = null;
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                tempDocument = builder.parse(new InputSource(new StringReader(sw.toString())));
                return (Element)document.importNode(tempDocument.getDocumentElement(), true);
            } catch (ParserConfigurationException pce) {
                // Parser with specified options can't be built
                logger.log(Level.WARNING, "ParserConfigurationException", pce);
                throw new IllegalStateException("ParserConfigurationException: "
                    + pce.getMessage());
            } catch (SAXException se) {
                logger.log(Level.WARNING, "SAXException", se);
                throw new IllegalStateException("SAXException: "
                    + se.getMessage());
            } catch (IOException ie) {
                logger.log(Level.WARNING, "IOException", ie);
                throw new IllegalStateException("IOException: "
                    + ie.getMessage());
            }
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
            throw new IllegalStateException("XMLStreamException: "
                + e.getMessage());
        }
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * All attributes will be made visible.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     * @see #toXML(XMLStreamWriter, Player, boolean, boolean)
     */
    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out);
    }

    /**
     * This method writes an XML-representation of this object with
     * a specified tag to the given stream.
     *
     * Almost all FreeColObjects end up calling these, and implementing
     * their own write{Attributes,Children} methods which begin by
     * calling their superclass.  This allows a clean nesting of the
     * serialization routines throughout the class hierarchy.
     *
     * All attributes will be made visible.
     *
     * @param out The target stream.
     * @param tag The tag to use.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public void toXML(XMLStreamWriter out, String tag) throws XMLStreamException {
        out.writeStartElement(tag);
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * To be overridden if required by any object that has attributes
     * and uses the toXML(XMLStreamWriter, String) call.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        if (getId() == null) {
            logger.warning("FreeColObject with null id: " + toString());
        } else {
            writeAttribute(out, ID_ATTRIBUTE_TAG, getId());
        }
    }

    /**
     * Write the children of this object to a stream.
     *
     * To be overridden if required by any object that has children
     * and uses the toXML(XMLStreamWriter, String) call.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        // do nothing
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(XMLStreamWriter out, Player player,
                      boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // FreeColObjects are not to contain data that varies with
        // the observer, so the extra arguments are moot here.
        // However, this method is overridden in FreeColGameObject
        // where they are meaningful, and we need a version here for
        // toXMLElement() to call.
        toXMLImpl(out);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    abstract protected void toXMLImpl(XMLStreamWriter out)
        throws XMLStreamException;

    /**
     * This method writes a partial XML-representation of this object to
     * the given stream using only the mandatory and specified fields.
     * Ideally this would be abstract, but as not all FreeColObject-subtypes
     * need partial updates we provide a non-operating stub here which is
     * to be overridden where needed.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        throw new UnsupportedOperationException("Partial update of unsupported type.");
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param element An XML-element that will be used to initialize
     *      this object.
     */
    public void readFromXMLElement(Element element) {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer xmlTransformer = factory.newTransformer();
                StringWriter stringWriter = new StringWriter();
                xmlTransformer.transform(new DOMSource(element), new StreamResult(stringWriter));
                String xml = stringWriter.toString();
                XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
                xsr.nextTag();
                readFromXML(xsr);
            } catch (TransformerException e) {
                logger.log(Level.WARNING, "TransformerException", e);
                throw new IllegalStateException("TransformerException");
            }
        } catch (XMLStreamException e) {
            logger.log(Level.WARNING, "XMLStreamException", e);
            throw new IllegalStateException("XMLStreamException");
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param element An XML-element that will be used to initialize
     *      this object.
     * @param specification a <code>Specification</code> value
     */
    public void readFromXMLElement(Element element, Specification specification) {
        setSpecification(specification);
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer xmlTransformer = factory.newTransformer();
                StringWriter stringWriter = new StringWriter();
                xmlTransformer.transform(new DOMSource(element), new StreamResult(stringWriter));
                String xml = stringWriter.toString();
                XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
                xsr.nextTag();
                readFromXML(xsr);
            } catch (TransformerException e) {
                logger.log(Level.WARNING, "TransformerException", e);
                throw new IllegalStateException("TransformerException");
            }
        } catch (XMLStreamException e) {
            logger.log(Level.WARNING, "XMLStreamException", e);
            throw new IllegalStateException("XMLStreamException");
        }
    }


    /**
     * Initializes this object from an XML-representation of this object,
     * unless the PARTIAL_ATTRIBUTE tag is present which indicates
     * a partial update of an existing object.
     *
     * @param in The input stream with the XML.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {
        if (in.getAttributeValue(null, PARTIAL_ATTRIBUTE) == null) {
            readAttributes(in);
            readChildren(in);
        } else {
            readFromXMLPartialImpl(in);
        }
    }

    /**
     * Reads an XML-representation of an array.
     *
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param arrayType The type of array to be read.
     * @return The array.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected int[] readFromArrayElement(String tagName, XMLStreamReader in, int[] arrayType)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }

        int[] array = new int[Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE))];

        for (int x=0; x<array.length; x++) {
            array[x] = Integer.parseInt(in.getAttributeValue(null, "x" + Integer.toString(x)));
        }

        in.nextTag();
        return array;
    }

    /**
     * Reads an XML-representation of a list.
     *
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param type The type of the items to be added. This type
     *      needs to have a constructor accepting a single
     *      <code>String</code>.
     * @return The list.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected <T> List<T> readFromListElement(String tagName, XMLStreamReader in, Class<T> type)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            throw new XMLStreamException(tagName + " expected, not:" + in.getLocalName());
        }
        final int length = Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE));
        List<T> list = new ArrayList<T>(length);
        for (int x = 0; x < length; x++) {
            try {
                final String value = in.getAttributeValue(null, "x" + Integer.toString(x));
                final T object;
                if (value != null) {
                    Constructor<T> c = type.getConstructor(type);
                    object = c.newInstance(new Object[] {value});
                } else {
                    object = null;
                }
                list.add(object);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException(tagName + " end expected, not: " + in.getLocalName());
        }
        return list;
    }

    /**
     * Reads an XML-representation of an array.
     *
     * @param tagName The tagname for the <code>Element</code>
     *       representing the array.
     * @param in The input stream with the XML.
     * @param arrayType The type of array to be read.
     * @return The array.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected String[] readFromArrayElement(String tagName, XMLStreamReader in, String[] arrayType)
        throws XMLStreamException {
        if (!in.getLocalName().equals(tagName)) {
            in.nextTag();
        }
        String[] array = new String[Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE))];
        for (int x=0; x<array.length; x++) {
            array[x] = in.getAttributeValue(null, "x" + Integer.toString(x));
        }

        in.nextTag();
        return array;
    }

    /**
     * Is there an attribute present in a stream?
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName An attribute name
     * @return True if the attribute is present.
     */
    public boolean hasAttribute(XMLStreamReader in, String attributeName) {
        final String attrib = in.getAttributeValue(null, attributeName);
        return attrib != null;
    }

    /**
     * Gets a boolean from an attribute in a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The boolean attribute value, or the default value if none found.
     */
    public boolean getAttribute(XMLStreamReader in, String attributeName,
                                boolean defaultValue) {
        final String attrib = in.getAttributeValue(null, attributeName);

        return (attrib == null) ? defaultValue
            : Boolean.parseBoolean(attrib);
    }

    /**
     * Gets a float from an attribute in a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The float attribute value, or the default value if none found.
     */
    public float getAttribute(XMLStreamReader in, String attributeName,
                              float defaultValue) {
        final String attrib = in.getAttributeValue(null, attributeName);

        float result = defaultValue;
        if (attrib != null) {
            try {
                result = Float.parseFloat(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not a float: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets an int from an attribute in a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The int attribute value, or the default value if none found.
     */
    public int getAttribute(XMLStreamReader in, String attributeName,
                            int defaultValue) {
        final String attrib = in.getAttributeValue(null, attributeName);

        int result = defaultValue;
        if (attrib != null) {
            try {
                result = Integer.parseInt(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not an integer: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a string from an attribute in a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The string attribute value, or the default value if none found.
     */
    public String getAttribute(XMLStreamReader in, String attributeName,
                               String defaultValue) {
        final String attrib = in.getAttributeValue(null, attributeName);

        return (attrib == null) ? defaultValue
            : attrib;
    }

    /**
     * Gets an enum from an attribute in a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param returnType The type of the return value.
     * @param defaultValue The default value.
     * @return The enum attribute value, or the default value if none found.
     */
    public <T extends Enum<T>> T getAttribute(XMLStreamReader in,
        String attributeName, Class<T> returnType, T defaultValue) {
        final String attrib = in.getAttributeValue(null, attributeName);

        T result = defaultValue;
        if (attrib != null) {
            try {
                result = Enum.valueOf(returnType,
                                      attrib.toUpperCase(Locale.US));
            } catch (Exception e) {
                logger.warning(attributeName + " is not a "
                    + defaultValue.getClass().getName() + ": " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a FreeCol object from an attribute in a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param game The <code>Game</code> to look in.
     * @param returnType The <code>FreeColObject</code> type to expect.
     * @param defaultValue The default value.
     * @return The <code>FreeColObject</code> found, or the default
     *     value if not.
     */
    public <T extends FreeColGameObject> T getAttribute(XMLStreamReader in,
        String attributeName, Game game, Class<T> returnType, T defaultValue) {
        final String attrib =
        // @compat 0.10.7
            (ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId(in) :
        // end @compat
            in.getAttributeValue(null, attributeName);

        return (attrib == null) ? defaultValue
            : game.getFreeColGameObject(attrib, returnType);
    }

    /**
     * Gets a FreeCol location from an attribute in a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param game The <code>Game</code> to look in.
     * @return The <code>Location</code>, or null if none found.
     */
    public Location getLocationAttribute(XMLStreamReader in,
                                         String attributeName, Game game) {
        final String attrib =
        // @compat 0.10.7
            (ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId(in) :
        // end @compat
            in.getAttributeValue(null, attributeName);
        
        return (attrib == null) ? null : game.getFreeColLocation(attrib);
    }
        
    /**
     * Write a boolean attribute to a stream.
     *
     * @param out The <code>XMLStreamWriter</code> to write to.
     * @param attributeName The attribute name.
     * @param value A boolean to write.
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName,
                               boolean value) throws XMLStreamException {
        out.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write a float attribute to a stream.
     *
     * @param out The <code>XMLStreamWriter</code> to write to.
     * @param attributeName The attribute name.
     * @param value A float to write.
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName,
                               float value) throws XMLStreamException {
        out.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write an integer attribute to a stream.
     *
     * @param out The <code>XMLStreamWriter</code> to write to.
     * @param attributeName The attribute name.
     * @param value An integer to write.
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName,
                               int value) throws XMLStreamException {
        out.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write an enum attribute to a stream.
     *
     * @param out The <code>XMLStreamWriter</code> to write to.
     * @param attributeName The attribute name.
     * @param value The <code>Enum</code> to write.
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName,
                               Enum<?> value) throws XMLStreamException {
        out.writeAttribute(attributeName,
                           value.toString().toLowerCase(Locale.US));
    }

    /**
     * Write an Object attribute to a stream.
     *
     * @param out The <code>XMLStreamWriter</code> to write to.
     * @param attributeName The attribute name.
     * @param value The <code>Object</code> to write.
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName,
                               Object value) throws XMLStreamException {
        out.writeAttribute(attributeName, String.valueOf(value));
    }

    /**
     * Write the id attribute of a non-null FreeColObject to a stream.
     *
     * @param out The <code>XMLStreamWriter</code> to write to.
     * @param attributeName The attribute name.
     * @param value The <code>FreeColObject</code> to write the id of.
     * @exception XMLStreamException if an error occurs
     */
    public void writeAttribute(XMLStreamWriter out, String attributeName,
                               FreeColObject value) throws XMLStreamException {
        if (value != null) {
            out.writeAttribute(attributeName, value.getId());
        }
    }

    /**
     * Write the id attribute of a non-null Location to a stream.
     *
     * @param out The <code>XMLStreamWriter</code> to write to.
     * @param attributeName The attribute name.
     * @param value The <code>Location</code> to write the id of.
     * @exception XMLStreamException if an error occurs
     */
    public void writeLocationAttribute(XMLStreamWriter out, String attributeName,
                                       Location value) throws XMLStreamException {
        if (value != null) {
            out.writeAttribute(attributeName,
                               ((FreeColGameObject)value).getId());
        }
    }

    // @compat 0.10.x
    /**
     * Reads the id attribute.
     *
     * Normally a simple getAttribute() would be sufficient, but
     * while we are allowing both the obsolete ID_ATTRIBUTE and the correct
     * ID_ATTRIBUTE_TAG, this routine is useful.
     *
     * When 0.10.x is obsolete, remove this routine and replace its uses
     * with getAttribute(in, ID_ATTRIBUTE_TAG, -1) or equivalent.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @return The id found, or null if none present.
     */
    public static String readId(XMLStreamReader in) {
        String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        if (id == null) id = in.getAttributeValue(null, ID_ATTRIBUTE);
        return id;
    }

    /**
     * Version of readId(XMLStreamReader) that reads from an element.
     *
     * To be replaced with just:
     *   element.getAttribute(FreeColObject.ID_ATTRIBUTE_TAG);
     *
     * @param element An element to read the id attribute from.
     * @return The id attribute value.
     */
    public static String readId(Element element) {
        String id = element.getAttribute(ID_ATTRIBUTE_TAG);
        if (id == null) id = element.getAttribute(ID_ATTRIBUTE);
        return id;
    }
    // end @compat

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        String newId = readId(in);
        setId(newId);
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
        }
    }

    /**
     * Reads a single child object.  Subclasses must override to read
     * their enclosed elements.  This particular instance of the
     * routine always throws XMLStreamException because we should
     * never arrive here.  However it is very useful to always call
     * super.readChild() when an unexpected tag is encountered, as the
     * exception thrown here provides some useful debugging context.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException because subclasses should have
     *     recognized all child elements.
     */
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        throw new XMLStreamException("Unexpected tag: " + currentTag(in));
    }

    /**
     * Updates this object from an XML-representation of this object.
     * Ideally this would be abstract, but as not all FreeColObject-subtypes
     * need partial updates we provide a non-operating stub here which is
     * to be overridden where needed.
     *
     * @param in The input stream with the XML.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLPartialImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Partial update of unsupported type: "
            + currentTag(in));
    }

    /**
     * Extract the current tag and its attributes from an input stream.
     * Useful for error messages.
     *
     * @param in The XML input stream.
     */
    public String currentTag(XMLStreamReader in) {
        StringBuilder sb = new StringBuilder(in.getLocalName());
        sb.append(", attributes:");
        int n = in.getAttributeCount();
        for (int i = 0; i < n; i++) {
            sb.append(" ").append(in.getAttributeLocalName(i))
                .append("=\"").append(in.getAttributeValue(i)).append("\"");
        }
        return sb.toString();
    }

    //  ---------- PROPERTY CHANGE SUPPORT DELEGATES ----------

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        if (pcs != null) {
            pcs.firePropertyChange(event);
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (pcs == null) {
            return new PropertyChangeListener[0];
        } else {
            return pcs.getPropertyChangeListeners();
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (pcs == null) {
            return new PropertyChangeListener[0];
        } else {
            return pcs.getPropertyChangeListeners(propertyName);
        }
    }

    public boolean hasListeners(String propertyName) {
        if (pcs == null) {
            return false;
        } else {
            return pcs.hasListeners(propertyName);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(propertyName, listener);
        }
    }

    // Feature container handling.

    /**
     * Gets the feature container for this object, if any.
     * None is provided here, but select subclasses will override.
     *
     * @return Null.
     */
    public FeatureContainer getFeatureContainer() {
        return null;
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The id of the ability to test.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id) {
        return hasAbility(id, null);
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColGameObjectType fcgot) {
        return hasAbility(id, fcgot, null);
    }

    /**
     * Is an ability present in this object?
     * Subclasses with complex ability handling should override this
     * routine.
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return True if the ability is present.
     */
    public boolean hasAbility(String id, FreeColGameObjectType fcgot,
                              Turn turn) {
        return FeatureContainer.hasAbility(getFeatureContainer(),
                                           id, fcgot, turn);
    }

    /**
     * Checks if this object contains a given ability key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public boolean containsAbilityKey(String key) {
        return FeatureContainer.containsAbilityKey(getFeatureContainer(),
                                                   key);
    }

    /**
     * Gets a copy of the abilities of this object.
     *
     * @return A set of abilities.
     */
    public Set<Ability> getAbilities() {
        return FeatureContainer.getAbilities(getFeatureContainer());
    }

    /**
     * Gets the set of abilities with the given Id from this object.
     *
     * @param id The id of the ability to test.
     * @return A set of abilities.
     */
    public final Set<Ability> getAbilitySet(String id) {
        return getAbilitySet(id, null);
    }

    /**
     * Gets the set of abilities with the given Id from this object.
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @return A set of abilities.
     */
    public final Set<Ability> getAbilitySet(String id,
                                            FreeColGameObjectType fcgot) {
        return getAbilitySet(id, fcgot, null);
    }

    /**
     * Gets the set of abilities with the given Id from this object.
     * Subclasses with complex ability handling should override this
     * routine.
     *
     * @param id The id of the ability to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     ability applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of abilities.
     */
    public Set<Ability> getAbilitySet(String id,
                                      FreeColGameObjectType fcgot,
                                      Turn turn) {
        return FeatureContainer.getAbilitySet(getFeatureContainer(),
                                              id, fcgot, turn);
    }

    /**
     * Add the given ability to this object.
     *
     * @param ability An <code>Ability</code> to add.
     * @return True if the ability was added.
     */
    public boolean addAbility(Ability ability) {
        return FeatureContainer.addAbility(getFeatureContainer(), ability);
    }

    /**
     * Remove the given ability from this object.
     *
     * @param ability An <code>Ability</code> to remove.
     * @return The ability removed.
     */
    public Ability removeAbility(Ability ability) {
        return FeatureContainer.removeAbility(getFeatureContainer(), ability);
    }

    /**
     * Remove all abilities with a given Id.
     *
     * @param id The id of the abilities to remove.
     */
    public void removeAbilities(String id) {
        FeatureContainer.removeAbilities(getFeatureContainer(), id);
    }


    /**
     * Checks if this object contains a given modifier key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public final boolean containsModifierKey(String key) {
        Set<Modifier> set = getModifierSet(key);
        return (set == null) ? false : !set.isEmpty();
    }

    /**
     * Gets a copy of the modifiers of this object.
     *
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifiers() {
        return FeatureContainer.getModifiers(getFeatureContainer());
    }

    /**
     * Gets the set of modifiers with the given Id from this object.
     *
     * @param id The id of the modifier to test.
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifierSet(String id) {
        return getModifierSet(id, null);
    }

    /**
     * Gets the set of modifiers with the given Id from this object.
     *
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return A set of modifiers.
     */
    public final Set<Modifier> getModifierSet(String id,
                                              FreeColGameObjectType fcgot) {
        return getModifierSet(id, fcgot, null);
    }

    /**
     * Gets the set of modifiers with the given Id from this object.
     *
     * Subclasses with complex modifier handling may override this
     * routine.
     *
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    public Set<Modifier> getModifierSet(String id,
                                        FreeColGameObjectType fcgot,
                                        Turn turn) {
        return FeatureContainer.getModifierSet(getFeatureContainer(), 
                                               id, fcgot, turn);
    }

    /**
     * Applies this objects modifiers with the given Id to the given number.
     *
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @return The modified number.
     */
    public final float applyModifier(float number, String id) {
        return applyModifier(number, id, null);
    }

    /**
     * Applies this objects modifiers with the given Id to the given number.
     *
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return The modified number.
     */
    public final float applyModifier(float number, String id,
                                     FreeColGameObjectType fcgot) {
        return applyModifier(number, id, fcgot, null);
    }

    /**
     * Applies this objects modifiers with the given Id to the given number.
     *
     * @param number The number to modify.
     * @param id The id of the modifiers to apply.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @return The modified number.
     */
    public final float applyModifier(float number, String id,
                                     FreeColGameObjectType fcgot, Turn turn) {
        return FeatureContainer.applyModifierSet(number, turn,
                                                 getModifierSet(id, fcgot, turn));
    }

    /**
     * Add the given modifier to this object.
     *
     * @param modifier An <code>Modifier</code> to add.
     * @return True if the modifier was added.
     */
    public boolean addModifier(Modifier modifier) {
        return FeatureContainer.addModifier(getFeatureContainer(), modifier);
    }

    /**
     * Remove the given modifier from this object.
     *
     * @param modifier An <code>Modifier</code> to remove.
     * @return The modifier removed.
     */
    public Modifier removeModifier(Modifier modifier) {
        return FeatureContainer.removeModifier(getFeatureContainer(), modifier);
    }

    /**
     * Remove all abilities with a given Id.
     *
     * @param id The id of the abilities to remove.
     */
    public void removeModifiers(String id) {
        FeatureContainer.removeModifiers(getFeatureContainer(), id);
    }


    /**
     * Adds all the features in an object to this object.
     *
     * @param fco The <code>FreeColObject</code> to add features from.
     */
    public void addFeatures(FreeColObject fco) {
        FeatureContainer.addFeatures(getFeatureContainer(), fco);
    }

    /**
     * Removes all the features in an object from this object.
     *
     * @param fco The <code>FreeColObject</code> to find features to remove in.
     */
    public void removeFeatures(FreeColObject fco) {
        FeatureContainer.removeFeatures(getFeatureContainer(), fco);
    }


    /**
     * Gets the tag name used to serialize this object, generally the
     * class name starting with a lower case letter. This method
     * should be overridden by all subclasses that need to be
     * serialized.
     *
     * @return <code>null</code>.
     */
    public static String getXMLElementTagName() {
        return null;
    }
}
