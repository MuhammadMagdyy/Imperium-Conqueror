package views;

import engine.*;
import buildings.*;
import units.*;
import exceptions.*;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Auto-Win AI — plays the game autonomously until victory or defeat.
 *
 * Strategy (priority order each turn):
 *   1. Build Farm if missing and affordable
 *   2. Build Market if missing and affordable
 *   3. Build Barracks if no military building and affordable
 *   4. Recruit from every ready military building
 *   5. Form armies when garrison reaches 3+ units
 *   6. Auto-resolve battles at any army already at an enemy city
 *   7. March idle armies toward the nearest unconquered city
 *   8. End Turn
 *
 * Runs in a SwingWorker so the UI stays live between turns.
 */
public class AIPlayer extends SwingWorker<Void, String> {

    private final Game     game;
    private final Runnable onRefresh;   // called on EDT after each turn
    private final Runnable onFinished;  // called on EDT when done
    private final Consumer<Boolean> onToggleBtn; // enable/disable the button
    private volatile boolean running = true;
    private int turnDelay = 700; // ms between turns

    public AIPlayer(Game game, Runnable onRefresh,
                    Runnable onFinished, Consumer<Boolean> onToggleBtn) {
        this.game          = game;
        this.onRefresh     = onRefresh;
        this.onFinished    = onFinished;
        this.onToggleBtn   = onToggleBtn;
    }

    public void stop() { running = false; }

    // ── MAIN AI LOOP ─────────────────────────────────────────────
    @Override
    protected Void doInBackground() throws Exception {
        publish("🤖 AI Advisor taking control...");

        while (running && !game.isGameOver()) {
            Player p = game.getPlayer();

            // ── 1. BUILD ─────────────────────────────────────────
            for (City city : new ArrayList<>(p.getControlledCities())) {
                tryBuild(p, city, "farm");
                tryBuild(p, city, "market");
                if (city.getMilitaryBuildings().isEmpty())
                    tryBuild(p, city, "barracks");
                // Extra archery range when wealthy
                if (p.getTreasury() > 5000 && city.getMilitaryBuildings().size() < 2)
                    tryBuild(p, city, "archeryrange");
            }

            // ── 2. UPGRADE economic buildings ────────────────────
            for (City city : new ArrayList<>(p.getControlledCities())) {
                for (EconomicBuilding eb : city.getEconomicalBuildings()) {
                    if (!eb.isCoolDown() && eb.getLevel() < 3
                            && p.getTreasury() > eb.getUpgradeCost() + 2000) {
                        try { p.upgradeBuilding(eb); publish("⬆ Upgraded " + eb.getClass().getSimpleName() + " in " + city.getName()); }
                        catch (Exception ignored) {}
                    }
                }
            }

            // ── 3. RECRUIT ───────────────────────────────────────
            for (City city : new ArrayList<>(p.getControlledCities())) {
                for (MilitaryBuilding mb : city.getMilitaryBuildings()) {
                    if (mb.isCoolDown()) continue;
                    String type = mb instanceof Barracks ? "infantry"
                                : mb instanceof Stable   ? "cavalry" : "archer";
                    for (int r = mb.getCurrentRecruit(); r < mb.getMaxRecruit(); r++) {
                        try {
                            p.recruitUnit(type, city.getName());
                            publish("⚔ Recruited " + type + " in " + city.getName());
                        } catch (Exception ignored) { break; }
                    }
                }
            }

            // ── 4. FORM ARMIES ───────────────────────────────────
            for (City city : new ArrayList<>(p.getControlledCities())) {
                Army def = city.getDefendingArmy();
                if (def == null || def.getUnits().isEmpty()) continue;
                // Only form if garrison is big enough (keep ≥1 unit as guard)
                boolean armyAlreadyHere = p.getControlledArmies().stream()
                    .anyMatch(a -> a.getCurrentLocation().equals(city.getName())
                                   && a.getCurrentStatus() == Status.IDLE);
                if (!armyAlreadyHere && def.getUnits().size() >= 3) {
                    Unit strongest = def.getUnits().stream()
                        .max(Comparator.comparingInt(u -> u.getLevel() * 1000 + u.getCurrentSoldierCount()))
                        .orElse(null);
                    if (strongest != null) {
                        p.initiateArmy(city, strongest);
                        publish("🏃 Formed army at " + city.getName());
                    }
                }
            }

            // ── 5. BATTLE & MARCH ────────────────────────────────
            for (Army army : new ArrayList<>(p.getControlledArmies())) {
                if (army.getCurrentStatus() != Status.IDLE) continue;

                City enemyHere = enemyCityAt(army.getCurrentLocation(), p);
                if (enemyHere != null) {
                    // Battle!
                    try {
                        int mySize  = army.getUnits().size();
                        int itsSize = enemyHere.getDefendingArmy().getUnits().size();
                        publish("⚔ Auto-resolving battle at " + enemyHere.getName()
                            + " (" + mySize + " vs " + itsSize + ")");
                        game.autoResolve(army, enemyHere.getDefendingArmy());
                        if (p.getControlledCities().contains(enemyHere))
                            publish("🏆 Conquered " + enemyHere.getName() + "!");
                        else
                            publish("💀 Lost battle at " + enemyHere.getName());
                    } catch (FriendlyFireException ignored) {}
                } else {
                    // March to nearest unconquered city
                    City target = nearestEnemy(p);
                    if (target != null && !army.getCurrentLocation().equals("onRoad")) {
                        game.targetCity(army, target.getName());
                        publish("🏃 Army marching to " + target.getName()
                            + " (" + army.getDistancetoTarget() + " turns)");
                    }
                }
            }

            // ── 6. END TURN ──────────────────────────────────────
            game.endTurn();
            publish("─── Turn " + game.getCurrentTurnCount() + " ───");
            SwingUtilities.invokeLater(onRefresh);
            Thread.sleep(turnDelay);
        }

        if (game.isGameOver()) {
            boolean won = game.getPlayer().getControlledCities().size()
                       == game.getAvailableCities().size();
            publish(won ? "🏆 AI WON the game!" : "💀 AI could not win in time.");
        }
        return null;
    }

    @Override
    protected void process(List<String> lines) {
        // Messages arrive on EDT — caller must handle them via log
        lines.forEach(l -> System.out.println("[AI] " + l));
    }

    @Override
    protected void done() {
        SwingUtilities.invokeLater(() -> {
            if (onToggleBtn != null) onToggleBtn.accept(true);
            if (onFinished  != null) onFinished.run();
        });
    }

    // ── HELPERS ──────────────────────────────────────────────────
    private void tryBuild(Player p, City city, String type) {
        try { p.build(type, city.getName()); publish("🏗 Built " + type + " in " + city.getName()); }
        catch (NotEnoughGoldException ignored) {}
        catch (Exception ignored) {}
    }

    private City enemyCityAt(String loc, Player p) {
        for (City c : game.getAvailableCities())
            if (c.getName().equals(loc) && !p.getControlledCities().contains(c)) return c;
        return null;
    }

    private City nearestEnemy(Player p) {
        City best = null; int bestDist = Integer.MAX_VALUE;
        for (City owned : p.getControlledCities()) {
            for (City enemy : game.getAvailableCities()) {
                if (p.getControlledCities().contains(enemy)) continue;
                // Skip cities already targeted by another army
                boolean alreadyTargeted = p.getControlledArmies().stream()
                    .anyMatch(a -> a.getTarget().equals(enemy.getName()));
                if (alreadyTargeted) continue;
                int d = dist(owned.getName(), enemy.getName());
                if (d < bestDist) { bestDist = d; best = enemy; }
            }
        }
        // If all enemies are already targeted, just return the first one
        if (best == null) {
            for (City c : game.getAvailableCities())
                if (!p.getControlledCities().contains(c)) return c;
        }
        return best;
    }

    private int dist(String a, String b) {
        for (Distance d : game.getDistances())
            if ((d.getFrom().equals(a)&&d.getTo().equals(b)) ||
                (d.getFrom().equals(b)&&d.getTo().equals(a))) return d.getDistance();
        return Integer.MAX_VALUE;
    }
}
