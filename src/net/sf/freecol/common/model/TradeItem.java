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


/**
 * One of the items a DiplomaticTrade consists of.
 */
public abstract class TradeItem extends FreeColObject {

    /** The game this TradeItem belongs to. */
    protected Game game;

    /** The player who is to provide this item. */
    private Player source;

    /** The player who is to receive this item. */
    private Player destination;


    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game The <code>Game</code> the trade occurs in.
     * @param id An id for this item.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     */
    public TradeItem(Game game, String id, Player source, Player destination) {
        this.game = game;
        setId(id);
        this.source = source;
        this.destination = destination;
    }

    /**
     * Creates a new <code>TradeItem</code> instance.
     *
     * @param game The <code>Game</code> the trade occurs in.
     * @param in The <code>XMLStreamReader</code> to read from.
     */
    public TradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;
    }

    /**
     * Gets the game.  The subclasses need this.
     *
     * @return The game.
     */
    protected final Game getGame() {
        return game;
    }

    /**
     * Get the source player.
     *
     * @return The source <code>Player</code>.
     */
    public final Player getSource() {
        return source;
    }

    /**
     * Set the source player.
     *
     * @param newSource The new source <code>Player</code>.
     */
    public final void setSource(final Player newSource) {
        this.source = newSource;
    }

    /**
     * Get the destination player.
     *
     * @return The destination <code>Player</code>.
     */
    public final Player getDestination() {
        return destination;
    }

    /**
     * Set the destination player.
     *
     * @param newDestination The new destination <code>Player</code>.
     */
    public final void setDestination(final Player newDestination) {
        this.destination = newDestination;
    }

    // The following routines must be supplied/overridden by the subclasses.

    /**
     * Is this trade item valid?
     *
     * @return True if the item is valid.
     */
    public abstract boolean isValid();

    /**
     * Is this trade item unique?
     * This is true for the StanceTradeItem and the GoldTradeItem,
     * and false for all others.
     *
     * @return True if the item is unique.
     */
    public abstract boolean isUnique();

    /**
     * Get the colony to trade.
     *
     * @return The <code>Colony</code> to trade.
     */
    public Colony getColony() { return null; }

    /**
     * Set the colony to trade.
     *
     * @param colony The new <code>Colony</code> to trade.
     */
    public void setColony(Colony colony) {}

    /**
     * Get the goods to trade.
     *
     * @return The <code>Goods</code> to trade.
     */
    public Goods getGoods() { return null; }

    /**
     * Set the goods to trade.
     *
     * @param goods The new <code>Goods</code> to trade.
     */
    public void setGoods(Goods goods) {}

    /**
     * Get the gold to trade.
     *
     * @return The gold to trade.
     */
    public int getGold() { return 0; }

    /**
     * Set the gold to trade.
     *
     * @param gold The new gold to trade.
     */
    public void setGold(int gold) {}

    /**
     * Get the stance to trade.
     *
     * @return The <code>Stance</code> to trade.
     */
    public Stance getStance() { return null; }

    /**
     * Set the stance to trade.
     *
     * @param stance The new <code>Stance</code> to trade.
     */
    public void setStance(Stance stance) {}

    /**
     * Get the unit to trade.
     *
     * @return The <code>Unit</code> to trade.
     */
    public Unit getUnit() { return null; }

    /**
     * Set the unit to trade.
     *
     * @param unit The new <code>Unit</code> to trade.
     */
    public void setUnit(Unit unit) {}


    // Serialization

    private static final String DESTINATION_TAG = "destination";
    private static final String SOURCE_TAG = "source";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, SOURCE_TAG, source);

        writeAttribute(out, DESTINATION_TAG, destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        source = getAttribute(in, SOURCE_TAG, game,
                              Player.class, (Player)null);

        destination = getAttribute(in, DESTINATION_TAG, game,
                                   Player.class, (Player)null);
    }
}
