package xyz.tprj.screencapture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public final class ScreenCapture extends JavaPlugin implements Listener {

    private boolean allStop = true;

    private Location loc;

    private final java.util.List<DrawTask> drawTaskList = new ArrayList<>();
    private CaptureTask captureTask;
    private QueueTask queueTask;
    private DrawingTask drawingTask;

    @Override
    public void onEnable() {
        // Plugin startup logic
        loc = Bukkit.getWorld("world").getSpawnLocation();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getMessage().startsWith("loc")) {
            loc = event.getPlayer().getLocation();
        } else if (event.getMessage().startsWith("start") && allStop) {
            allStop = false;
            captureTask = new CaptureTask();
            captureTask.runTaskTimer(this, 50, 2);
            queueTask = new QueueTask();
            queueTask.runTaskTimer(this, 50, 1);
            drawingTask = new DrawingTask();
            drawingTask.runTaskTimer(this, 50, 1);
            for (int i = 0; i < 8; i++) {
                DrawTask dt = new DrawTask();
                dt.runTaskTimer(this, 0, 1);
                drawTaskList.add(dt);
            }

        } else if (event.getMessage().startsWith("stop")) {
            allStop = true;
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public class CaptureTask extends BukkitRunnable {

        private Dimension screenSize;
        private Rectangle screenRectangle;
        private Robot robot;

        public CaptureTask() {
            try {
                screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                screenRectangle = new Rectangle(screenSize);
                System.out.println(screenSize);
                robot = new Robot();
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            capturedImageQueue.add(robot.createScreenCapture(screenRectangle));
            if (allStop) cancel();
            System.out.println(String.format("%d queue images", capturedImageQueue.size()));
            System.out.println(String.format("%d buffered queue", screenQueue.size()));
            System.out.println(String.format("%d draw pixel queue", drawQueue.size()));
        }
    }

    private final Queue<BufferedImage> capturedImageQueue = new ArrayDeque<>();

    public class QueueTask extends BukkitRunnable {

        @Override
        public void run() {
            if (capturedImageQueue.isEmpty()) return;
            BufferedImage bi = capturedImageQueue.poll();
            int height = bi.getHeight();
            int width = bi.getWidth();
            ArrayList<ArrayList<java.awt.Color>> arrayArraysDusts = new ArrayList<>();
            for (int i = 0; i < height; i += 22) {
                ArrayList<java.awt.Color> dusts = new ArrayList<>();
                for (int j = 0; j < width; j += 22) {
                    java.awt.Color c = new java.awt.Color(bi.getRGB(j, i));
                    dusts.add(c);
                }
                arrayArraysDusts.add(dusts);
            }
            screenQueue.add(arrayArraysDusts);
            if (capturedImageQueue.size() >= 50) {
                allStop = true;
                cancel();
            }
        }
    }

    private Queue<ArrayList<ArrayList<java.awt.Color>>> screenQueue = new ArrayDeque<>();

    public class DrawingTask extends BukkitRunnable {

        @Override
        public void run() {
            if (screenQueue.isEmpty()) return;
            ArrayList<ArrayList<java.awt.Color>> current = screenQueue.poll();
            java.util.List<DrawObject> tempPairs = new ArrayList<>();
            for (int i = 0, s = current.size(); i < s; i++) {
                ArrayList<java.awt.Color> currentSub = current.get(i);
                for (int j = 0, s1 = currentSub.size(); j < s1; j++) {
                    DrawObject drawObject = new DrawObject(s, j, i, currentSub.get(j));
                    drawQueue.add(drawObject);
                    tempPairs.add(drawObject);
                }
            }
            drawQueue.addAll(tempPairs);
            if (allStop) cancel();
        }
    }

    private final Queue<DrawObject> drawQueue = new ArrayDeque<>();

    public class DrawTask extends BukkitRunnable {

        public synchronized void show() {
            for (int i = 0; i < 700; i++) {
                DrawObject q = drawQueue.poll();
                if (q != null) {
                    loc.getWorld().spawnParticle(Particle.REDSTONE, q.getLocation(), 1, q.getDust());
                }
            }
        }

        @Override
        public void run() {
                show();
                if (drawQueue.isEmpty()) {
                    System.out.println("Queue empty");
                } else {
                    System.out.println(drawQueue.size());
                }
            if (allStop) {
                //cancel();
            }
        }
    }

    public class DrawObject {
        private int height;
        private int x;
        private int y;
        private java.awt.Color color;

        public DrawObject(int height, int x, int y, java.awt.Color color) {
            this.height = height;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public Particle.DustOptions getDust() {
            return new Particle.DustOptions(org.bukkit.Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()), 0.1f);
        }

        public Location getLocation() {
            return loc.clone().add(x * 0.02, height * 0.02 - (y * 0.02), 0);
        }
    }
}
