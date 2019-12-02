/*
 * Copyright (C) 2019 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mycompany.lebot_crespel_project;

import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.StopShooting;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;

/**
 *
 * @author r5lebot
 */
public class HurtsState extends Behavior {
    
    @Override
    public void react(HunterBot bot) {
        bot.getLog().info("[[ Hurts State ]]");
        
	if (bot.getInfo().isShooting() || bot.getInfo().isSecondaryShooting()) {
            bot.getAct().act(new StopShooting());
        }
         
        Item item = bot.getItems().getPathNearestSpawnedItem(ItemType.Category.HEALTH);
        if (item == null) {
        	bot.getLog().warning("NO HEALTH ITEM TO RUN TO => ITEMS");
        	if (bot.getNavigation().isNavigating()) return;
        	bot.getNavigation().navigate(bot.getNavPoints().getRandomNavPoint());
        } else {
                if (bot.getNavigation().isNavigating()) return;
        	bot.getNavigation().navigate(item);
        	bot.setItem(item);
        }
    }
}
