package com.pveplands.morepotables;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;

/**
 * Utility class to hold the mods' properties.
 */
public class Options {
    private static final Random random = new Random();
    
    HashMap<Integer, Boolean> plantables;
    /**
     * HashMap holding all plantable template Ids, and a boolean value that
     * indicates whether or not this template was marked as Potable before.
     */
    public HashMap<Integer, Boolean> getPlantables() {
        return plantables;
    }
    
    boolean randomModels = true;
    /**
     * Whether or not to use a random model from existing planter models,
     * instead of trying to use "model.planter.itemname", which results in
     * question mark bags for missing models in the client's graphics.jar.
     */
    public boolean useRandomModels() {
        return randomModels;
    }
    
    String[] models = new String[] {
        "basil", "belladonna", "cumin", "fennelplant", "ginger", "lovage", "mint",
        "oregano", "paprika", "parsley", "rosemary", "sage", "thyme", "turmeric"
    };
    /**
     * Gets all the models that will be used for custom potables.
     */
    public String[] getModels() {
        return models;
    }
    
    public Options(Properties p) {
        init(p);
    }
    
    /**
     * Reads all the options from a Properties object. Can be called anytime
     * with new options. All old properties will be cleared or overwritten.
     */
    public final void init(Properties p) {
        if (plantables == null) plantables = new HashMap<>();
        else plantables.clear();
        
        if (p == null) {
            MorePotables.logger.warning(String.format("Tried to initialise options with Properties NULL."));
            return;
        }
        
        try {
            randomModels = Boolean.valueOf(p.getProperty("useRandomModels", String.valueOf(randomModels)));

            String values = p.getProperty("potables", "");
            
            if (values.isEmpty()) {
                MorePotables.logger.info("Adding no new potables.");
            }
            else {
                String[] ids = values.replace(" ", "").split(",");

                for (String id : ids) {
                    try {
                        Integer value = Integer.valueOf(id);
                        plantables.put(value, false);
                        MorePotables.logger.info(String.format("Added ID %d to potables list.", value));
                    }
                    catch (Exception e) {
                        MorePotables.logger.log(Level.SEVERE, "Couldn't parse or add ID to potables list.", e);
                    }
                }
            }
            
            values = p.getProperty("models", "");
            if (values.isEmpty()) {
                MorePotables.logger.info("No models specified, using default.");
            }
            else {
                models = values.replaceAll(" ", "").split(",");
                MorePotables.logger.info(String.format("Picking from models: %s", Arrays.toString(models)));
            }
        }
        catch (Exception e) {
            MorePotables.logger.log(Level.SEVERE, "Could not read properties file.", e);
        }
    }
    
    /**
     * @param templateId Item template Id.
     * @return True if this item template should be made potable.
     */
    public boolean isPlantable(int templateId) {
        return plantables.containsKey(templateId);
    }
    
    /**
     * @param templateId Item template Id.
     * @return True if this item template was already potable by default.
     */
    public boolean wasPotable(int templateId) {
        return plantables.getOrDefault(templateId, false);
    }
    
    /**
     * Gets the model name to use.
     * @param templateId Item template Id to get the model for.
     * @return Model name to use.
     */
    public String getModel(int templateId) {
        random.setSeed(templateId);
        
        return models[random.nextInt(models.length)];
    }
}
