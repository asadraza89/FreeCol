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

package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.Specification;


/**
 * Represents a list of Options.
 */
public class ListOption<T> extends AbstractOption<List<AbstractOption<T>>> {

    private static Logger logger = Logger.getLogger(ListOption.class.getName());

    /** The AbstractOption used to generate new values. */
    private AbstractOption<T> template;

    /** The maximum number of list entries. Defaults to Integer.MAX_VALUE. */
    private int maximumNumber = Integer.MAX_VALUE;

    /** The list of options. */
    private List<AbstractOption<T>> value = new ArrayList<AbstractOption<T>>();


    /**
     * Creates a new <code>ListOption</code>.
     *
     * @param id The identifier for this option.  This is used when
     *     the object should be found in an {@link OptionGroup}.
     */
    public ListOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>ListOption</code>.
     *
     * @param specification The enclosing <code>Specification</code>.
     */
    public ListOption(Specification specification) {
        super(specification);
    }


    /**
     * Gets the maximum number of allowed values.
     *
     * @return The maximum number of allowed values for this option.
     */
    public int getMaximumValue() {
        return maximumNumber;
    }

    /**
     * Gets the generating template.
     *
     * @return The template.
     */
    public AbstractOption<T> getTemplate() {
        return template;
    }

    /**
     * Get the values of the current non-null options in the list.
     *
     * @return A list of option values.
     */
    public List<T> getOptionValues() {
        List<T> result = new ArrayList<T>();
        for (AbstractOption<T> option : value) {
            if (option != null) result.add(option.getValue());
        }
        return result;
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public ListOption<T> clone() {
        return new ListOption<T>(getId());
    }

    /**
     * Gets the current value of this <code>ListOption</code>.
     *
     * @return The value.
     */
    public List<AbstractOption<T>> getValue() {
        return value;
    }

    /**
     * Sets the value of this <code>ListOption</code>.
     *
     * @param value The value to be set.
     */
    public void setValue(List<AbstractOption<T>> value) {
        // Fail fast: the list value may be empty, but it must not be null.
        if (value == null) {
            throw new IllegalArgumentException("Null ListOption");
        }
        final List<AbstractOption<T>> oldValue = this.value;
        this.value = value;

        if (isDefined && !value.equals(oldValue)) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNullValueOK() {
        return true;
    }


    // Serialization

    private static final String MAXIMUM_NUMBER_TAG = "maximumNumber";
    private static final String OPTION_VALUE_TAG = "optionValue";
    private static final String TEMPLATE_TAG = "template";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, MAXIMUM_NUMBER_TAG, maximumNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        if (template != null) {
            out.writeStartElement(TEMPLATE_TAG);
        
            template.toXML(out);
            
            out.writeEndElement();
        }

        for (AbstractOption option : value) {
            option.toXML(out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        maximumNumber = getAttribute(in, MAXIMUM_NUMBER_TAG, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        value.clear(); // Clear containers.

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override @SuppressWarnings("unchecked")
    public void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        // @compat 0.10.4
        if (OPTION_VALUE_TAG.equals(tag)) {
            String modId = readId(in);
            logger.finest("Found old-style mod value: " + modId);
            if (modId != null) {
                FreeColModFile fcmf = Mods.getModFile(modId);
                if (fcmf != null) {
                    ModOption modOption = new ModOption(modId);
                    modOption.setValue(fcmf);
                    value.add((AbstractOption<T>)modOption);
                }
            }
        // end @compat

        } else if (TEMPLATE_TAG.equals(tag)) {
            in.nextTag();
            template = (AbstractOption<T>)readOption(in);
            in.nextTag();

        } else {
            AbstractOption<T> option = (AbstractOption<T>)readOption(in);
            if (option != null) value.add(option);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "listOption".
     */
    public static String getXMLElementTagName() {
        return "listOption";
    }
}
