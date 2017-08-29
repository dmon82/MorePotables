package com.pveplands.morepotables;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class ReloadConfigAction implements ModAction, BehaviourProvider, ActionPerformer {
    private static final int[] emptyIntArray = new int[0];
    
    private short actionId;
    @Override
    public short getActionId() {
        return actionId;
    }
    
    private ActionEntry actionEntry;
    public ActionEntry getActionEntry() {
        return actionEntry;
    }
    
    public ReloadConfigAction() {
        actionId = (short)ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, "Reload MorePotables config", "reloading", emptyIntArray);
        ModActions.registerAction(actionEntry);
    }
    
    public List<ActionEntry> getMyBehaviours(Creature performer, Item activated) {
        if (performer.getPower() <= 2 || activated == null || activated.getTemplateId() != 176)
            return null;

        ArrayList<ActionEntry> list = new ArrayList<>();
        list.add(new ActionEntry((short)-1, "Reload configuration", "reloading"));
        list.add(actionEntry);
        
        return list;
    }
    
    public boolean performMyAction(Creature performer, Item activated) {
        if (performer.getPower() <= 2 || activated == null || activated.getTemplateId() != 176)
            return true;
        
        String modName = MorePotables.class.getName() + " " + MorePotables.class.getPackage().getImplementationVersion();
        Path path = Paths.get("mods/MorePotables.properties");
        Logger logger = MorePotables.logger;
        Options options = MorePotables.getOptions();
        
        performer.getCommunicator().sendAlertServerMessage(String.format("Trying to reload config for %s...", modName));
        
        if (!Files.exists(path)) {
            performer.getCommunicator().sendAlertServerMessage("The config file seems to be missing.");
            return true;
        }
        
        InputStream stream = null;
        
        try {
            performer.getCommunicator().sendAlertServerMessage("Opening the config file for.");
            stream = Files.newInputStream(path);
            Properties properties = new Properties();
            
            performer.getCommunicator().sendAlertServerMessage("Reading from the config file.");
            properties.load(stream);
            
            logger.info("Reloading configuration.");
            performer.getCommunicator().sendAlertServerMessage("Loading all options.");
            options.init(properties);
            
            logger.info("Configuration reloaded.");
            performer.getCommunicator().sendAlertServerMessage("The config file has been reloaded.");
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Error while reloading properties file.", e);
            performer.getCommunicator().sendAlertServerMessage("Error reloading the config file, check the server log.");
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Properties file not closed, possible file lock.", e);
                performer.getCommunicator().sendAlertServerMessage("Error closing the config file, possible file lock remains.");
            }
        }
        
        return true;
    }
    
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activated, int tilex, int tiley, boolean onSurface, int tile) {
        return getMyBehaviours(performer, activated);
    }
    
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activated, Creature target) {
        return getMyBehaviours(performer, activated);
    }
    
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activated, Item target) {
        return getMyBehaviours(performer, activated);
    }
    
    @Override
    public boolean action(Action action, Creature performer, Item activated, Item target, short num, float counter) {
        return performMyAction(performer, activated);
    }
    
    @Override
    public boolean action(Action action, Creature performer, Item activated, Creature target, short num, float counter) {
        return performMyAction(performer, activated);
    }
    
    @Override
    public boolean action(Action action, Creature performer, Item activated, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short num, float counter) {
        return performMyAction(performer, activated);
    }
}
