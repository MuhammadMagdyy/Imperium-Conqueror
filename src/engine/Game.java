package engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import buildings.EconomicBuilding;
import buildings.Farm;
import buildings.Market;
import buildings.MilitaryBuilding;
import exceptions.FriendlyFireException;
import units.Archer;
import units.Army;
import units.Cavalry;
import units.Infantry;
import units.Status;
import units.Unit;

public class Game {
    private Player player;
    private ArrayList<City> availableCities;
    private ArrayList<Distance> distances;
    private final int maxTurnCount = 30;
    private int currentTurnCount;

    public Game(String playerName, String playerCity) throws IOException {
        player = new Player(playerName);
        player.setTreasury(5000);
        availableCities = new ArrayList<>();
        distances = new ArrayList<>();
        currentTurnCount = 1;
        loadCitiesAndDistances();
        for (City c : availableCities) {
            if (c.getName().equals(playerCity)) {
                player.getControlledCities().add(c);
            }
        }
        if (playerCity.toLowerCase().equals("cairo")) {
            loadArmy("Rome",   "rome_army.csv");
            loadArmy("Sparta", "sparta_army.csv");
        } else if (playerCity.toLowerCase().equals("rome")) {
            loadArmy("Cairo",  "cairo_army.csv");
            loadArmy("Sparta", "sparta_army.csv");
        } else {
            loadArmy("Rome",  "rome_army.csv");
            loadArmy("Cairo", "cairo_army.csv");
        }
    }

    // ---------------------------------------------------------------
    //  Robust CSV loader: classpath → JAR sibling → working directory
    // ---------------------------------------------------------------
    private BufferedReader openResource(String name) throws IOException {
        // 1. Inside the JAR / classpath (works when packaged)
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        if (is != null) return new BufferedReader(new InputStreamReader(is));

        // 2. Next to the running JAR file
        try {
            File jarFile = new File(
                    getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            File sibling = new File(jarFile.getParentFile(), name);
            if (sibling.exists()) return new BufferedReader(new FileReader(sibling));
        } catch (URISyntaxException ignored) {}

        // 3. Current working directory (IDE run from project root)
        File cwd = new File(name);
        if (cwd.exists()) return new BufferedReader(new FileReader(cwd));

        // 4. "csv files/" subfolder (original project layout)
        File csvFolder = new File("csv_files/" + name);
        if (csvFolder.exists()) return new BufferedReader(new FileReader(csvFolder));

        throw new IOException(
                "Cannot find '" + name + "'.\n" +
                        "Make sure the CSV files are in the same folder as ConquerorGame.jar, " +
                        "or are bundled inside it.");
    }

    private void loadCitiesAndDistances() throws IOException {
        BufferedReader br = openResource("distances.csv");
        String currentLine = br.readLine();
        ArrayList<String> names = new ArrayList<>();
        while (currentLine != null) {
            String[] content = currentLine.split(",");
            if (!names.contains(content[0])) {
                availableCities.add(new City(content[0]));
                names.add(content[0]);
            }
            if (!names.contains(content[1])) {
                availableCities.add(new City(content[1]));
                names.add(content[1]);
            }
            distances.add(new Distance(content[0], content[1],
                    Integer.parseInt(content[2].trim())));
            currentLine = br.readLine();
        }
        br.close();
    }

    public void loadArmy(String cityName, String path) throws IOException {
        BufferedReader br = openResource(path);
        String currentLine = br.readLine();
        Army resultArmy = new Army(cityName);
        while (currentLine != null) {
            String[] content = currentLine.split(",");
            String unitType = content[0].trim().toLowerCase();
            int unitLevel = Integer.parseInt(content[1].trim());
            Unit u = null;
            if (unitType.equals("archer")) {
                if (unitLevel == 1)      u = new Archer(1, 60, 0.4, 0.5, 0.6);
                else if (unitLevel == 2) u = new Archer(2, 60, 0.4, 0.5, 0.6);
                else                     u = new Archer(3, 70, 0.5, 0.6, 0.7);
            } else if (unitType.equals("infantry")) {
                if (unitLevel == 1)      u = new Infantry(1, 50, 0.5, 0.6, 0.7);
                else if (unitLevel == 2) u = new Infantry(2, 50, 0.5, 0.6, 0.7);
                else                     u = new Infantry(3, 60, 0.6, 0.7, 0.8);
            } else if (unitType.equals("cavalry")) {
                if (unitLevel == 1)      u = new Cavalry(1, 40, 0.6, 0.7, 0.75);
                else if (unitLevel == 2) u = new Cavalry(2, 40, 0.6, 0.7, 0.75);
                else                     u = new Cavalry(3, 60, 0.7, 0.8, 0.9);
            }
            if (u != null) {
                resultArmy.getUnits().add(u);
                u.setParentArmy(resultArmy);
            }
            currentLine = br.readLine();
        }
        for (City c : availableCities) {
            if (c.getName().toLowerCase().equals(cityName.toLowerCase()))
                c.setDefendingArmy(resultArmy);
        }
        br.close();
    }

    public void targetCity(Army army, String targetName) {
        String from = army.getCurrentLocation();
        if (army.getCurrentLocation().equals("onRoad"))
            from = army.getTarget();
        for (Distance d : distances) {
            if ((d.getFrom().equals(from) || d.getFrom().equals(targetName))
                    && (d.getTo().equals(from) || d.getTo().equals(targetName))) {
                army.setTarget(targetName);
                int distance = d.getDistance();
                if (army.getCurrentLocation().equals("onRoad"))
                    distance += army.getDistancetoTarget();
                army.setDistancetoTarget(distance);
            }
        }
    }

    public void endTurn() {
        currentTurnCount++;
        double totalUpkeep = 0;
        for (City c : player.getControlledCities()) {
            for (MilitaryBuilding b : c.getMilitaryBuildings()) {
                b.setCoolDown(false);
                b.setCurrentRecruit(0);
            }
            for (EconomicBuilding b : c.getEconomicalBuildings()) {
                b.setCoolDown(false);
                if (b instanceof Market)
                    player.setTreasury(player.getTreasury() + b.harvest());
                else if (b instanceof Farm)
                    player.setFood(player.getFood() + b.harvest());
            }
            totalUpkeep += c.getDefendingArmy().foodNeeded();
        }
        for (Army a : player.getControlledArmies()) {
            if (!a.getTarget().equals("") && a.getCurrentStatus() == Status.IDLE) {
                a.setCurrentStatus(Status.MARCHING);
                a.setCurrentLocation("onRoad");
            }
            if (a.getDistancetoTarget() > 0 && !a.getTarget().equals(""))
                a.setDistancetoTarget(a.getDistancetoTarget() - 1);
            if (a.getDistancetoTarget() == 0) {
                a.setCurrentLocation(a.getTarget());
                a.setTarget("");
                a.setCurrentStatus(Status.IDLE);
            }
            totalUpkeep += a.foodNeeded();
        }
        if (totalUpkeep <= player.getFood()) {
            player.setFood(player.getFood() - totalUpkeep);
        } else {
            player.setFood(0);
            for (Army a : player.getControlledArmies()) {
                for (Unit u : a.getUnits()) {
                    u.setCurrentSoldierCount(
                            u.getCurrentSoldierCount()
                                    - (int)(u.getCurrentSoldierCount() * 0.1));
                }
            }
        }
        for (City c : availableCities) {
            if (c.isUnderSiege()) {
                if (c.getTurnsUnderSiege() < 3) {
                    c.setTurnsUnderSiege(c.getTurnsUnderSiege() + 1);
                } else {
                    c.setUnderSiege(false);
                    return;
                }
                for (Unit u : c.getDefendingArmy().getUnits()) {
                    u.setCurrentSoldierCount(
                            u.getCurrentSoldierCount()
                                    - (int)(u.getCurrentSoldierCount() * 0.1));
                }
            }
        }
    }

    public void autoResolve(Army attacker, Army defender) throws FriendlyFireException {
        int turn = 1;
        while (attacker.getUnits().size() != 0 && defender.getUnits().size() != 0) {
            Unit unit1 = attacker.getUnits().get(
                    (int)(Math.random() * attacker.getUnits().size()));
            Unit unit2 = defender.getUnits().get(
                    (int)(Math.random() * defender.getUnits().size()));
            if (turn == 1) unit1.attack(unit2);
            else           unit2.attack(unit1);
            turn = turn == 1 ? 0 : 1;
        }
        if (attacker.getUnits().size() != 0)
            occupy(attacker, defender.getCurrentLocation());
    }

    public void occupy(Army a, String cityName) {
        for (City c : availableCities) {
            if (c.getName().equals(cityName)) {
                player.getControlledCities().add(c);
                player.getControlledArmies().remove(a);
                c.setDefendingArmy(a);
                c.setUnderSiege(false);
                c.setTurnsUnderSiege(-1);
                a.setCurrentStatus(Status.IDLE);
            }
        }
    }

    public boolean isGameOver() {
        return player.getControlledCities().size() == availableCities.size()
                || currentTurnCount > maxTurnCount;
    }

    public ArrayList<City>     getAvailableCities()  { return availableCities; }
    public ArrayList<Distance> getDistances()         { return distances; }
    public int  getMaxTurnCount()                     { return maxTurnCount; }
    public Player getPlayer()                         { return player; }
    public void setPlayer(Player player)              { this.player = player; }
    public int  getCurrentTurnCount()                 { return currentTurnCount; }
    public void setCurrentTurnCount(int c)            { this.currentTurnCount = c; }
}
