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
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;

/**
 *
 * @author r5lebot
 */

public class AttackState extends Behavior {
    
    @Override
    public void react(HunterBot bot) {
        bot.getLog().info("[[ Attack State ]]");
        
        boolean shooting = false;
        double distance = Double.MAX_VALUE;
        
        // 1) pick new enemy if the old one has been lost
        if (bot.getEnemy() == null || !bot.getEnemy().isVisible()) {
            // pick new enemy
            bot.setEnemy(bot.getPlayers().getNearestVisiblePlayer(bot.getPlayers().getVisibleEnemies().values()));
            if (bot.getEnemy() == null) {
//                bot.getLog().info("Can't see any enemies... ???");
                return;
            }
        }
        
        // 2) stop shooting if enemy is not visible
        if (!bot.getEnemy().isVisible()) {
	    if (bot.getInfo().isShooting() || bot.getInfo().isSecondaryShooting()) {
                    // stop shooting
                    bot.getAct().act(new StopShooting());
            }
            bot.setRunningToPlayer(false);            
        } else {
        	// 2) or shoot on enemy if it is visible
	        distance = bot.getInfo().getLocation().getDistance(bot.getEnemy().getLocation());
	        if (bot.getShoot().shoot(bot.getWeaponPrefs(), bot.getEnemy()) != null) {	            
	            shooting = true;
	        }
        }
        
        // 3) if enemy is far or not visible - run to him
        int decentDistance = Math.round(bot.getRandom().nextFloat() * 800) + 200;
        if (!bot.getEnemy().isVisible() || !shooting || decentDistance < distance) {
            if (!bot.getRunningToPlayer()) {
                bot.getNavigation().navigate(bot.getEnemy());
                bot.setRunningToPlayer(true);
            }
        } else {
            bot.setRunningToPlayer(false);
            bot.getNavigation().stopNavigation();
        }
        
        bot.setItem(null);
    }
}
