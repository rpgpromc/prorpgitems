package studio.magemonkey.divinity.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import studio.magemonkey.codex.util.EffectUT;
import studio.magemonkey.codex.util.LocUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.essences.EssencesManager.EssenceEffect;

public class ParticleUtils {

    private static Divinity plugin = Divinity.instance;

    public static void spiral(final Location loc, final String eff1, final String eff2) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (++i == 20) {
                    this.cancel();
                }
                int          n  = i;
                final double n2 = (0.5 + n * 0.15) % 3.0;
                for (int n3 = 0; n3 < n2 * 10.0; ++n3) {
                    final double n4 = 6.283185307179586 / (n2 * 10.0) * n3;
                    EffectUT.playEffect(LocUT.getPointOnCircle(loc, false, n4, n2, 1.0),
                            eff1,
                            0.1f,
                            0.1f,
                            0.1f,
                            0.0f,
                            2);
                    if (n < 15) {
                        EffectUT.playEffect(LocUT.getPointOnCircle(loc, false, n4, n2, 1.0),
                                eff2,
                                0.1f,
                                0.1f,
                                0.1f,
                                0.0f,
                                1);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static void wave(final LivingEntity e, final String eff1, final String eff2) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (++i == 20) {
                    this.cancel();
                }
                int          n  = i;
                final double n2 = (0.5 + n * 0.15) % 3.0;
                for (int n3 = 0; n3 < n2 * 10.0; ++n3) {
                    final double n4 = 6.283185307179586 / (n2 * 10.0) * n3;
                    EffectUT.playEffect(LocUT.getPointOnCircle(e.getLocation(), false, n4, n2, 1.0),
                            eff1,
                            0.1f,
                            0.1f,
                            0.1f,
                            0f,
                            2);
                    if (n < 15) {
                        EffectUT.playEffect(LocUT.getPointOnCircle(e.getLocation(), false, n4, n2, 1.0),
                                eff2,
                                0.1f,
                                0.1f,
                                0.1f,
                                0f,
                                1);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static void doParticle(final Entity e, final String eff1, final String eff2) {
        new BukkitRunnable() {
            int    i          = 1;
            int    k          = 0;
            World  localWorld = e.getWorld();
            double d3         = e.getLocation().getX();
            double d5         = e.getLocation().getY();
            double d7         = e.getLocation().getZ();

            @Override
            public void run() {
                if (k == 3) this.cancel();
                EffectUT.playEffect(new Location(localWorld, d3, d5 + 1.8D, d7), eff1, 0.0F, 0.0F, 0.0F, 0.0F, i);
                EffectUT.playEffect(new Location(localWorld, d3, d5 + 1.5D, d7), eff1, 0.0F, 0.0F, 0.0F, 0.0F, i);
                EffectUT.playEffect(new Location(localWorld, d3 + 0.6D, d5 + 1.8D, d7),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 + 0.6D, d5 + 1.5D, d7),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 + 0.6D, d5 + 1.8D, d7 + 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 + 0.6D, d5 + 1.5D, d7 + 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 - 0.6D, d5 + 1.8D, d7),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 - 0.6D, d5 + 1.5D, d7),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 - 0.6D, d5 + 1.8D, d7 + 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 - 0.6D, d5 + 1.5D, d7 + 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3, d5 + 1.8D, d7 + 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 - 0.6D, d5 + 1.5D, d7 + 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 + 0.6D, d5 + 1.8D, d7 - 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 + 0.6D, d5 + 1.5D, d7 - 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3, d5 + 1.8D, d7 - 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3, d5 + 1.5D, d7 - 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 - 0.6D, d5 + 1.8D, d7 - 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3 - 0.6D, d5 + 1.5D, d7 - 0.6D),
                        eff1,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        i);
                EffectUT.playEffect(new Location(localWorld, d3, d5 + 1.0D, d7), eff2, 0.0F, 0.0F, 0.0F, 0.0F, 2);
                k++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public static void helix(final EssenceEffect ee, final LivingEntity e1, final int lvl) {
        new BukkitRunnable() {
            double phi = 0;

            @Override
            public void run() {
                phi = phi + Math.PI / 8;
                double x, y, z;

                Location location1 = e1.getLocation();
                for (double t = 0; t <= lvl * Math.PI; t = t + Math.PI / 16) { // it was 2 <lvl>
                    for (double i = 0; i <= 1; i = i + 1) {
                        x = 0.4 * (2 * Math.PI - t) * 0.5 * Math.cos(t + phi + i * Math.PI);
                        y = 0.5 * t;
                        z = 0.4 * (2 * Math.PI - t) * 0.5 * Math.sin(t + phi + i * Math.PI);
                        location1.add(x, y, z);
                        ee.display(location1);
                        //EffectUT.playEffect(particle, location1, offX, offY, offZ, speed, amount);
                        location1.subtract(x, y, z);
                    }
                }

                if (phi > 2 * Math.PI) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 3L);
    }

    public static void aura(final EssenceEffect ee, final LivingEntity p, final int lvl) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= 20) {
                    cancel();
                }
                Location l = p.getLocation().add(0.0D, lvl, 0.0D);
                ee.display(l);
                //EffectUT.playEffect(eff, l, 0.5F, 0.5F, 0.5F, 0.1F, 25);
                i++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 5L);
    }

    public static void foot(final EssenceEffect ee, final LivingEntity p, final int lvl) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= 50) {
                    cancel();
                }
                Location l = p.getLocation();
                ee.display(l);
                //EffectUT.playEffect(eff, l, 0f, 0f, 0f, 0.1F, 10 * lvl);
                i++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 2L);
    }

    public static void repairEffect(final Location loc, final String eff) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (++i == 72) {
                    this.cancel();
                }
                int            n             = i;
                final double   n2            = 0.3141592653589793 * n;
                final double   n3            = n * 0.1 % 2.5;
                final double   n4            = 0.45;
                final Location pointOnCircle = LocUT.getPointOnCircle(LocUT.getCenter(loc), true, n2, n4, n3);
                final Location pointOnCircle2 =
                        LocUT.getPointOnCircle(LocUT.getCenter(loc), true, n2 - 3.141592653589793, n4, n3);
                EffectUT.playEffect(pointOnCircle, "FLAME", 0.0f, 0.0f, 0.0f, 0.0f, 1);
                EffectUT.playEffect(pointOnCircle2, "FLAME", 0.0f, 0.0f, 0.0f, 0.0f, 1);
                EffectUT.playEffect(LocUT.getCenter(loc).add(0, 0.5, 0), eff, 0.2f, 0.0f, 0.2f, 0.0f, 5);
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public static void drawParticleLine(Location from,
                                        Location to,
                                        String pe,
                                        float offX,
                                        float offY,
                                        float offZ,
                                        float speed,
                                        int amount) {
        Location origin = from;
        Vector   target = new Location(to.getWorld(), to.getX(), to.getY(), to.getZ()).toVector();
        origin.setDirection(target.subtract(origin.toVector()));
        Vector increase = origin.getDirection();

        for (int counter = 0; counter < from.distance(to); counter++) {
            Location loc = origin.add(increase);

            Material wall = loc.getBlock().getType();
            if (wall != Material.AIR && wall.isSolid() && wall.isBlock()) {
                break;
            }

            EffectUT.playEffect(loc, pe, offX, offY, offZ, speed, 5);
        }
    }
}
