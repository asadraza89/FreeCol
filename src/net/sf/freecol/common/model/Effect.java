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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * The effect of a natural disaster or other event. How the
 * probability of the effect is interpreted depends on the number of
 * effects value of the disaster or event. If the number of effects is
 * ALL, the probability is ignored. If it is ONE, then the probability
 * may be an arbitrary integer, and is used only for comparison with
 * other effects. If the number of effects is SEVERAL, however, the
 * probability must be a percentage.
 *
 * @see Disaster
 */
public class Effect extends FreeColGameObjectType {

    public static final String DAMAGED_UNIT = "model.disaster.effect.damageUnit";
    public static final String LOSS_OF_UNIT = "model.disaster.effect.lossOfUnit";
    public static final String LOSS_OF_MONEY = "model.disaster.effect.lossOfMoney";
    public static final String LOSS_OF_GOODS = "model.disaster.effect.lossOfGoods";
    public static final String LOSS_OF_TILE_PRODUCTION = "model.disaster.effect.lossOfTileProduction";
    public static final String LOSS_OF_BUILDING_PRODUCTION = "model.disaster.effect.lossOfBuildingProduction";

    /** The probability of this effect. */
    private int probability;

    /** Scopes that might limit this Effect to certain types of objects. */
    private List<Scope> scopes = null;



    protected Effect() {
        // empty constructor
    }

    /**
     * Creates a new <code>Effect</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @param specification a <code>Specification</code> value
     * @exception XMLStreamException if an error occurs
     */
    public Effect(XMLStreamReader in, Specification specification) throws XMLStreamException {
        setSpecification(specification);
        readFromXML(in);
    }

    /**
     * Create a new effect from an existing one.
     *
     * @param template The <code>Effect</code> to copy from.
     */
    public Effect(Effect template) {
        setSpecification(template.getSpecification());
        setId(template.getId());
        this.probability = template.probability;
        this.scopes = template.scopes;
        addFeatures(template);
    }


    /**
     * Get the probability of this effect.
     *
     * @return The probability.
     */
    public final int getProbability() {
        return probability;
    }

    /**
     * Get the scopes applicable to this effect.
     *
     * @return A list of <code>Scope</code>s.
     */
    public final List<Scope> getScopes() {
        if (scopes == null) return Collections.emptyList();
        return scopes;
    }


    /**
     * Does at least one of this effect's scopes apply to an object.
     *
     * @param objectType The <code>FreeColGameObjectType</code> to check.
     * @return True if this effect applies.
     */
    public boolean appliesTo(final FreeColGameObjectType objectType) {
        if (scopes == null || scopes.isEmpty()) return true;
        for (Scope scope : scopes) {
            if (scope.appliesTo(objectType)) return true;
        }
        return false;
    }


    // Serialization

    private static final String PROBABILITY_TAG = "probability";


    /**
     * {@inheritDoc}
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, PROBABILITY_TAG, probability);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (Scope scope : getScopes()) scope.toXML(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        probability = getAttribute(in, PROBABILITY_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            scopes = null;
        }

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (Scope.getXMLElementTagName().equals(tag)) {
            Scope scope = new Scope(in);
            if (scope != null) {
                if (scopes == null) scopes = new ArrayList<Scope>();
                scopes.add(scope);
            }

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String result = "[" + getId() + "probability=" + probability + "%";
        for (Scope scope : getScopes()) result += " " + scope;
        result += "]";
        return result;
    }

    /**
     * Gets the XML tag name for this element.
     *
     * @return "effect".
     */
    public static String getXMLElementTagName() {
        return "effect";
    }
}
