package views;

import engine.*;
import buildings.*;
import units.*;
import exceptions.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;

public class GameFrame extends JFrame {

    // ─── Game State ───────────────────────────────────────────────────
    private final Game game;
    private final String playerName;

    // ─── Icons ────────────────────────────────────────────────────────
    private BufferedImage bgMap;
    private ImageIcon iconGold, iconFood, iconArmy, iconCastle, iconFarm, iconSwords;

    // ─── Top Bar ──────────────────────────────────────────────────────
    private JLabel lblGold, lblFood, lblTurn;

    // ─── Left: City List ──────────────────────────────────────────────
    private DefaultListModel<City> cityListModel;
    private JList<City> cityList;

    // ─── Center: City Detail ──────────────────────────────────────────
    private JPanel cityDetailPanel;
    private JLabel cityNameLabel;
    private JPanel buildingsPanel;
    private JPanel armyPanel;
    private JLabel cityStatusLabel;

    // ─── Right: Controlled Armies ─────────────────────────────────────
    private DefaultListModel<Army> armyListModel;
    private JList<Army> armyList;
    private JLabel armyInfoLabel;

    // ─── Log ──────────────────────────────────────────────────────────
    private JTextArea logArea;

    public GameFrame(Game game, String playerName, String startCity) {
        this.game = game;
        this.playerName = playerName;
        loadIcons();
        buildFrame();
        buildTopBar();
        buildCenterArea();
        buildBottomBar();
        refreshAll();
        showIntro(startCity);
        setVisible(true);
        this.getRootPane().registerKeyboardAction(
                e -> showPauseMenu(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void showPauseMenu() {
        // We use a custom styled array of buttons
        Object[] options = {"Continue", "Field Manual", "Restart Game", "Main Menu"};

        // Custom medieval themed dialog
        int choice = JOptionPane.showOptionDialog(
                this,
                "─── GAME PAUSED ───\nWhat are your orders, Commander " + playerName + "?",
                "Pause Menu",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                iconCastle,
                options,
                options[0]
        );

        switch (choice) {
            case 0: // Continue
                break;
            case 1: // Field Manual
                views.HowToPlayDialog.show(this);
                break;
            case 2: // Restart
                handleRestart();
                break;
            case 3: // Main Menu
                handleExitToMenu();
                break;
        }
    }

    private void handleRestart() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure? All progress in this campaign will be lost.",
                "Confirm Restart", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Get the player's starting city name to reset the engine
                String startCity = game.getPlayer().getControlledCities().get(0).getName();
                engine.Game newGame = new engine.Game(playerName, startCity);
                new GameFrame(newGame, playerName, startCity);
                this.dispose();
            } catch (IOException ex) {
                showError("Failed to restart: " + ex.getMessage());
            }
        }
    }

    private void handleExitToMenu() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Return to the main menu?", "Exit Game", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new MainMenuFrame();
            this.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ASSET LOADING
    // ─────────────────────────────────────────────────────────────────
    private void loadIcons() {
        bgMap    = loadImg("background/FantasyWorldMap.png");
        iconGold  = loadIcon("icons/gold-bars.png", 28, 28);
        iconFood  = loadIcon("icons/farm.png",      28, 28);
        iconArmy  = loadIcon("icons/army.png",      28, 28);
        iconCastle= loadIcon("icons/castle.png",    28, 28);
        iconFarm  = loadIcon("icons/farm.png",      22, 22);
        iconSwords= loadIcon("icons/swords.png",    22, 22);
    }

    private BufferedImage loadImg(String path) {
        try { return ImageIO.read(new File(path)); } catch (Exception e) { return null; }
    }

    private ImageIcon loadIcon(String path, int w, int h) {
        try {
            BufferedImage raw = ImageIO.read(new File(path));
            return new ImageIcon(raw.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) { return null; }
    }

    // ─────────────────────────────────────────────────────────────────
    //  FRAME SETUP
    // ─────────────────────────────────────────────────────────────────
    private void buildFrame() {
        setTitle("Conqueror — " + playerName + "'s Empire");
        setSize(1280, 780);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        // Dark map background
        JPanel bg = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgMap != null) {
                    g.drawImage(bgMap, 0, 0, getWidth(), getHeight(), null);
                    g.setColor(new Color(0, 0, 0, 120));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g.setColor(new Color(18, 12, 5));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        bg.setOpaque(false);
        setContentPane(bg);

    }

    // ─────────────────────────────────────────────────────────────────
    //  TOP BAR (Resources + Player Info)
    // ─────────────────────────────────────────────────────────────────
    private void buildTopBar() {
        // Switch to BoxLayout to allow the "Glue" to work properly
        JPanel bar = darkPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(160, 120, 40)),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)
        ));

        // --- LEFT SIDE: Resources ---
        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftSide.setOpaque(false);

        // Player badge
        JLabel playerBadge = new JLabel("⚜  " + playerName.toUpperCase());
        playerBadge.setFont(new Font("Serif", Font.BOLD, 17));
        playerBadge.setForeground(new Color(220, 175, 55));
        leftSide.add(playerBadge);
        leftSide.add(makeSep());

        // Gold
        if (iconGold != null) leftSide.add(new JLabel(iconGold));
        lblGold = resourceLabel("0");
        leftSide.add(lblGold);
        leftSide.add(makeSep());

        // Food
        if (iconFood != null) leftSide.add(new JLabel(iconFood));
        lblFood = resourceLabel("0");
        leftSide.add(lblFood);
        leftSide.add(makeSep());

        // Turn
        JLabel turnIcon = new JLabel("⏳");
        turnIcon.setFont(new Font("Serif", Font.PLAIN, 20));
        leftSide.add(turnIcon);
        lblTurn = resourceLabel("Turn 1 / 30");
        leftSide.add(lblTurn);

        // --- RIGHT SIDE: Action Buttons ---
        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightSide.setOpaque(false);

        // Strategic Map Button
        JButton mapBtn = smallBtn("🗺  STRATEGIC MAP");
        mapBtn.addActionListener(e -> {
            AudioManager.get().playClick();
            new StrategicMapDialog(this, game, bgMap).setVisible(true);
        });
        rightSide.add(mapBtn);

        // End Turn Button
        JButton endTurnBtn = goldButton("END TURN  ►");
        endTurnBtn.addActionListener(e -> doEndTurn());
        rightSide.add(endTurnBtn);

        // Combine them
        bar.add(leftSide, BorderLayout.WEST);
        bar.add(rightSide, BorderLayout.EAST);

        getContentPane().add(bar, BorderLayout.NORTH);
    }

    // ─────────────────────────────────────────────────────────────────
    //  CENTER (3-column layout)
    // ─────────────────────────────────────────────────────────────────
    private void buildCenterArea() {
        JPanel center = new JPanel(new BorderLayout(6, 0));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        center.add(buildLeftPanel(),   BorderLayout.WEST);
        center.add(buildCityDetail(),  BorderLayout.CENTER);
        center.add(buildRightPanel(),  BorderLayout.EAST);

        getContentPane().add(center, BorderLayout.CENTER);
    }

    // ─── LEFT: City List ──────────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = glassPanel(220, 0);
        panel.setLayout(new BorderLayout(0, 6));

        JLabel header = panelHeader("🌍  CITIES", iconCastle);
        panel.add(header, BorderLayout.NORTH);

        cityListModel = new DefaultListModel<>();
        for (City c : game.getAvailableCities())
            cityListModel.addElement(c);

        cityList = new JList<>(cityListModel);
        cityList.setOpaque(false);
        cityList.setFont(new Font("Serif", Font.BOLD, 15));
        cityList.setForeground(new Color(230, 210, 150));
        cityList.setSelectionBackground(new Color(140, 100, 20, 180));
        cityList.setSelectionForeground(Color.WHITE);
        cityList.setFixedCellHeight(44);
        cityList.setCellRenderer(new CityListRenderer());

        cityList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshCityDetail();
        });

        JScrollPane scroll = darkScroll(cityList);
        panel.add(scroll, BorderLayout.CENTER);

        // Quick-action buttons
        JPanel actions = new JPanel(new GridLayout(3, 1, 4, 4));
        actions.setOpaque(false);
        JButton buildBtn   = smallBtn("🏗  Build");
        JButton upgradeBtn = smallBtn("⬆  Upgrade");
        JButton recruitBtn = smallBtn("⚔  Recruit");
        buildBtn.addActionListener(e -> doBuild());
        upgradeBtn.addActionListener(e -> doUpgrade());
        recruitBtn.addActionListener(e -> doRecruit());
        actions.add(buildBtn);
        actions.add(upgradeBtn);
        actions.add(recruitBtn);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    // ─── CENTER: City Detail ──────────────────────────────────────────
    private JPanel buildCityDetail() {
        cityDetailPanel = glassPanel(0, 0);
        cityDetailPanel.setLayout(new BorderLayout(0, 8));

        cityNameLabel = new JLabel("Select a City", SwingConstants.CENTER);
        cityNameLabel.setFont(new Font("Serif", Font.BOLD, 26));
        cityNameLabel.setForeground(new Color(220, 175, 55));
        cityNameLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
        cityDetailPanel.add(cityNameLabel, BorderLayout.NORTH);

        cityStatusLabel = new JLabel("", SwingConstants.CENTER);
        cityStatusLabel.setFont(new Font("Serif", Font.ITALIC, 14));
        cityStatusLabel.setForeground(new Color(200, 180, 120));

        JPanel innerSplit = new JPanel(new GridLayout(1, 2, 8, 0));
        innerSplit.setOpaque(false);

        // Buildings
        JPanel bldPane = glassPanel(0, 0);
        bldPane.setLayout(new BorderLayout(0, 4));
        bldPane.add(panelHeader("🏛  BUILDINGS", null), BorderLayout.NORTH);
        buildingsPanel = new JPanel();
        buildingsPanel.setOpaque(false);
        buildingsPanel.setLayout(new BoxLayout(buildingsPanel, BoxLayout.Y_AXIS));
        JScrollPane bScroll = darkScroll(buildingsPanel);
        bldPane.add(bScroll, BorderLayout.CENTER);
        innerSplit.add(bldPane);

        // Defending Army
        JPanel armyPane = glassPanel(0, 0);
        armyPane.setLayout(new BorderLayout(0, 4));
        armyPane.add(panelHeader("🛡  DEFENDING ARMY", iconArmy), BorderLayout.NORTH);
        armyPanel = new JPanel();
        armyPanel.setOpaque(false);
        armyPanel.setLayout(new BoxLayout(armyPanel, BoxLayout.Y_AXIS));
        JScrollPane aScroll = darkScroll(armyPanel);
        armyPane.add(aScroll, BorderLayout.CENTER);

        // Army action buttons
        JPanel armyBtns = new JPanel(new GridLayout(1, 2, 4, 0));
        armyBtns.setOpaque(false);
        JButton initiateBtn = smallBtn("⚔  Form Army");
        JButton siegeBtn    = smallBtn("⚙  Lay Siege");
        initiateBtn.addActionListener(e -> doInitiateArmy());
        siegeBtn.addActionListener(e -> doLaySiege());
        armyBtns.add(initiateBtn);
        armyBtns.add(siegeBtn);
        armyPane.add(armyBtns, BorderLayout.SOUTH);

        innerSplit.add(armyPane);

        cityDetailPanel.add(cityStatusLabel, BorderLayout.SOUTH);
        cityDetailPanel.add(innerSplit, BorderLayout.CENTER);

        return cityDetailPanel;
    }

    // ─── RIGHT: Controlled Armies ─────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = glassPanel(240, 0);
        panel.setLayout(new BorderLayout(0, 6));

        panel.add(panelHeader("⚔  YOUR ARMIES", iconArmy), BorderLayout.NORTH);

        armyListModel = new DefaultListModel<>();
        armyList = new JList<>(armyListModel);
        armyList.setOpaque(false);
        armyList.setFont(new Font("Serif", Font.PLAIN, 13));
        armyList.setForeground(new Color(220, 200, 140));
        armyList.setSelectionBackground(new Color(140, 100, 20, 180));
        armyList.setSelectionForeground(Color.WHITE);
        armyList.setFixedCellHeight(56);
        armyList.setCellRenderer(new ArmyListRenderer());
        armyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshArmyInfo();
        });

        panel.add(darkScroll(armyList), BorderLayout.CENTER);

        armyInfoLabel = new JLabel("<html><center><i>Select an army</i></center></html>", SwingConstants.CENTER);
        armyInfoLabel.setFont(new Font("Serif", Font.ITALIC, 13));
        armyInfoLabel.setForeground(new Color(180, 160, 100));

        JPanel armyActions = new JPanel(new GridLayout(3, 1, 4, 4));
        armyActions.setOpaque(false);
        armyActions.add(armyInfoLabel);
        JButton marchBtn    = smallBtn("🏃  March To...");
        JButton resolveBtn  = smallBtn("⚔  Auto Resolve");
        marchBtn.addActionListener(e -> doMarchArmy());
        resolveBtn.addActionListener(e -> doAutoResolve());
        armyActions.add(marchBtn);
        armyActions.add(resolveBtn);
        panel.add(armyActions, BorderLayout.SOUTH);

        // Log panel below armies
        JPanel logPanel = glassPanel(240, 180);
        logPanel.setLayout(new BorderLayout(0, 4));
        logPanel.add(panelHeader("📜  LOG", null), BorderLayout.NORTH);
        logArea = new JTextArea();
        logArea.setOpaque(false);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setForeground(new Color(200, 185, 130));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logPanel.add(darkScroll(logArea), BorderLayout.CENTER);

        JPanel rightColumn = new JPanel(new BorderLayout(0, 6));
        rightColumn.setOpaque(false);
        rightColumn.add(panel, BorderLayout.CENTER);
        rightColumn.add(logPanel, BorderLayout.SOUTH);
        return rightColumn;
    }

    // ─────────────────────────────────────────────────────────────────
    //  BOTTOM BAR
    // ─────────────────────────────────────────────────────────────────
    private void buildBottomBar() {
        JPanel bar = darkPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        bar.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(160, 120, 40)));

        JLabel hint = new JLabel(
            "Select a city to manage it  •  Form armies from defending units  •  March to enemy cities  •  Auto-resolve battles");
        hint.setFont(new Font("Serif", Font.ITALIC, 13));
        hint.setForeground(new Color(160, 140, 80));
        bar.add(hint);

        getContentPane().add(bar, BorderLayout.SOUTH);
    }

    // ─────────────────────────────────────────────────────────────────
    //  REFRESH METHODS
    // ─────────────────────────────────────────────────────────────────
    private void refreshAll() {
        refreshTopBar();
        refreshCityList();
        refreshCityDetail();
        refreshArmyList();
        // DYNAMIC MUSIC INTEGRATION
        boolean isTense = game.getPlayer().getTreasury() < 1000 ||
                game.getAvailableCities().stream().anyMatch(City::isUnderSiege);
        AudioManager.get().setTensionMode(isTense);
    }

    private void refreshTopBar() {
        lblGold.setText(String.format("%,.0f", game.getPlayer().getTreasury()));
        lblFood.setText(String.format("%,.0f", game.getPlayer().getFood()));
        lblTurn.setText("Turn " + game.getCurrentTurnCount() + " / " + game.getMaxTurnCount());
    }

    private void refreshCityList() {
        City selected = cityList.getSelectedValue();
        cityListModel.clear();
        for (City c : game.getAvailableCities())
            cityListModel.addElement(c);
        if (selected != null) cityList.setSelectedValue(selected, true);
    }

    private void refreshCityDetail() {
        City city = cityList.getSelectedValue();
        buildingsPanel.removeAll();
        armyPanel.removeAll();

        if (city == null) {
            cityNameLabel.setText("Select a City");
            cityStatusLabel.setText("");
            buildingsPanel.revalidate();
            buildingsPanel.repaint();
            armyPanel.revalidate();
            armyPanel.repaint();
            return;
        }

        boolean owned = game.getPlayer().getControlledCities().contains(city);
        boolean enemy = !owned;

        cityNameLabel.setText((owned ? "🏰 " : "⚔ ") + city.getName().toUpperCase());
        cityNameLabel.setForeground(owned ? new Color(80, 200, 100) : new Color(220, 80, 60));

        String statusStr = owned ? "✅ Your Territory" : "🔴 Enemy Territory";
        if (city.isUnderSiege()) statusStr += "  |  ⚙ UNDER SIEGE (Turn " + city.getTurnsUnderSiege() + "/3)";
        cityStatusLabel.setText(statusStr);

        // Buildings
        if (owned) {
            if (city.getEconomicalBuildings().isEmpty() && city.getMilitaryBuildings().isEmpty()) {
                buildingsPanel.add(italicLabel("No buildings yet"));
            }
            for (EconomicBuilding b : city.getEconomicalBuildings())
                buildingsPanel.add(buildingCard(b));
            for (MilitaryBuilding b : city.getMilitaryBuildings())
                buildingsPanel.add(buildingCard(b));
        } else {
            buildingsPanel.add(italicLabel("Intelligence: buildings unknown"));
        }

        // Defending Army
        Army def = city.getDefendingArmy();
        if (def != null && !def.getUnits().isEmpty()) {
            for (Unit u : def.getUnits())
                armyPanel.add(unitCard(u, owned));
        } else {
            armyPanel.add(italicLabel("No defending units"));
        }

        buildingsPanel.revalidate(); buildingsPanel.repaint();
        armyPanel.revalidate(); armyPanel.repaint();
    }

    private void refreshArmyList() {
        Army selected = armyList.getSelectedValue();
        armyListModel.clear();
        for (Army a : game.getPlayer().getControlledArmies())
            armyListModel.addElement(a);
        if (selected != null) armyList.setSelectedValue(selected, true);
        refreshArmyInfo();
    }

    private void refreshArmyInfo() {
        Army a = armyList.getSelectedValue();
        if (a == null) {
            armyInfoLabel.setText("<html><center><i>Select an army</i></center></html>");
            return;
        }
        String status = switch (a.getCurrentStatus()) {
            case IDLE -> "⚓ IDLE";
            case MARCHING -> "🏃 MARCHING → " + a.getTarget() + " (" + a.getDistancetoTarget() + " turns)";
            case BESIEGING -> "⚙ BESIEGING " + a.getCurrentLocation();
        };
        armyInfoLabel.setText("<html><center>📍 " + a.getCurrentLocation() + "<br>" + status + "<br>"
            + a.getUnits().size() + " units</center></html>");
    }

    // ─────────────────────────────────────────────────────────────────
    //  CARD BUILDERS
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildingCard(Building b) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(80, 60, 20, 120)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        String typeName = b.getClass().getSimpleName();
        String emoji = switch (typeName) {
            case "Farm" -> "🌾";
            case "Market" -> "💰";
            case "Barracks" -> "⚔";
            case "Stable" -> "🐴";
            case "ArcheryRange" -> "🏹";
            default -> "🏛";
        };

        JLabel nameL = new JLabel(emoji + "  " + typeName);
        nameL.setFont(new Font("Serif", Font.BOLD, 14));
        nameL.setForeground(new Color(220, 200, 140));

        String detail = "Lvl " + b.getLevel() + "/3";
        if (b instanceof EconomicBuilding eb) detail += "  |  +" + eb.harvest() + " resources/turn";
        if (b instanceof MilitaryBuilding mb) detail += "  |  Recruit cost: " + mb.getRecruitmentCost();
        if (b.isCoolDown()) detail += "  |  ⏳ Cooling";

        JLabel detailL = new JLabel(detail);
        detailL.setFont(new Font("Serif", Font.PLAIN, 12));
        detailL.setForeground(new Color(170, 155, 100));

        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(nameL);
        text.add(detailL);
        card.add(text, BorderLayout.CENTER);

        // Level dots
        JPanel dots = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        dots.setOpaque(false);
        for (int i = 1; i <= 3; i++) {
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Serif", Font.PLAIN, 16));
            dot.setForeground(i <= b.getLevel() ? new Color(220, 170, 30) : new Color(60, 50, 30));
            dots.add(dot);
        }
        card.add(dots, BorderLayout.EAST);

        return card;
    }

    private JPanel unitCard(Unit u, boolean showActions) {
        JPanel card = new JPanel(new BorderLayout(6, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 50, 20, 100)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        String typeName = u.getClass().getSimpleName();
        String emoji = switch (typeName) {
            case "Archer" -> "🏹";
            case "Infantry" -> "⚔";
            case "Cavalry" -> "🐴";
            default -> "👤";
        };

        JLabel nameL = new JLabel(emoji + " " + typeName + "  Lvl " + u.getLevel());
        nameL.setFont(new Font("Serif", Font.BOLD, 13));
        nameL.setForeground(new Color(210, 190, 130));

        int pct = u.getMaxSoldierCount() > 0 ?
            (int)(100.0 * u.getCurrentSoldierCount() / u.getMaxSoldierCount()) : 0;
        Color barColor = pct > 60 ? new Color(60, 180, 60) :
                         pct > 30 ? new Color(200, 160, 30) : new Color(200, 50, 50);

        JProgressBar hp = new JProgressBar(0, u.getMaxSoldierCount());
        hp.setValue(u.getCurrentSoldierCount());
        hp.setString(u.getCurrentSoldierCount() + "/" + u.getMaxSoldierCount());
        hp.setStringPainted(true);
        hp.setForeground(barColor);
        hp.setBackground(new Color(30, 20, 10));
        hp.setFont(new Font("Serif", Font.PLAIN, 11));
        hp.setBorder(BorderFactory.createLineBorder(new Color(80, 60, 20), 1));
        hp.setPreferredSize(new Dimension(150, 16));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(nameL);
        text.add(hp);
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    //  GAME ACTIONS
    // ─────────────────────────────────────────────────────────────────
    private void doBuild() {
        City city = requireOwnedCity();
        if (city == null) return;

        String[] options = {"Farm", "Market", "Barracks", "Stable", "ArcheryRange"};
        String choice = (String) JOptionPane.showInputDialog(this,
            "Choose building to construct in " + city.getName() + ":",
            "Build", JOptionPane.PLAIN_MESSAGE, iconCastle, options, options[0]);
        if (choice == null) return;

        try {
            game.getPlayer().build(choice.toLowerCase(), city.getName());
            log("✅ Built " + choice + " in " + city.getName());
            refreshAll();
        } catch (NotEnoughGoldException ex) {
            showError("Not enough gold to build " + choice + "!");
        }
    }

    private void doUpgrade() {
        City city = requireOwnedCity();
        if (city == null) return;

        // Gather all buildings
        ArrayList<Building> allBuildings = new ArrayList<>();
        allBuildings.addAll(city.getEconomicalBuildings());
        allBuildings.addAll(city.getMilitaryBuildings());

        if (allBuildings.isEmpty()) { showError("No buildings to upgrade."); return; }

        String[] labels = allBuildings.stream()
            .map(b -> b.getClass().getSimpleName() + " (Lvl " + b.getLevel() + ") — Cost: " + b.getUpgradeCost() + " 💰")
            .toArray(String[]::new);

        String choice = (String) JOptionPane.showInputDialog(this,
            "Select building to upgrade in " + city.getName() + ":",
            "Upgrade Building", JOptionPane.PLAIN_MESSAGE, iconSwords, labels, labels[0]);
        if (choice == null) return;

        int idx = 0;
        for (int i = 0; i < labels.length; i++) if (labels[i].equals(choice)) { idx = i; break; }
        Building b = allBuildings.get(idx);

        try {
            game.getPlayer().upgradeBuilding(b);
            log("⬆  Upgraded " + b.getClass().getSimpleName() + " in " + city.getName() + " to Lvl " + b.getLevel());
            refreshAll();
        } catch (NotEnoughGoldException ex) {
            showError("Not enough gold! Need " + b.getUpgradeCost());
        } catch (BuildingInCoolDownException ex) {
            showError("Building is cooling down. Wait for next turn.");
        } catch (MaxLevelException ex) {
            showError("Building is already at maximum level!");
        }
    }

    private void doRecruit() {
        City city = requireOwnedCity();
        if (city == null) return;

        if (city.getMilitaryBuildings().isEmpty()) {
            showError("No military buildings in " + city.getName() + ".\nBuild a Barracks, Stable, or Archery Range first.");
            return;
        }

        String[] types = city.getMilitaryBuildings().stream()
            .map(b -> {
                String t = b instanceof Barracks ? "infantry" :
                           b instanceof Stable ? "cavalry" : "archer";
                return t + " (from " + b.getClass().getSimpleName() + ", cost: " + b.getRecruitmentCost() + " 💰)";
            })
            .toArray(String[]::new);

        String choice = (String) JOptionPane.showInputDialog(this,
            "Recruit unit in " + city.getName() + ":",
            "Recruit", JOptionPane.PLAIN_MESSAGE, iconSwords, types, types[0]);
        if (choice == null) return;

        String unitType = choice.split(" ")[0];
        try {
            game.getPlayer().recruitUnit(unitType, city.getName());
            log("⚔  Recruited " + unitType + " in " + city.getName());
            refreshAll();
        } catch (BuildingInCoolDownException ex) {
            showError("Building is in cool-down. Wait for next turn.");
        } catch (MaxRecruitedException ex) {
            showError("Max recruits reached for this turn (3 per building).");
        } catch (NotEnoughGoldException ex) {
            showError("Not enough gold to recruit!");
        }
    }

    private void doInitiateArmy() {
        City city = requireOwnedCity();
        if (city == null) return;

        Army def = city.getDefendingArmy();
        if (def == null || def.getUnits().isEmpty()) {
            showError("No units to form an army from."); return;
        }

        String[] unitLabels = def.getUnits().stream()
            .map(u -> u.getClass().getSimpleName() + " Lvl" + u.getLevel() +
                      " [" + u.getCurrentSoldierCount() + "/" + u.getMaxSoldierCount() + "]")
            .toArray(String[]::new);

        String choice = (String) JOptionPane.showInputDialog(this,
            "Select the lead unit from " + city.getName() + "'s garrison:",
            "Form Army", JOptionPane.PLAIN_MESSAGE, iconArmy, unitLabels, unitLabels[0]);
        if (choice == null) return;

        int idx = 0;
        for (int i = 0; i < unitLabels.length; i++) if (unitLabels[i].equals(choice)) { idx = i; break; }
        Unit unit = def.getUnits().get(idx);

        game.getPlayer().initiateArmy(city, unit);
        log("⚔  Army formed at " + city.getName() + " with " + unit.getClass().getSimpleName());
        refreshAll();
    }

    private void doMarchArmy() {
        Army army = armyList.getSelectedValue();
        if (army == null) { showError("Select an army first."); return; }

        if (army.getCurrentStatus() == Status.BESIEGING) {
            showError("Army is currently besieging a city."); return;
        }

        // Offer non-controlled cities as targets
        ArrayList<City> targets = new ArrayList<>();
        for (City c : game.getAvailableCities()) {
            if (!c.getName().equals(army.getCurrentLocation()))
                targets.add(c);
        }
        if (targets.isEmpty()) { showError("No valid targets."); return; }

        String[] cityNames = targets.stream().map(City::getName).toArray(String[]::new);
        String choice = (String) JOptionPane.showInputDialog(this,
            "March army to which city?", "March Army",
            JOptionPane.PLAIN_MESSAGE, iconArmy, cityNames, cityNames[0]);
        if (choice == null) return;

        game.targetCity(army, choice);
        log("🏃 Army marching to " + choice + " (" + army.getDistancetoTarget() + " turns away)");
        refreshAll();
    }

    private void doLaySiege() {
        Army army = armyList.getSelectedValue();
        if (army == null) { showError("Select an army first."); return; }

        City target = null;
        for (City c : game.getAvailableCities()) {
            if (c.getName().equals(army.getCurrentLocation()) &&
                !game.getPlayer().getControlledCities().contains(c)) {
                target = c; break;
            }
        }
        if (target == null) {
            showError("Army must be at an enemy city to lay siege."); return;
        }

        try {
            game.getPlayer().laySiege(army, target);
            log("⚙  Siege laid on " + target.getName() + "!");
            refreshAll();
        } catch (TargetNotReachedException ex) {
            showError("Army has not reached the target city.");
        } catch (FriendlyCityException ex) {
            showError("Cannot lay siege to your own city!");
        }
    }

    private void doAutoResolve() {
        Army army = armyList.getSelectedValue();
        if (army == null) { showError("Select an army first."); return; }

        // Find the enemy city at army's location
        City target = null;
        for (City c : game.getAvailableCities()) {
            if (c.getName().equals(army.getCurrentLocation()) &&
                !game.getPlayer().getControlledCities().contains(c)) {
                target = c; break;
            }
        }
        if (target == null) {
            showError("Army must be at an enemy city to battle."); return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Auto-resolve battle at " + target.getName() + "?\n" +
            "Your forces: " + army.getUnits().size() + " units\n" +
            "Enemy forces: " + target.getDefendingArmy().getUnits().size() + " units",
            "Confirm Battle", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Army defender = target.getDefendingArmy();
            game.autoResolve(army, defender);

            if (game.getPlayer().getControlledCities().contains(target)) {
                log("🏆 VICTORY! " + target.getName() + " has been conquered!");
                JOptionPane.showMessageDialog(this,
                    "⚔  VICTORY!\n\n" + target.getName() + " has fallen to your forces!",
                    "City Conquered!", JOptionPane.INFORMATION_MESSAGE, iconCastle);
            } else {
                log("💀 DEFEAT at " + target.getName() + ". Army destroyed.");
                JOptionPane.showMessageDialog(this,
                    "Your army was defeated at " + target.getName() + ".",
                    "Defeat", JOptionPane.ERROR_MESSAGE);
            }
            refreshAll();
            checkGameOver();
        } catch (FriendlyFireException ex) {
            showError("Cannot attack your own forces!");
        }
    }

    private void doEndTurn() {
        game.endTurn();
        log("─────────────────── Turn " + game.getCurrentTurnCount() + " ───────────────────");

        if (game.getPlayer().getFood() < 500)
            log("⚠  WARNING: Food supplies running low!");
        if (game.getPlayer().getTreasury() < 1000)
            log("⚠  WARNING: Treasury is low!");

        refreshAll();
        checkGameOver();
    }

    private void checkGameOver() {
        if (!game.isGameOver()) return;

        boolean won = game.getPlayer().getControlledCities().size() == game.getAvailableCities().size();
        String msg = won
            ? "🏆 VICTORY!\n\nYou have conquered all cities and united the empire!\nYour legend will echo through the ages."
            : "💀 DEFEAT\n\nTime has run out. The empire remains divided.\nPerhaps another commander will succeed...";

        JOptionPane.showMessageDialog(this, msg, won ? "VICTORY!" : "GAME OVER",
            won ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);

        int r = JOptionPane.showConfirmDialog(this, "Play again?",
            "New Game", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            new MainMenuFrame();
        }
        dispose();
    }

    // ─────────────────────────────────────────────────────────────────
    //  INTRO DIALOG
    // ─────────────────────────────────────────────────────────────────
    private void showIntro(String city) {
        String story = "<html><body style='width:320px;font-family:serif;font-size:13px;'>" +
            "<h2 style='color:#D4AA30;'>The Year is 60 BC...</h2>" +
            "<p>Three great powers struggle for dominance of the known world.<br>" +
            "<b>Cairo</b>, <b>Rome</b>, and <b>Sparta</b> each claim supremacy.</p>" +
            "<p>You, <b style='color:#D4AA30;'>" + playerName + "</b>, rise to lead the armies of " +
            "<b style='color:#80C860;'>" + city + "</b>.</p>" +
            "<p>You have <b>30 turns</b> to conquer all cities.<br>" +
            "Manage your treasury wisely, recruit powerful armies,<br>" +
            "and crush your enemies before time runs out.</p>" +
            "<p style='color:#C8A050;'><i>The fate of the ancient world is in your hands.</i></p>" +
            "</body></html>";

        JLabel label = new JLabel(story);
        JOptionPane.showMessageDialog(this, label, "The Story Begins",
            JOptionPane.INFORMATION_MESSAGE, iconCastle);
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────
    private City requireOwnedCity() {
        City city = cityList.getSelectedValue();
        if (city == null) { showError("Select a city first."); return null; }
        if (!game.getPlayer().getControlledCities().contains(city)) {
            showError("You don't control " + city.getName() + "."); return null;
        }
        return city;
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Action Failed", JOptionPane.WARNING_MESSAGE);
    }

    private JLabel resourceLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Serif", Font.BOLD, 16));
        l.setForeground(new Color(230, 210, 130));
        return l;
    }

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(new Color(100, 80, 30, 150));
        sep.setPreferredSize(new Dimension(2, 28));
        return sep;
    }

    private JLabel italicLabel(String text) {
        JLabel l = new JLabel("<html><i>" + text + "</i></html>");
        l.setFont(new Font("Serif", Font.ITALIC, 13));
        l.setForeground(new Color(140, 130, 90));
        l.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel panelHeader(String text, ImageIcon icon) {
        JLabel l = new JLabel(text, icon, SwingConstants.LEFT);
        l.setFont(new Font("Serif", Font.BOLD, 14));
        l.setForeground(new Color(210, 165, 45));
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(160, 120, 40, 120)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        l.setOpaque(false);
        return l;
    }

    private JPanel darkPanel(LayoutManager lm) {
        JPanel p = new JPanel(lm) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(15, 10, 5, 230));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setOpaque(false);
        return p;
    }

    private JPanel glassPanel(int w, int h) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 8, 3, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(140, 100, 30, 100));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
            }
        };
        p.setOpaque(false);
        if (w > 0) p.setPreferredSize(new Dimension(w, h > 0 ? h : p.getPreferredSize().height));
        return p;
    }

    private JScrollPane darkScroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.setBorder(BorderFactory.createLineBorder(new Color(80, 60, 20, 80), 1));
        s.getVerticalScrollBar().setUnitIncrement(12);
        return s;
    }

    private JButton smallBtn(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? new Color(60, 40, 5) :
                           getModel().isRollover() ? new Color(100, 70, 15) : new Color(35, 25, 8);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(160, 120, 35, 150));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font("Serif", Font.BOLD, 13));
        b.setForeground(new Color(220, 195, 120));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton goldButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isPressed() ? new Color(100, 70, 10) :
                            getModel().isRollover() ? new Color(200, 150, 30) : new Color(170, 120, 20);
                Color bot = getModel().isPressed() ? new Color(60, 40, 5) :
                            getModel().isRollover() ? new Color(140, 100, 10) : new Color(110, 75, 10);
                g2.setPaint(new java.awt.GradientPaint(0, 0, top, 0, getHeight(), bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(220, 170, 50, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font("Serif", Font.BOLD, 15));
        b.setForeground(new Color(255, 240, 180));
        b.setPreferredSize(new Dimension(180, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ─────────────────────────────────────────────────────────────────
    //  CUSTOM LIST CELL RENDERERS
    // ─────────────────────────────────────────────────────────────────
    private class CityListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            City city = (City) value;
            boolean owned = game.getPlayer().getControlledCities().contains(city);
            boolean siege = city.isUnderSiege();

            JPanel card = new JPanel(new BorderLayout(6, 0));
            card.setOpaque(true);
            card.setBackground(isSelected ? new Color(100, 70, 15, 200) : new Color(20, 14, 6, 200));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 45, 15, 100)),
                BorderFactory.createEmptyBorder(4, 10, 4, 8)
            ));

            String emoji = owned ? "🏰" : "⚔";
            JLabel nameL = new JLabel(emoji + "  " + city.getName());
            nameL.setFont(new Font("Serif", Font.BOLD, 15));
            nameL.setForeground(owned ? new Color(80, 200, 100) :
                                siege ? new Color(255, 160, 40) : new Color(220, 80, 60));

            String sub = owned ? "Your Territory" :
                        (siege ? "⚙ Under Siege" : "Enemy City");
            JLabel subL = new JLabel(sub);
            subL.setFont(new Font("Serif", Font.ITALIC, 11));
            subL.setForeground(new Color(160, 140, 90));

            JPanel text = new JPanel(new GridLayout(2, 1, 0, 1));
            text.setOpaque(false);
            text.add(nameL);
            text.add(subL);
            card.add(text, BorderLayout.CENTER);

            int defenders = city.getDefendingArmy() != null ?
                city.getDefendingArmy().getUnits().size() : 0;
            JLabel defL = new JLabel("🛡 " + defenders);
            defL.setFont(new Font("Serif", Font.BOLD, 13));
            defL.setForeground(new Color(180, 160, 80));
            card.add(defL, BorderLayout.EAST);

            return card;
        }
    }

    private class ArmyListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            Army army = (Army) value;

            JPanel card = new JPanel(new BorderLayout(6, 0));
            card.setOpaque(true);
            card.setBackground(isSelected ? new Color(100, 70, 15, 200) : new Color(20, 14, 6, 200));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 45, 15, 100)),
                BorderFactory.createEmptyBorder(6, 10, 6, 8)
            ));

            String statusEmoji = switch (army.getCurrentStatus()) {
                case IDLE -> "⚓";
                case MARCHING -> "🏃";
                case BESIEGING -> "⚙";
            };
            JLabel locL = new JLabel(statusEmoji + "  " + army.getCurrentLocation());
            locL.setFont(new Font("Serif", Font.BOLD, 13));
            locL.setForeground(new Color(210, 190, 130));

            String detail = army.getUnits().size() + " units";
            if (army.getCurrentStatus() == Status.MARCHING)
                detail += " → " + army.getTarget() + " (" + army.getDistancetoTarget() + " turns)";
            JLabel detL = new JLabel(detail);
            detL.setFont(new Font("Serif", Font.ITALIC, 11));
            detL.setForeground(new Color(150, 135, 85));

            JPanel text = new JPanel(new GridLayout(2, 1, 0, 1));
            text.setOpaque(false);
            text.add(locL);
            text.add(detL);
            card.add(text, BorderLayout.CENTER);

            return card;
        }
    }
}
