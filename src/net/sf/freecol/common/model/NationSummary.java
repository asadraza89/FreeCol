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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Player.Stance;

import org.w3c.dom.Element;


/**
 * A summary of an enemy nation.
 */
public class NationSummary extends FreeColObject {

    /** The stance of the player toward the requesting player. */
    private Stance stance;

    /** The number of settlements this player has. */
    private int numberOfSettlements;

    /** The number of units this (European) player has. */
    private int numberOfUnits;

    /** The military strength of this (European) player. */
    private int militaryStrength;

    /** The naval strength of this (European) player. */
    private int navalStrength;

    /** The gold this (European) player has. */
    private int gold;

    /** The (European) player SoL. */
    private int soL;

    /** The number of founding fathers this (European) player has. */
    private int foundingFathers;

    /** The tax rate of this (European) player. */
    private int tax;


    /**
     * Creates a nation summary for the specified player.
     *
     * @param player The <code>Player</code> to create the summary for.
     * @param requester The <code>Player</code> making the request.
     */
    public NationSummary(Player player, Player requester) {
        setId("");

        stance = player.getStance(requester);
        if (stance == Stance.UNCONTACTED) stance = Stance.PEACE;

        numberOfSettlements = player.getSettlements().size();

        if (player.isEuropean()) {
            numberOfUnits = militaryStrength = navalStrength = 0;
            CombatModel cm = player.getGame().getCombatModel();
            for (Unit unit : player.getUnits()) {
                numberOfUnits++;
                if (unit.isNaval()) {
                    navalStrength += cm.getOffencePower(unit, null);
                } else {
                    militaryStrength += cm.getOffencePower(unit, null);
                }
            }

            gold = player.getGold();
            if (player == requester || requester
                .hasAbility("model.ability.betterForeignAffairsReport")) {
                soL = player.getSoL();
                foundingFathers = player.getFatherCount();
                tax = player.getTax();
            } else {
                soL = foundingFathers = tax = -1;
            }
        } else {
            numberOfUnits = militaryStrength = navalStrength = gold = soL
                = foundingFathers = tax = -1;
        }
    }

    /**
     * Creates a new <code>NationSummary</code> instance.
     *
     * @param element An <code>Element</code> value.
     */
    public NationSummary(Element element) {
        readFromXMLElement(element);
    }


    // Trivial accessors
    public Stance getStance() {
        return stance;
    }

    public int getNumberOfSettlements() {
        return numberOfSettlements;
    }

    public int getNumberOfUnits() {
        return numberOfUnits;
    }

    public int getMilitaryStrength() {
        return militaryStrength;
    }

    public int getNavalStrength() {
        return navalStrength;
    }

    public int getGold() {
        return gold;
    }

    public int getFoundingFathers() {
        return foundingFathers;
    }

    public int getSoL() {
        return soL;
    }

    public int getTax() {
        return tax;
    }


    // Serialization

    private static final String FOUNDING_FATHERS_TAG = "foundingFathers";
    private static final String GOLD_TAG = "gold";
    private static final String MILITARY_STRENGTH_TAG = "militaryStrength";
    private static final String NAVAL_STRENGTH_TAG = "navalStrength";
    private static final String NUMBER_OF_SETTLEMENTS_TAG = "numberOfSettlements";
    private static final String NUMBER_OF_UNITS_TAG = "numberOfUnits";
    private static final String SOL_TAG = "SoL";
    private static final String STANCE_TAG = "stance";
    private static final String TAX_TAG = "tax";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, NUMBER_OF_SETTLEMENTS_TAG, numberOfSettlements);

        writeAttribute(out, NUMBER_OF_UNITS_TAG, numberOfUnits);

        writeAttribute(out, MILITARY_STRENGTH_TAG, militaryStrength);

        writeAttribute(out, NAVAL_STRENGTH_TAG, navalStrength);

        writeAttribute(out, STANCE_TAG, stance);

        writeAttribute(out, GOLD_TAG, gold);

        if (soL >= 0) {
            writeAttribute(out, SOL_TAG, soL);
        }

        if (foundingFathers >= 0) {
            writeAttribute(out, FOUNDING_FATHERS_TAG, foundingFathers);
        }

        if (tax >= 0) {
            writeAttribute(out, TAX_TAG, tax);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        stance = getAttribute(in, STANCE_TAG, Stance.class, Stance.PEACE);

        numberOfSettlements = getAttribute(in, NUMBER_OF_SETTLEMENTS_TAG, -1);

        numberOfUnits = getAttribute(in, NUMBER_OF_UNITS_TAG, -1);

        militaryStrength = getAttribute(in, MILITARY_STRENGTH_TAG, -1);

        navalStrength = getAttribute(in, NAVAL_STRENGTH_TAG, -1);

        gold = getAttribute(in, GOLD_TAG, -1);

        soL = getAttribute(in, SOL_TAG, -1);

        foundingFathers = getAttribute(in, FOUNDING_FATHERS_TAG, -1);

        tax = getAttribute(in, TAX_TAG, -1);
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "nationSummary"
     */
    public static String getXMLElementTagName() {
        return "nationSummary";
    }
}
