package net.sacredlabyrinth.Phaed.PreciousStones;

import net.sacredlabyrinth.Phaed.PreciousStones.vectors.Field;
import net.sacredlabyrinth.Phaed.PreciousStones.vectors.TranslocationBlock;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author phaed
 */
public class TranslocationApplier implements Runnable
{
    private PreciousStones plugin;
    private Queue<TranslocationBlock> translocationQueue;
    private Queue<TranslocationBlock> dependentQueue = new LinkedList<TranslocationBlock>();
    private final int timerID;
    private final World world;
    private final Field field;

    /**
     * @param griefQueue
     * @param world
     */
    public TranslocationApplier(Field field, Queue<TranslocationBlock> translocationQueue, World world)
    {
        this.field = field;
        this.translocationQueue = translocationQueue;
        this.world = world;
        this.plugin = PreciousStones.getInstance();

        timerID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 5, 5);
    }

    public void run()
    {
        int i = 0;

        while (i < 100 && !translocationQueue.isEmpty())
        {
            TranslocationBlock tb = translocationQueue.poll();

            if (tb != null)
            {
                if (plugin.getSettingsManager().isDependentBlock(tb.getTypeId()))
                {
                    dependentQueue.add(tb);
                    continue;
                }

                boolean applied = PreciousStones.getInstance().getTranslocationManager().applyTranslocationBlock(tb, world);

                // if the block could not be applied, due to another block being in the way
                // then don't apply it nad set it on the database as not-applied

                if (!applied)
                {
                    PreciousStones.debug("not-applied:" + tb);
                    plugin.getStorageManager().updateTranslocationBlockApplied(field, tb, false);
                }
            }
            i++;
        }

        if (translocationQueue.isEmpty())
        {
            while (i < 200 && !dependentQueue.isEmpty())
            {
                TranslocationBlock tb = dependentQueue.poll();

                boolean applied = PreciousStones.getInstance().getTranslocationManager().applyTranslocationBlock(tb, world);

                // if the block could not be applied, due to another block being in the way
                // then don't apply it nad set it on the database as not-applied

                if (!applied)
                {
                    plugin.getStorageManager().updateTranslocationBlockApplied(field, tb, false);
                }

                i++;
            }

            if (!dependentQueue.iterator().hasNext())
            {
                Bukkit.getServer().getScheduler().cancelTask(timerID);
                plugin.getStorageManager().updateTranslocationApplyMode(field, true);
                field.setTranslocating(false);
            }
        }
    }
}