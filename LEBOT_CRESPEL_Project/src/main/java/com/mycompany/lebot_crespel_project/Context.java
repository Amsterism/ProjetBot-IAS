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

/**
 *
 * @author r5lebot
 */
public class Context {
    
    private HunterBot bot;
    private Behavior state = new IdleState();
    
    public Context(HunterBot bot){
        this.bot = bot;
    }
    
    public void reaction(){
        state.react(bot);
    }
    
    public void setState(Behavior state){
        this.state = state;
    }
    
    public Behavior getState(){
        return this.state;
    } 
}
