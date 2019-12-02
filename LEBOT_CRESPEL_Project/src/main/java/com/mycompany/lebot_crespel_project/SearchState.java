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

import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.StopShooting;

/**
 *
 * @author r5lebot
 */
public class SearchState extends Behavior {
    
    @Override
    public void react(HunterBot bot) {
        bot.getLog().info("[[ Search State ]]");
        pursueCount++;
        if (bot.getInfo().isShooting() || bot.getInfo().isSecondaryShooting()) {
                    // stop shooting
                bot.getAct().act(new StopShooting());
        }
        if (bot.getEnemy() != null) {
        	bot.getNavigation().navigate(bot.getEnemy());
        	bot.setItem(null);
        } else {
        	if (bot.getNavigation().isNavigating()) return;
        bot.getNavigation().navigate(bot.getNavPoints().getRandomNavPoint());
        }
    }
}
