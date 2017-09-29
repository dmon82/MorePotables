package com.pveplands.morepotables;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Methods;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class PlanterBehaviour implements ModAction, BehaviourProvider, ActionPerformer {
    private short actionId;
    public short getActionId() {
        return actionId;
    }
    
    private ActionEntry actionEntry;
    
    public PlanterBehaviour() {
        actionId = (short)ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, "Plant in planter", "planting", new int[0]);
        ModActions.registerAction(actionEntry);
    }
    
    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item activated, Item target) {
        if (target.getTemplateId() != ItemList.planterPottery)
            return null;
        
        if (!MorePotables.isPlantable(activated.getTemplateId()))
            return null;
        
        return Arrays.asList(new ActionEntry(actionId, "Plant " + activated.getName(), "planting"));
    }
    
    @Override
    public boolean action(Action act, Creature performer, Item activated, Item target, short action, float counter) {
        if (action != actionId)
            return true;
        
        if (!MorePotables.isPlantable(activated.getTemplateId()))
            return true;
        
        return plantHerb(act, performer, activated, target, counter);
    }
    
    /**
     * Copied from com.wurmonline.server.behaviours.PlanterBehaviour.
     */
    private static boolean plantHerb(Action act, Creature performer, Item herbSpice, Item pot, float counter) {
        if (!Methods.isActionAllowed(performer, act.getNumber())) {
            return true;
        }
        else if (!performer.isWithinDistanceTo(pot, 6f)) {
            performer.getCommunicator().sendNormalServerMessage("You're too far away.");
            return true;
        }
        
        int time = 0;
        if (counter == 1.0f) {
            String type = herbSpice.isSpice() ? "spice" : "herb";
            Skill gardening = performer.getSkills().getSkillOrLearn(10045);
            time = Actions.getStandardActionTime(performer, gardening, herbSpice, 0.0);
            act.setTimeLeft(time);
            performer.getCommunicator().sendNormalServerMessage("You start planting the " + herbSpice.getName() + ".");
            Server.getInstance().broadCastAction(performer.getName() + " starts to plant some " + type + ".", performer, 5);
            performer.sendActionControl(Actions.actionEntrys[186].getVerbString(), true, time);
            return false;
        }
        time = act.getTimeLeft();
        if (counter * 10.0f > (float)time) {
            float ql = herbSpice.getQualityLevel() + pot.getQualityLevel();
            ql /= 2.0f;
            float dmg = herbSpice.getDamage() + pot.getDamage();
            dmg /= 2.0f;
            Skill gardening = performer.getSkills().getSkillOrLearn(10045);
            try {
                int toCreate = 1162;
                ItemTemplate template = ItemTemplateFactory.getInstance().getTemplate(1162);
                double power = gardening.skillCheck(template.getDifficulty() + dmg, ql, false, counter);
                if (power > 0.0) {
                    try {
                        Item newPot = ItemFactory.createItem(1162, pot.getQualityLevel(), pot.getRarity(), performer.getName());
                        newPot.setRealTemplate(herbSpice.getTemplate().getGrows());
                        newPot.setLastOwnerId(pot.getLastOwnerId());
                        newPot.setDescription(pot.getDescription());
                        newPot.setDamage(pot.getDamage());
                        Item parent = pot.getParentOrNull();
                        if (parent != null && parent.getItemsAsArray().length > 30) {
                            performer.getCommunicator().sendNormalServerMessage("The pot will not fix back into the rack, so you place it on the ground.", (byte)2);
                            newPot.setPosXY(pot.getPosX(), pot.getPosY());
                            VolaTile tile = Zones.getTileOrNull(pot.getTileX(), pot.getTileY(), pot.isOnSurface());
                            if (tile != null) {
                                tile.addItem(newPot, false, false);
                            }
                        } else if (parent == null) {
                            newPot.setPosXYZRotation(pot.getPosX(), pot.getPosY(), pot.getPosZ(), pot.getRotation());
                            newPot.setIsPlanted(pot.isPlanted());
                            VolaTile tile = Zones.getTileOrNull(pot.getTileX(), pot.getTileY(), pot.isOnSurface());
                            if (tile != null) {
                                tile.addItem(newPot, false, false);
                            }
                        } else {
                            parent.insertItem(newPot, true);
                        }
                        Items.destroyItem(pot.getWurmId());
                        performer.getCommunicator().sendNormalServerMessage("You finished planting the " + herbSpice.getName() + " in the pot.");
                    }
                    catch (NoSuchTemplateException nst) {
                        MorePotables.logger.log(Level.WARNING, nst.getMessage(), (Throwable)((Object)nst));
                    }
                    catch (FailedException fe) {
                        MorePotables.logger.log(Level.WARNING, fe.getMessage(), (Throwable)((Object)fe));
                    }
                } else {
                    performer.getCommunicator().sendNormalServerMessage("Sadly, the fragile " + herbSpice.getName() + " do not survive despite your best efforts.", (byte)3);
                }
                Items.destroyItem(herbSpice.getWurmId());
            }
            catch (NoSuchTemplateException nst) {
                MorePotables.logger.log(Level.WARNING, nst.getMessage(), (Throwable)((Object)nst));
            }
            return true;
        }
        return false;
    }
}
