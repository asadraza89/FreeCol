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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class Event extends FreeColGameObjectType {

    /** A restriction on the scope of the event. */
    private String value;

    /** The score value of this event. */
    private int scoreValue = 0;

    /** Limits on this event. */
    private Map<String, Limit> limits = null;



    /**
     * Create a new event.
     *
     * @param id The event id.
     * @param specification The containing <code>Specification</code>.
     */
    public Event(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Gets the event restriction.
     *
     * @return The restriction.
     */
    public final String getValue() {
        return value;
    }

    /**
     * Sets the event restriction.
     *
     * @param newValue The new event restriction.
     */
    public final void setValue(final String newValue) {
        this.value = newValue;
    }

    /**
     * Get the limits on this event.
     *
     * @return A list of limits.
     */
    public final Collection<Limit> getLimits() {
        if (limits == null) return Collections.emptyList();
        return limits.values();
    }

    /**
     * Gets a particular limit by id.
     *
     * @param id The id to look for.
     * @return The corresponding <code>Limit</code> or null if not found.
     */
    public final Limit getLimit(String id) {
        return (limits == null) ? null : limits.get(id);
    }

    /**
     * Get the score value of this event.
     *
     * @return The score value.
     */
    public final int getScoreValue() {
        return scoreValue;
    }

    /**
     * Set the score value of this event.
     *
     * @param newScoreValue The new score value.
     */
    public final void setScoreValue(final int newScoreValue) {
        this.scoreValue = newScoreValue;
    }


    // Serialization

    private static final String SCORE_VALUE_TAG = "scoreValue";


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

        if (value != null) {
            writeAttribute(out, VALUE_TAG, value);
        }

        if (scoreValue != 0) {
            writeAttribute(out, SCORE_VALUE_TAG, scoreValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (Limit limit : getLimits()) limit.toXML(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        value = getAttribute(in, VALUE_TAG, (String)null);

        scoreValue = getAttribute(in, SCORE_VALUE_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            limits = null;
        }
        
        super.readChildren(in);
    }        

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (Limit.getXMLElementTagName().equals(tag)) {
            Limit limit = new Limit(getSpecification());
            limit.readFromXML(in);
            if (limits == null) limits = new HashMap<String, Limit>();
            limits.put(limit.getId(), limit);

            // @compat 0.10.5
            if (limit.getId().equals("model.limit.independence.colonies")) {
                limit.setId("model.limit.independence.coastalColonies");
                limit.getLeftHandSide().setMethodName("isConnectedPort");
            }
            // end @compat

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "event".
     */
    public static String getXMLElementTagName() {
        return "event";
    }
}
