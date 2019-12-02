package com.mycompany.lebot_crespel_project;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import cz.cuni.amis.introspection.java.JProp;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.Pogamut;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Players;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.UT2004DistanceStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.UT2004PositionStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.UT2004TimeStuckDetector;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Move;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Rotate;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Stop;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.StopShooting;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerKilled;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;

/**
 * Example of Simple Pogamut bot, that randomly walks around the map searching
 * for preys shooting at everything that is in its way.
 *
 * @author Rudolf Kadlec aka ik
 * @author Jimmy
 */
@AgentScoped
public class HunterBot extends UT2004BotModuleController<UT2004Bot> {

    /**
     * boolean switch to activate engage behavior
     */
    @JProp
    public boolean shouldEngage = true;
    /**
     * boolean switch to activate pursue behavior
     */
    @JProp
    public boolean shouldPursue = true;
    /**
     * boolean switch to activate rearm behavior
     */
    @JProp
    public boolean shouldRearm = true;
    /**
     * boolean switch to activate collect health behavior
     */
    @JProp
    public boolean shouldCollectHealth = true;
    /**
     * how low the health level should be to start collecting health items
     */
    @JProp
    public int healthLevel = 25;
    /**
     * how many bot the hunter killed other bots (i.e., bot has fragged them /
     * got point for killing somebody)
     */
    @JProp
    public int frags = 0;
    /**
     * how many times the hunter died
     */
    @JProp
    public int deaths = 0;

    /**
     * {@link PlayerKilled} listener that provides "frag" counting + is switches
     * the state of the hunter.
     *
     * @param event
     */
    @EventListener(eventClass = PlayerKilled.class)
    public void playerKilled(PlayerKilled event) {
        if (event.getKiller().equals(info.getId())) {
            ++frags;
        }
        if (enemy == null) {
            return;
        }
        if (enemy.getId().equals(event.getId())) {
            enemy = null;
        }
    }
    /**
     * Used internally to maintain the information about the bot we're currently
     * hunting, i.e., should be firing at.
     */
    
    public Context context = new Context(this);
    
    protected Player enemy = null;
    /**
     * Item we're running for. 
     */
    protected Item item = null;
    /**
     * Taboo list of items that are forbidden for some time.
     */
    protected TabooSet<Item> tabooItems = null;
    
    private UT2004PathAutoFixer autoFixer;
    
    private static int instanceCount = 0;

    /**
     * Bot's preparation - called before the bot is connected to GB2004 and
     * launched into UT2004.
     */
    @Override
    public void prepareBot(UT2004Bot bot) {
        tabooItems = new TabooSet<Item>(bot);

        autoFixer = new UT2004PathAutoFixer(bot, navigation.getPathExecutor(), fwMap, aStar, navBuilder); // auto-removes wrong navigation links between navpoints

        // listeners        
        navigation.getState().addListener(new FlagListener<NavigationState>() {

            @Override
            public void flagChanged(NavigationState changedValue) {
                switch (changedValue) {
                    case PATH_COMPUTATION_FAILED:
                    case STUCK:
                        if (item != null) {
                            tabooItems.add(item, 10);
                        }
                        reset();
                        break;

                    case TARGET_REACHED:
                        reset();
                        break;
                }
            }
        });

        // DEFINE WEAPON PREFERENCES
        weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);                
        weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);        
        weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);        
        weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);
    }

    /**
     * Here we can modify initializing command for our bot.
     *
     * @return
     */
    @Override
    public Initialize getInitializeCommand() {
        // just set the name of the bot and his skill level, 1 is the lowest, 7 is the highest
    	// skill level affects how well will the bot aim
        return new Initialize().setName("Hunter-" + (++instanceCount)).setDesiredSkill(2);
    }

    /**
     * Resets the state of the Hunter.
     */
    protected void reset() {
            item = null;
            enemy = null;
            navigation.stopNavigation();
            itemsToRunAround = null;
//            context.setState(new IdleState());
//            log.info("dead");
    }
    
    protected void resetDead() {
            item = null;
            enemy = null;
            navigation.stopNavigation();
            itemsToRunAround = null;
            context.setState(new IdleState());
//            log.info("dead");
    }
    
    
//    @EventListener(eventClass=PlayerDamaged.class)
//    public void playerDamaged(PlayerDamaged event) {
//    	log.info("I have just hurt other bot for: " + event.getDamageType() + "[" + event.getDamage() + "]");
//    }
//    
//    @EventListener(eventClass=BotDamaged.class)
//    public void botDamaged(BotDamaged event) {
//    	log.info("I have just been hurt by other bot for: " + event.getDamageType() + "[" + event.getDamage() + "]");
//    }

    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second - it
     * is affected by visionTime variable, that can be adjusted in GameBots ini
     * file in UT2004/System folder.
     *
     * @throws cz.cuni.amis.pogamut.base.exceptions.PogamutException
     */
    
    @Override
    public void logic() {  
        stateManagement();
        context.reaction();
    }

    protected boolean runningToPlayer = false;
    
    //////////////////////
    // STATE MANAGEMENT //
    //////////////////////
    
    protected void stateManagement() {
        
        if((botKilled())){
            resetDead();
            return;
        }
        
        // Idle State to Attack State
        if ((context.getState() instanceof IdleState) && shouldEngage && players.canSeeEnemies()) {
            context.setState(new AttackState());
            return;
        }
        
        // Attack State to Idle State
        if ((context.getState() instanceof AttackState) && botEnemyKilled()) {
            context.setState(new IdleState());
            return;
        }

        // Attack State to Dead State
        if ((context.getState() instanceof AttackState) && botKilled()) {
            context.setState(new DeadState());
            return;
        }

        // Attack State to Hurts State
        if ((context.getState() instanceof AttackState) && shouldCollectHealth && info.getHealth() < healthLevel) {
            context.setState(new HurtsState());
            return;
        }

        // Attack State to Search State
        if ((context.getState() instanceof AttackState) && !players.canSeeEnemies()) {
            context.setState(new SearchState());
            context.getState().pursueCount = 0;
            return;
        }
        
        // Hurts State to Attack State
        if ((context.getState() instanceof HurtsState) && info.getHealth() > healthLevel && players.canSeeEnemies()) {
            context.setState(new AttackState());
            return;
        }
        
        // Search State to Idle State
        if ((context.getState() instanceof SearchState) && context.getState().pursueCount > 20) {
            context.getState().pursueCount = 0;
            context.setState(new IdleState());
            return;
        }
        
        // Hurts State to Search State
        if ((context.getState() instanceof HurtsState) && info.getHealth() > healthLevel && !players.canSeeEnemies() && !botKilled()) {
            bot.getLog().info("" + context.getState().pursueCount);
            if(context.getState().pursueCount == 0){
            context.setState(new SearchState());}
            else{context.getState().pursueCount = 0;
            
            context.setState(new SearchState());}
            
            return;
        }
        
        // Hurts State to Dead State
        if ((context.getState() instanceof HurtsState) && botKilled()) {
            context.setState(new DeadState());
            return;
        }
      
        // Search State to Attack State
        if ((context.getState() instanceof SearchState) && players.canSeeEnemies()) {
            context.getState().pursueCount = 0;
            context.setState(new AttackState());
            return;
        }
        bot.getLog().info("" + context.getState().pursueCount);
    }

    protected List<Item> itemsToRunAround = null;

    protected void stateRunAroundItems() {
        //log.info("Decision is: ITEMS");
        //config.setName("Hunter [ITEMS]");
        if (navigation.isNavigatingToItem()) return;
        
        List<Item> interesting = new ArrayList<Item>();
        
        // ADD WEAPONS
        for (ItemType itemType : ItemType.Category.WEAPON.getTypes()) {
        	if (!weaponry.hasLoadedWeapon(itemType)) interesting.addAll(items.getSpawnedItems(itemType).values());
        }
        // ADD ARMORS
        for (ItemType itemType : ItemType.Category.ARMOR.getTypes()) {
        	interesting.addAll(items.getSpawnedItems(itemType).values());
        }
        // ADD QUADS
        interesting.addAll(items.getSpawnedItems(UT2004ItemType.U_DAMAGE_PACK).values());
        // ADD HEALTHS
        if (info.getHealth() < 100) {
        	interesting.addAll(items.getSpawnedItems(UT2004ItemType.HEALTH_PACK).values());
        }
        
        Item item = MyCollections.getRandom(tabooItems.filter(interesting));
        if (item == null) {
        	log.warning("NO ITEM TO RUN FOR!");
        	if (navigation.isNavigating()) return;
        	bot.getBotName().setInfo("RANDOM NAV");
        	navigation.navigate(navPoints.getRandomNavPoint());
        } else {
        	this.item = item;
        	log.info("RUNNING FOR: " + item.getType().getName());
        	bot.getBotName().setInfo("ITEM: " + item.getType().getName() + "");
        	navigation.navigate(item);        	
        }        
    }

    ////////////////
    // BOT KILLED //
    ////////////////
    protected int oldDeaths = 0;
    
    public boolean botKilled(){
        
        if(!(this.oldDeaths ==  this.getInfo().getDeaths())){
            this.oldDeaths =  this.getInfo().getDeaths();            
            return true;
        }
        else{
//            log.info("not dead");
//            log.info("old deaths :" + this.oldDeaths);
//            log.info("deaths :" + this.getInfo().getDeaths());
            
            return false;
        }
    }
    //////////////////
    // ENEMY KILLED //
    //////////////////
    protected int oldFrags = 0;
    
    public boolean botEnemyKilled(){
        if(!(this.oldFrags == this.frags)){
            this.oldFrags = this.frags;
            return true;
        }
        else{
            return false;
        }
    }
    
    ////////////////
    // Get & Set  //
    ////////////////

    public Player getEnemy(){
        return this.enemy;
    }
    
    public void setEnemy(Player player){
        this.enemy = player;
    }
    
    public boolean getRunningToPlayer(){
        return this.runningToPlayer;
    }
    
    public void setRunningToPlayer(boolean running){
        this.runningToPlayer = running;
    }
    
    public Item getItem(){
        return this.item;
    }
    
    public void setItem(Item i){
        this.item = i;
    }
    
    ///////////////////////////////////
    public static void main(String args[]) throws PogamutException {
        // starts 3 Hunters at once
        // note that this is the most easy way to get a bunch of (the same) bots running at the same time        
    	new UT2004BotRunner(HunterBot.class, "Hunter").setMain(true).setLogLevel(Level.INFO).startAgents(1);
    }
}
