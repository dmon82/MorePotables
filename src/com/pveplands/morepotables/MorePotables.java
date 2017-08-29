package com.pveplands.morepotables;

import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.items.ItemTypes;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javassist.CtClass;
import javassist.CtMethod;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

/**
 * Enables planting more items in pottery planters. The model used is determined
 * by a seeded RNG using the template Id as seed, and picked from the available
 * models for planters.
 */
public class MorePotables implements WurmServerMod, Configurable, ItemTemplatesCreatedListener, ServerStartedListener, PreInitable {
    static final Logger logger = Logger.getLogger(MorePotables.class.getName() + " " + MorePotables.class.getPackage().getImplementationVersion());
    
    /**
     * Item template flags to set.
     */
    static final short[] potable = new short[] { ItemTypes.ITEM_TYPE_POTABLE };
    
    /**
     * All available pottery planter models as of WU 1.3.1.3.
     */
    static final String[] models = new String[] {
        "basil", "belladonna", "cumin", "fennelplant", "ginger", "lovage", "mint",
        "oregano", "paprika", "parsley", "rosemary", "sage", "thyme", "turmeric" };
    
    /**
     * Utility class holding the mod properties.
     */
    private static Options options;
    public static Options getOptions() {
        return options;
    }
    
    public MorePotables() {
    }

    /**
     * Checks whether or not an item should be made potable in a planter.
     * @param templateId Item template Id to check.
     * @return True if the item should be potable in a planter.
     */
    public static boolean isPlantable(int templateId) {
        return options.isPlantable(templateId);
    }
    
    @Override
    public void preInit() {
        // Don't apply changes to item models.
        if (!options.useRandomModels())
            return;
        
        try {
            /**
             * Effectively overrides the model name for the filled planter,
             * otherwise the models will be question mark bags instead,
             * because "model.planter.pickaxe.*" does not exist for example.
             */
            CtClass item = HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item");
            CtMethod getModelName = item.getMethod("getModelName", "()Ljava/lang/String;");
            
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("if (getTemplateId() == 1162) {");
            
            for (int templateId : options.plantables.keySet()) {
                String modelName = options.getModel(templateId);
                
                sb.append("    if (getRealTemplateId() == ").append(templateId).append(") return \"model.planter.");
                sb.append(modelName).append("\";");
                
                logger.info(String.format("Using model '%s' for template Id %d.", modelName, templateId));
            }
            
            sb.append("}");
            sb.append("}");
            
            getModelName.insertBefore(sb.toString());
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Error during pre-initialisation.", e);
        }
    }

    @Override
    public void configure(Properties p) {
        options = new Options(p);
    }

    @Override
    public void onItemTemplatesCreated() {
        for (int templateId : options.getPlantables().keySet()) {
            ItemTemplate template = ItemTemplateFactory.getInstance().getTemplateOrNull(templateId);
            
            if (template == null) {
                logger.warning(String.format("Unknown item template ID: %d", templateId));
                continue;
            }
            
            if (template.isPotable()) {
                options.plantables.put(templateId, true);
                logger.warning(String.format("%s [id: %d] was already potable, and should be removed from the config file.", template.getName(), templateId));
            }
            else {
                template.assignTypes(potable);
                logger.info(String.format("Made %s [id: %d] plantable in a pot.", template.getName(), templateId));
            }
        }
    }

    @Override
    public void onServerStarted() {
        ModActions.registerAction(new ReloadConfigAction());
        ModActions.registerAction(new PlanterBehaviour());
    }
}
