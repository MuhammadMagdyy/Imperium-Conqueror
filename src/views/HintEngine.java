package views;

import engine.*;
import buildings.*;
import units.*;
import java.util.*;

/**
 * Analyses the current game state and returns the single most important
 * strategic hint the player should act on right now to win.
 *
 * Hints are returned as a Hint record containing:
 *   - title   : short headline
 *   - body    : detailed explanation of WHY and HOW
 *   - urgency : LOW / MEDIUM / HIGH / CRITICAL
 */
public class HintEngine {

    public enum Urgency { LOW, MEDIUM, HIGH, CRITICAL }

    public record Hint(String title, String body, Urgency urgency) {}

    // ── Entry point ──────────────────────────────────────────────
    public static Hint analyse(Game game) {
        Player p    = game.getPlayer();
        int    turn = game.getCurrentTurnCount();
        int    remaining = game.getMaxTurnCount() - turn;
        int    enemyCount = (int) game.getAvailableCities().stream()
                                .filter(c -> !p.getControlledCities().contains(c)).count();

        // ── 1. CRITICAL: food starvation imminent ────────────────
        double upkeep = estimateUpkeep(p);
        if (p.getFood() < upkeep * 2 && upkeep > 0) {
            boolean hasFarm = p.getControlledCities().stream()
                .anyMatch(c -> c.getEconomicalBuildings().stream().anyMatch(b -> b instanceof Farm));
            if (!hasFarm)
                return new Hint(
                    "⚠  STARVATION IMMINENT — Build a Farm NOW",
                    "Your food will run out in ~" + (upkeep>0?(int)(p.getFood()/upkeep):0) + " turn(s).\n" +
                    "Select your city → 🏗 Build → Farm (costs 1 000 💰).\n" +
                    "A Farm at Level 1 produces 500 🌾 per turn, scaling to 1 000 at Level 3.\n" +
                    "Without food your soldiers starve 10% each turn — armies become useless.",
                    Urgency.CRITICAL);
            return new Hint(
                "⚠  CRITICAL: Food is nearly gone",
                "You have ~" + (int)p.getFood() + " 🌾 but your armies eat ~" + (int)upkeep + " per turn.\n" +
                "• Stop marching non-essential armies (marching costs more food than idle).\n" +
                "• Upgrade your Farm to produce more per turn.\n" +
                "• Disband units by not replacing them — fewer soldiers = less upkeep.",
                Urgency.CRITICAL);
        }

        // ── 2. CRITICAL: turn limit very close ────────────────────
        if (remaining <= 4 && enemyCount > 0) {
            City nearest = nearestEnemy(p, game);
            String advice = nearest != null
                ? "March every available army to " + nearest.getName() + " immediately and AUTO-RESOLVE.\n" +
                  "At this stage, losing an army is acceptable — you must capture cities fast."
                : "Launch all armies at enemy cities and use Auto-Resolve — no time for sieges.";
            return new Hint(
                "🚨  ONLY " + remaining + " TURNS LEFT — Attack NOW",
                "You still need " + enemyCount + " more city/cities.\n" + advice,
                Urgency.CRITICAL);
        }

        // ── 3. HIGH: army has arrived at enemy city — take action ─
        for (Army a : p.getControlledArmies()) {
            City target = enemyCityAt(a.getCurrentLocation(), p, game);
            if (target != null && a.getCurrentStatus() == Status.IDLE) {
                int defSize = target.getDefendingArmy().getUnits().size();
                int attSize = a.getUnits().size();
                boolean siegeFavoured = defSize > attSize || target.isUnderSiege();
                return new Hint(
                    "⚔  Army at " + target.getName() + " — Ready to Strike!",
                    "Your army (" + attSize + " units) is standing at " + target.getName() +
                    " (" + defSize + " defenders).\n\n" +
                    (siegeFavoured
                        ? "👉  Recommended: LAY SIEGE\n" +
                          "The enemy outnumbers you. Siege drains 10% of their soldiers per turn.\n" +
                          "After 3 turns the city surrenders automatically — no battle risk."
                        : "👉  Recommended: AUTO-RESOLVE\n" +
                          "You outnumber the defenders. Auto-resolving now gives a strong win chance.\n" +
                          "Select your army → ⚔ Auto Resolve."),
                    Urgency.HIGH);
            }
        }

        // ── 4. HIGH: army besieging — how many turns left ─────────
        for (City c : game.getAvailableCities()) {
            if (c.isUnderSiege()) {
                int left = 3 - c.getTurnsUnderSiege();
                return new Hint(
                    "⚙  Siege of " + c.getName() + " — " + left + " turn(s) remaining",
                    c.getName() + " has been under siege for " + c.getTurnsUnderSiege() + "/3 turns.\n" +
                    "• Press END TURN to advance the siege.\n" +
                    "• The city will surrender automatically after " + left + " more turn(s).\n" +
                    "• Use this time to recruit more units or build in your other cities.\n" +
                    (left == 1 ? "👉  Next turn the city falls — prepare to occupy it!" : ""),
                    Urgency.HIGH);
            }
        }

        // ── 5. HIGH: no army marching and enemies exist ───────────
        boolean hasMarching = p.getControlledArmies().stream()
            .anyMatch(a -> a.getCurrentStatus() == Status.MARCHING || a.getCurrentStatus() == Status.BESIEGING);
        boolean hasIdleArmy = p.getControlledArmies().stream()
            .anyMatch(a -> a.getCurrentStatus() == Status.IDLE);

        if (enemyCount > 0 && !hasMarching) {
            City nearest = nearestEnemy(p, game);
            if (hasIdleArmy && nearest != null) {
                int dist = distanceTo(p, game, nearest);
                return new Hint(
                    "🏃  March Your Army to " + nearest.getName(),
                    "You have an idle army but no active campaign.\n" +
                    "👉  Select your army in the right panel → 🏃 March To… → " + nearest.getName() + ".\n" +
                    "Travel time: " + dist + " turn(s). Start now — every turn idle is a turn wasted.\n" +
                    (remaining - dist < 4 ? "⚠  Hurry! You'll arrive with only " + (remaining-dist) + " turns to spare." : ""),
                    Urgency.HIGH);
            }
            // No army at all — need to form one
            if (!hasIdleArmy) {
                for (City c : p.getControlledCities()) {
                    if (c.getDefendingArmy() != null && !c.getDefendingArmy().getUnits().isEmpty()) {
                        return new Hint(
                            "⚔  Form an Army in " + c.getName(),
                            "You have units garrisoned in " + c.getName() + " but no marching army.\n" +
                            "👉  Select " + c.getName() + " → ⚔ Form Army → choose your strongest unit.\n" +
                            "Then immediately set a destination: 🏃 March To… → " + (nearest!=null?nearest.getName():"nearest enemy city") + ".\n" +
                            "A city left unattacked is gold being left on the table.",
                            Urgency.HIGH);
                    }
                }
            }
        }

        // ── 6. MEDIUM: no Farm ────────────────────────────────────
        boolean hasFarm = p.getControlledCities().stream()
            .anyMatch(c -> c.getEconomicalBuildings().stream().anyMatch(b -> b instanceof Farm));
        if (!hasFarm && p.getTreasury() >= 1000) {
            City best = bestBuildCity(p);
            return new Hint(
                "🌾  Build a Farm — Food Security First",
                "You have no Farm. Without food income your armies will starve as they grow.\n" +
                "👉  Select " + (best!=null?best.getName():"your city") + " → 🏗 Build → Farm (1 000 💰).\n" +
                "After building, press END TURN once to clear the cool-down, then it harvests every turn.\n" +
                "Farm Level 1 → +500 🌾/turn. Upgrade to Level 3 for +1 000 🌾/turn.",
                Urgency.MEDIUM);
        }

        // ── 7. MEDIUM: no Market ─────────────────────────────────
        boolean hasMarket = p.getControlledCities().stream()
            .anyMatch(c -> c.getEconomicalBuildings().stream().anyMatch(b -> b instanceof Market));
        if (!hasMarket && p.getTreasury() >= 1500) {
            City best = bestBuildCity(p);
            return new Hint(
                "💰  Build a Market — Gold is Power",
                "You have no Market. Gold funds everything: buildings, units, upgrades.\n" +
                "👉  Select " + (best!=null?best.getName():"your city") + " → 🏗 Build → Market (1 500 💰).\n" +
                "Market Level 1 → +1 000 💰/turn. Level 3 → +2 000 💰/turn.\n" +
                "With a Market running you can outbuild your enemies every single turn.",
                Urgency.MEDIUM);
        }

        // ── 8. MEDIUM: no military building ──────────────────────
        boolean hasMilitary = p.getControlledCities().stream()
            .anyMatch(c -> !c.getMilitaryBuildings().isEmpty());
        if (!hasMilitary && p.getTreasury() >= 1500) {
            return new Hint(
                "🏹  Build a Military Building",
                "You have no way to recruit units yet.\n" +
                "👉  Options (cheapest first):\n" +
                "  • Archery Range  1 500 💰 → Archers (excellent vs Cavalry)\n" +
                "  • Barracks       2 000 💰 → Infantry (balanced, strong vs Archers)\n" +
                "  • Stable         2 500 💰 → Cavalry (devastating vs Archers)\n" +
                "Start with Archery Range for cost-efficiency, or Barracks for durability.",
                Urgency.MEDIUM);
        }

        // ── 9. MEDIUM: garrison has enough units to form army ─────
        if (!hasMarching && enemyCount > 0) {
            for (City c : p.getControlledCities()) {
                int garrisonSize = c.getDefendingArmy() != null
                    ? c.getDefendingArmy().getUnits().size() : 0;
                if (garrisonSize >= 3) {
                    City nearest = nearestEnemy(p, game);
                    return new Hint(
                        "⚔  " + c.getName() + " garrison is strong — time to attack",
                        c.getName() + " has " + garrisonSize + " garrison units. That's enough to form a war party.\n" +
                        "👉  Select " + c.getName() + " → ⚔ Form Army → pick your best unit.\n" +
                        "Then march immediately to " + (nearest!=null?nearest.getName():"the nearest enemy") + ".\n" +
                        "Leave 1–2 units in the garrison to defend the city while you're away.",
                        Urgency.MEDIUM);
                }
            }
        }

        // ── 10. MEDIUM: can recruit but hasn't ───────────────────
        for (City c : p.getControlledCities()) {
            for (MilitaryBuilding mb : c.getMilitaryBuildings()) {
                if (!mb.isCoolDown() && mb.getCurrentRecruit() < mb.getMaxRecruit()
                        && p.getTreasury() >= mb.getRecruitmentCost()) {
                    String type = mb instanceof Barracks ? "Infantry" :
                                  mb instanceof Stable   ? "Cavalry"  : "Archer";
                    return new Hint(
                        "⚔  Recruit a " + type + " in " + c.getName(),
                        "Your " + mb.getClass().getSimpleName() + " in " + c.getName() +
                        " is ready and you can afford it (" + mb.getRecruitmentCost() + " 💰).\n" +
                        "👉  Select " + c.getName() + " → ⚔ Recruit → choose " + type + ".\n" +
                        "You can recruit up to 3 units per building per turn (slots: " +
                        mb.getCurrentRecruit() + "/3 used this turn).\n" +
                        "More units = stronger attack and better siege survival.",
                        Urgency.MEDIUM);
                }
            }
        }

        // ── 11. LOW: upgrade a building ──────────────────────────
        for (City c : p.getControlledCities()) {
            for (EconomicBuilding eb : c.getEconomicalBuildings()) {
                if (!eb.isCoolDown() && eb.getLevel() < 3 && p.getTreasury() >= eb.getUpgradeCost()) {
                    return new Hint(
                        "⬆  Upgrade " + eb.getClass().getSimpleName() + " in " + c.getName(),
                        "Your " + eb.getClass().getSimpleName() + " is Level " + eb.getLevel() +
                        " and ready to upgrade (cost: " + eb.getUpgradeCost() + " 💰).\n" +
                        "👉  Select " + c.getName() + " → ⬆ Upgrade → pick the " + eb.getClass().getSimpleName() + ".\n" +
                        "After upgrade it will produce " +
                        projectedHarvest(eb) + " resources/turn instead of " + eb.harvest() + ".\n" +
                        "Economic upgrades pay for themselves in just a few turns.",
                        Urgency.LOW);
                }
            }
        }

        // ── 12. LOW: all good, just reinforce ─────────────────────
        return new Hint(
            "✅  Position is strong — keep expanding",
            "Your empire looks stable. Here's what to focus on:\n" +
            "• Keep at least 1 Farm + 1 Market per city for maximum income.\n" +
            "• Always have an army marching toward an enemy city.\n" +
            "• Upgrade buildings when you have surplus gold (1 500+ above your next planned spend).\n" +
            "• Aim to conquer " + enemyCount + " more city/cities before turn " + game.getMaxTurnCount() + ".",
            Urgency.LOW);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static double estimateUpkeep(Player p) {
        double sum = 0;
        for (City c : p.getControlledCities()) {
            Army def = c.getDefendingArmy();
            if (def != null) sum += def.foodNeeded();
        }
        for (Army a : p.getControlledArmies()) sum += a.foodNeeded();
        return sum;
    }

    private static City nearestEnemy(Player p, Game game) {
        City best = null; int bestDist = Integer.MAX_VALUE;
        for (City owned : p.getControlledCities()) {
            for (City enemy : game.getAvailableCities()) {
                if (p.getControlledCities().contains(enemy)) continue;
                int d = dist(game, owned.getName(), enemy.getName());
                if (d < bestDist) { bestDist = d; best = enemy; }
            }
        }
        return best;
    }

    private static int distanceTo(Player p, Game game, City target) {
        int best = Integer.MAX_VALUE;
        for (City owned : p.getControlledCities()) {
            int d = dist(game, owned.getName(), target.getName());
            if (d < best) best = d;
        }
        for (Army a : p.getControlledArmies()) {
            if (a.getTarget().equals(target.getName())) return a.getDistancetoTarget();
        }
        return best;
    }

    private static int dist(Game game, String a, String b) {
        for (Distance d : game.getDistances())
            if ((d.getFrom().equals(a)&&d.getTo().equals(b)) ||
                (d.getFrom().equals(b)&&d.getTo().equals(a))) return d.getDistance();
        return Integer.MAX_VALUE;
    }

    private static City enemyCityAt(String loc, Player p, Game game) {
        for (City c : game.getAvailableCities())
            if (c.getName().equals(loc) && !p.getControlledCities().contains(c)) return c;
        return null;
    }

    private static City bestBuildCity(Player p) {
        return p.getControlledCities().isEmpty() ? null : p.getControlledCities().get(0);
    }

    private static int projectedHarvest(EconomicBuilding eb) {
        int next = eb.getLevel() + 1;
        if (eb instanceof Farm)   return next==2?700:1000;
        if (eb instanceof Market) return next==2?1500:2000;
        return 0;
    }
}
