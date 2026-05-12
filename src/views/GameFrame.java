package views;

import engine.*;
import buildings.*;
import units.*;
import exceptions.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class GameFrame extends JFrame {

    private final Game   game;
    private final String playerName;

    // Icons
    private BufferedImage bgMap;
    private ImageIcon iconGold, iconFood, iconArmy, iconCastle, iconSwords;

    // Top bar labels
    private JLabel lblGold, lblFood, lblTurn;
    private JButton autoWinBtn;
    private AIPlayer aiPlayer;

    // City list (left)
    private DefaultListModel<City> cityListModel;
    private JList<City>            cityList;

    // City detail (center) — toggled by CardLayout
    private JPanel  centerCards;       // CardLayout host
    private static final String CARD_DETAIL = "detail";
    private static final String CARD_MAP    = "map";
    private JButton mapToggleBtn;
    private CityMapPanel cityMapPanel;

    private JLabel  cityNameLabel, cityStatusLabel;
    private JPanel  buildingsPanel, armyPanel;

    // Army list (right) — multi-select
    private DefaultListModel<Army> armyListModel;
    private JList<Army>            armyList;
    private JLabel  armyInfoLabel;

    // Hint panel
    private JLabel   hintUrgencyIcon, hintTitle;
    private JTextArea hintBody;

    // Log
    private JTextArea logArea;

    // ── Constructor ───────────────────────────────────────────────
    public GameFrame(Game game, String playerName, String startCity) {
        this.game       = game;
        this.playerName = playerName;
        FontManager.init();
        loadIcons();
        buildFrame();
        buildTopBar();
        buildCenter();
        buildBottomBar();
        bindKeys();
        refreshAll();
        showIntro(startCity);
        setVisible(true);
    }

    // ── ICONS ─────────────────────────────────────────────────────
    private void loadIcons() {
        bgMap      = loadImg("background/FantasyWorldMap.png");
        iconGold   = loadIcon("icons/gold-bars.png", 26, 26);
        iconFood   = loadIcon("icons/farm.png",      26, 26);
        iconArmy   = loadIcon("icons/army.png",      26, 26);
        iconCastle = loadIcon("icons/castle.png",    26, 26);
        iconSwords = loadIcon("icons/swords.png",    22, 22);
    }
    private BufferedImage loadImg(String p){try{return ImageIO.read(new File(p));}catch(Exception e){return null;}}
    private ImageIcon loadIcon(String p,int w,int h){
        try{BufferedImage r=ImageIO.read(new File(p));return new ImageIcon(r.getScaledInstance(w,h,Image.SCALE_SMOOTH));}
        catch(Exception e){return null;}}

    // ── FRAME ────────────────────────────────────────────────────
    private void buildFrame() {
        setTitle("Conqueror — " + playerName + "'s Empire");
        setSize(1360, 840);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel bg = new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);
                if(bgMap!=null){g.drawImage(bgMap,0,0,getWidth(),getHeight(),null);
                    g.setColor(new Color(0,0,0,125));g.fillRect(0,0,getWidth(),getHeight());}
                else{g.setColor(new Color(14,9,3));g.fillRect(0,0,getWidth(),getHeight());}
            }
        };
        bg.setOpaque(false);
        setContentPane(bg);
    }

    // ── KEY BINDINGS ─────────────────────────────────────────────
    private void bindKeys() {
        // ESC → Pause Menu
        getRootPane().registerKeyboardAction(e -> showPauseMenu(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void showPauseMenu() {
        AudioManager.get().playClick();
        PauseMenuDialog.Choice choice = PauseMenuDialog.show(this);
        switch (choice) {
            case CONTINUE    -> { /* do nothing */ }
            case HOW_TO_PLAY -> HowToPlayDialog.show(this);
            case RESTART     -> { stopAI(); new MainMenuFrame(); dispose(); }
            case EXIT        -> { stopAI(); AudioManager.get().shutdown(); System.exit(0); }
        }
    }

    // ── TOP BAR ──────────────────────────────────────────────────
    private void buildTopBar() {
        JPanel left = darkPanel(new FlowLayout(FlowLayout.LEFT, 13, 8));
        JLabel badge = new JLabel("⚜  " + playerName.toUpperCase());
        badge.setFont(FontManager.FONT_HEADING.deriveFont(17f));
        badge.setForeground(new Color(218,172,48));
        left.add(badge); left.add(sep());
        if(iconGold!=null) left.add(new JLabel(iconGold));
        lblGold=resLbl("0");      left.add(lblGold);  left.add(sep());
        if(iconFood!=null) left.add(new JLabel(iconFood));
        lblFood=resLbl("0");      left.add(lblFood);  left.add(sep());
        JLabel ti=new JLabel("⏳");ti.setFont(new Font("Serif",Font.PLAIN,19));left.add(ti);
        lblTurn=resLbl("Turn 1/30"); left.add(lblTurn);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 6));
        right.setOpaque(false);

        // Map toggle
        mapToggleBtn = topBtn("🗺  MAP");
        mapToggleBtn.addActionListener(e -> toggleMap());
        right.add(mapToggleBtn);

        // Advisor
        JButton advisor = topBtn("💡  ADVISOR");
        advisor.addActionListener(e -> showHint());
        right.add(advisor);

        // How to Play
        JButton help = topBtn("?  HOW TO PLAY");
        help.addActionListener(e -> { AudioManager.get().playClick(); HowToPlayDialog.show(this); });
        right.add(help);

        // Auto-Win
        autoWinBtn = topBtn("🤖  AUTO-WIN");
        autoWinBtn.addActionListener(e -> toggleAutoWin());
        right.add(autoWinBtn);

        // End Turn
        JButton end = goldBtn("END TURN  ►");
        end.addActionListener(e -> doEndTurn());
        right.add(end);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(14,9,3,232));
        bar.setBorder(BorderFactory.createMatteBorder(0,0,2,0,new Color(160,118,38)));
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        getContentPane().add(bar, BorderLayout.NORTH);
    }

    // ── CENTER (3-column) ────────────────────────────────────────
    private void buildCenter() {
        JPanel c = new JPanel(new BorderLayout(6,0));
        c.setOpaque(false);
        c.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        c.add(buildLeft(),   BorderLayout.WEST);
        c.add(buildMiddle(), BorderLayout.CENTER);
        c.add(buildRight(),  BorderLayout.EAST);
        getContentPane().add(c, BorderLayout.CENTER);
    }

    // LEFT — city list
    private JPanel buildLeft() {
        JPanel p = glass(234,0); p.setLayout(new BorderLayout(0,6));
        p.add(panelHead("🌍  CITIES", iconCastle), BorderLayout.NORTH);
        cityListModel = new DefaultListModel<>();
        game.getAvailableCities().forEach(cityListModel::addElement);
        cityList = new JList<>(cityListModel);
        cityList.setOpaque(false);
        cityList.setFont(FontManager.FONT_BODY_BOLD.deriveFont(14f));
        cityList.setForeground(new Color(228,208,148));
        cityList.setSelectionBackground(new Color(138,98,18,185));
        cityList.setSelectionForeground(Color.WHITE);
        cityList.setFixedCellHeight(46);
        cityList.setCellRenderer(new CityRenderer());
        cityList.addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) refreshCityDetail(); });
        p.add(darkScroll(cityList), BorderLayout.CENTER);
        JPanel acts = new JPanel(new GridLayout(3,1,4,4)); acts.setOpaque(false);
        JButton b1=smlBtn("🏗  Build");   b1.addActionListener(e->doBuild());
        JButton b2=smlBtn("⬆  Upgrade"); b2.addActionListener(e->doUpgrade());
        JButton b3=smlBtn("⚔  Recruit"); b3.addActionListener(e->doRecruit());
        acts.add(b1);acts.add(b2);acts.add(b3);
        p.add(acts, BorderLayout.SOUTH); return p;
    }

    // MIDDLE — CardLayout: Map ↔ City Detail
    private JPanel buildMiddle() {
        centerCards = new JPanel(new CardLayout()); centerCards.setOpaque(false);

        // ── MAP card ─────────────────────────────────────────────
        JPanel mapCard = glass(0,0); mapCard.setLayout(new BorderLayout(0,4));
        mapCard.add(panelHead("🗺  WORLD MAP — click a city to inspect it", null), BorderLayout.NORTH);
        cityMapPanel = new CityMapPanel(game, city -> {
            // Click on map selects city and switches to detail
            cityList.setSelectedValue(city, true);
            showDetailCard();
        });
        mapCard.add(cityMapPanel, BorderLayout.CENTER);
        centerCards.add(mapCard, CARD_MAP);

        // ── DETAIL card ──────────────────────────────────────────
        JPanel detailCard = glass(0,0); detailCard.setLayout(new BorderLayout(0,6));
        cityNameLabel = new JLabel("Select a City", SwingConstants.CENTER);
        cityNameLabel.setFont(FontManager.FONT_TITLE.deriveFont(27f));
        cityNameLabel.setForeground(new Color(218,172,48));
        cityNameLabel.setBorder(BorderFactory.createEmptyBorder(8,0,2,0));
        detailCard.add(cityNameLabel, BorderLayout.NORTH);
        cityStatusLabel = new JLabel("", SwingConstants.CENTER);
        cityStatusLabel.setFont(FontManager.FONT_ITALIC.deriveFont(13f));
        cityStatusLabel.setForeground(new Color(195,175,115));
        detailCard.add(cityStatusLabel, BorderLayout.SOUTH);

        JPanel split = new JPanel(new GridLayout(1,2,8,0)); split.setOpaque(false);
        JPanel bp = glass(0,0); bp.setLayout(new BorderLayout(0,4));
        bp.add(panelHead("🏛  BUILDINGS",null), BorderLayout.NORTH);
        buildingsPanel = new JPanel(); buildingsPanel.setOpaque(false);
        buildingsPanel.setLayout(new BoxLayout(buildingsPanel,BoxLayout.Y_AXIS));
        bp.add(darkScroll(buildingsPanel), BorderLayout.CENTER);
        split.add(bp);

        JPanel ap = glass(0,0); ap.setLayout(new BorderLayout(0,4));
        ap.add(panelHead("🛡  GARRISON",iconArmy), BorderLayout.NORTH);
        armyPanel = new JPanel(); armyPanel.setOpaque(false);
        armyPanel.setLayout(new BoxLayout(armyPanel,BoxLayout.Y_AXIS));
        ap.add(darkScroll(armyPanel), BorderLayout.CENTER);
        JPanel abtns = new JPanel(new GridLayout(1,2,4,0)); abtns.setOpaque(false);
        JButton form=smlBtn("⚔  Form Army"); form.addActionListener(e->doInitiateArmy());
        JButton siege=smlBtn("⚙  Lay Siege"); siege.addActionListener(e->doLaySiege());
        abtns.add(form);abtns.add(siege);
        ap.add(abtns, BorderLayout.SOUTH);
        split.add(ap);
        detailCard.add(split, BorderLayout.CENTER);
        centerCards.add(detailCard, CARD_DETAIL);

        return centerCards;
    }

    // RIGHT — multi-select armies + hint + log
    private JPanel buildRight() {
        JPanel armies = glass(254,0); armies.setLayout(new BorderLayout(0,6));
        armies.add(panelHead("⚔  YOUR ARMIES  (Ctrl+click = multi-select)", iconArmy), BorderLayout.NORTH);

        armyListModel = new DefaultListModel<>();
        armyList = new JList<>(armyListModel);
        armyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // ← multi-select
        armyList.setOpaque(false);
        armyList.setFont(FontManager.FONT_BODY.deriveFont(13f));
        armyList.setForeground(new Color(218,198,138));
        armyList.setSelectionBackground(new Color(138,98,18,185));
        armyList.setSelectionForeground(Color.WHITE);
        armyList.setFixedCellHeight(58);
        armyList.setCellRenderer(new ArmyRenderer());
        armyList.addListSelectionListener(e->{ if(!e.getValueIsAdjusting()) refreshArmyInfo(); });
        armies.add(darkScroll(armyList), BorderLayout.CENTER);

        armyInfoLabel = new JLabel("<html><center><i>Select armies above</i></center></html>", SwingConstants.CENTER);
        armyInfoLabel.setFont(FontManager.FONT_ITALIC.deriveFont(12f));
        armyInfoLabel.setForeground(new Color(175,158,98));
        JPanel aacts = new JPanel(new GridLayout(3,1,4,4)); aacts.setOpaque(false);
        aacts.add(armyInfoLabel);
        JButton march   = smlBtn("🏃  March To…  (all selected)");
        JButton resolve = smlBtn("⚔  Auto Resolve  (all selected)");
        march.addActionListener(e->doMarch());
        resolve.addActionListener(e->doAutoResolve());
        aacts.add(march);aacts.add(resolve);
        armies.add(aacts, BorderLayout.SOUTH);

        // Hint panel
        JPanel hintPanel = glass(254,158); hintPanel.setLayout(new BorderLayout(0,4));
        hintPanel.add(panelHead("💡  ADVISOR",null), BorderLayout.NORTH);
        JPanel hi = new JPanel(new BorderLayout(6,4)); hi.setOpaque(false);
        hi.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        JPanel tr = new JPanel(new BorderLayout(6,0)); tr.setOpaque(false);
        hintUrgencyIcon = new JLabel("");
        hintUrgencyIcon.setFont(new Font("Serif",Font.PLAIN,18));
        hintTitle = new JLabel("Press 💡 Advisor for a hint");
        hintTitle.setFont(FontManager.FONT_BODY_BOLD.deriveFont(12f));
        hintTitle.setForeground(new Color(218,185,65));
        tr.add(hintUrgencyIcon,BorderLayout.WEST); tr.add(hintTitle,BorderLayout.CENTER);
        hintBody = new JTextArea("Click 💡 ADVISOR in the top bar to get\na personalised strategic hint.");
        hintBody.setEditable(false); hintBody.setLineWrap(true); hintBody.setWrapStyleWord(true);
        hintBody.setOpaque(false);
        hintBody.setFont(FontManager.FONT_SMALL.deriveFont(11f));
        hintBody.setForeground(new Color(195,178,120));
        hi.add(tr,BorderLayout.NORTH); hi.add(darkScroll(hintBody),BorderLayout.CENTER);
        hintPanel.add(hi, BorderLayout.CENTER);

        // Log
        JPanel logPane = glass(254,148); logPane.setLayout(new BorderLayout(0,4));
        logPane.add(panelHead("📜  LOG",null), BorderLayout.NORTH);
        logArea = new JTextArea(); logArea.setEditable(false); logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true); logArea.setOpaque(false);
        logArea.setFont(FontManager.FONT_SMALL.deriveFont(Font.PLAIN,11f));
        logArea.setForeground(new Color(195,180,125));
        logPane.add(darkScroll(logArea), BorderLayout.CENTER);

        JPanel col = new JPanel(new BorderLayout(0,6)); col.setOpaque(false);
        col.add(armies, BorderLayout.CENTER);
        JPanel bot = new JPanel(new GridLayout(2,1,0,6)); bot.setOpaque(false);
        bot.add(hintPanel); bot.add(logPane);
        col.add(bot, BorderLayout.SOUTH);
        return col;
    }

    // ── BOTTOM BAR ───────────────────────────────────────────────
    private void buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER,8,6));
        bar.setBackground(new Color(14,9,3,232));
        bar.setBorder(BorderFactory.createMatteBorder(2,0,0,0,new Color(160,118,38)));
        JLabel hint = new JLabel("ESC = Pause  •  🗺 = World Map  •  Ctrl+click armies for multi-select  •  💡 = Advisor  •  🤖 = Auto-Win");
        hint.setFont(FontManager.FONT_ITALIC.deriveFont(12f));
        hint.setForeground(new Color(150,133,72));
        bar.add(hint);
        getContentPane().add(bar, BorderLayout.SOUTH);
    }

    // ── MAP TOGGLE ───────────────────────────────────────────────
    private void toggleMap() {
        AudioManager.get().playClick();
        CardLayout cl = (CardLayout) centerCards.getLayout();
        String showing = getShowingCard();
        if (CARD_MAP.equals(showing)) {
            showDetailCard();
        } else {
            cityMapPanel.repaint();
            cl.show(centerCards, CARD_MAP);
            mapToggleBtn.setText("📋  DETAIL");
        }
    }
    private void showDetailCard() {
        ((CardLayout)centerCards.getLayout()).show(centerCards, CARD_DETAIL);
        mapToggleBtn.setText("🗺  MAP");
    }
    private String getShowingCard() {
        // Inspect which component is visible
        for (Component c : centerCards.getComponents())
            if (c.isVisible()) {
                // Map card is first added
                for (int i=0;i<centerCards.getComponentCount();i++)
                    if (centerCards.getComponent(i)==c)
                        return i==0?CARD_MAP:CARD_DETAIL;
            }
        return CARD_DETAIL;
    }

    // ── AUTO-WIN ─────────────────────────────────────────────────
    private void toggleAutoWin() {
        if (aiPlayer != null && !aiPlayer.isDone()) {
            stopAI();
            return;
        }
        AudioManager.get().playClick();
        int confirm = JOptionPane.showConfirmDialog(this,
            "<html><b>Hand control to the AI?</b><br><br>" +
            "The AI will play automatically until it wins or the game ends.<br>" +
            "You can click '⏹ STOP AI' at any time to take back control.</html>",
            "Auto-Win Mode", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        autoWinBtn.setText("⏹  STOP AI");
        autoWinBtn.setForeground(new Color(255, 100, 80));
        log("🤖 AI Advisor taking control — press ⏹ STOP AI to regain control.");

        aiPlayer = new AIPlayer(game,
            // onRefresh — called each turn on EDT
            () -> { refreshAll(); cityMapPanel.repaint(); },
            // onFinished — when AI stops
            () -> {
                autoWinBtn.setText("🤖  AUTO-WIN");
                autoWinBtn.setForeground(new Color(218,192,118));
                refreshAll();
                checkGameOver();
            },
            // onToggleBtn — enable/disable
            enabled -> autoWinBtn.setEnabled(enabled)
        ) {
            // Override process() to pipe AI messages into the game log
            @Override
            protected void process(java.util.List<String> lines) {
                lines.forEach(GameFrame.this::log);
                cityMapPanel.repaint();
            }
        };
        aiPlayer.execute();
    }

    private void stopAI() {
        if (aiPlayer != null) {
            aiPlayer.stop();
            aiPlayer.cancel(false);
            aiPlayer = null;
        }
        autoWinBtn.setText("🤖  AUTO-WIN");
        autoWinBtn.setForeground(new Color(218,192,118));
        log("⏹ AI stopped. You have control.");
    }

    // ── ADVISOR HINT ─────────────────────────────────────────────
    private void showHint() {
        AudioManager.get().playClick();
        HintEngine.Hint h = HintEngine.analyse(game);
        hintTitle.setText(h.title()); hintBody.setText(h.body()); hintBody.setCaretPosition(0);
        Color tc = switch(h.urgency()){
            case CRITICAL->new Color(240,75,55); case HIGH->new Color(240,160,40);
            case MEDIUM->new Color(218,185,65);  case LOW->new Color(100,200,100);};
        String ic = switch(h.urgency()){
            case CRITICAL->"🚨"; case HIGH->"⚠"; case MEDIUM->"💡"; case LOW->"✅";};
        hintUrgencyIcon.setText(ic); hintTitle.setForeground(tc);

        JPanel pop = new JPanel(new BorderLayout(8,10));
        pop.setBackground(new Color(18,11,3));
        pop.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
        JLabel pt = new JLabel(ic+"  "+h.title());
        pt.setFont(FontManager.FONT_HEADING.deriveFont(16f)); pt.setForeground(tc);
        JTextArea pb = new JTextArea(h.body());
        pb.setEditable(false); pb.setLineWrap(true); pb.setWrapStyleWord(true);
        pb.setOpaque(false); pb.setFont(FontManager.FONT_BODY.deriveFont(13f));
        pb.setForeground(new Color(225,205,150)); pb.setPreferredSize(new Dimension(440,175));
        JLabel ut = new JLabel("Priority: "+h.urgency(), SwingConstants.RIGHT);
        ut.setFont(FontManager.FONT_SMALL.deriveFont(Font.ITALIC,11f)); ut.setForeground(tc);
        pop.add(pt,BorderLayout.NORTH); pop.add(pb,BorderLayout.CENTER); pop.add(ut,BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, pop, "Advisor", JOptionPane.PLAIN_MESSAGE, null);
    }

    // ── REFRESH ───────────────────────────────────────────────────
    public  void refreshAll()        { refreshTopBar(); refreshCityList(); refreshCityDetail(); refreshArmyList(); }
    private void refreshTopBar()     {
        lblGold.setText(String.format("%,.0f 💰",game.getPlayer().getTreasury()));
        lblFood.setText(String.format("%,.0f 🌾",game.getPlayer().getFood()));
        lblTurn.setText("Turn "+game.getCurrentTurnCount()+" / "+game.getMaxTurnCount());
    }
    private void refreshCityList()   {
        City sel=cityList.getSelectedValue(); cityListModel.clear();
        game.getAvailableCities().forEach(cityListModel::addElement);
        if(sel!=null) cityList.setSelectedValue(sel,true);
    }
    private void refreshArmyList()   {
        List<Army> prev=armyList.getSelectedValuesList();
        armyListModel.clear();
        game.getPlayer().getControlledArmies().forEach(armyListModel::addElement);
        // Restore selection
        for(int i=0;i<armyListModel.size();i++)
            if(prev.contains(armyListModel.get(i)))
                armyList.addSelectionInterval(i,i);
        refreshArmyInfo();
    }
    private void refreshArmyInfo()   {
        List<Army> sel=armyList.getSelectedValuesList();
        if(sel.isEmpty()){armyInfoLabel.setText("<html><center><i>Select armies above</i></center></html>");return;}
        if(sel.size()==1){
            Army a=sel.get(0);
            String st=switch(a.getCurrentStatus()){
                case IDLE->"⚓ IDLE at "+a.getCurrentLocation();
                case MARCHING->"🏃 → "+a.getTarget()+" ("+a.getDistancetoTarget()+" turns)";
                case BESIEGING->"⚙ BESIEGING "+a.getCurrentLocation();};
            armyInfoLabel.setText("<html><center>"+st+"<br>"+a.getUnits().size()+" units</center></html>");
        } else {
            int total=sel.stream().mapToInt(a->a.getUnits().size()).sum();
            armyInfoLabel.setText("<html><center><b>"+sel.size()+" armies selected</b><br>"+total+" total units</center></html>");
        }
    }
    private void refreshCityDetail() {
        buildingsPanel.removeAll(); armyPanel.removeAll();
        City city=cityList.getSelectedValue();
        if(city==null){cityNameLabel.setText("Select a City");cityStatusLabel.setText("");repaintPanels();return;}
        boolean owned=game.getPlayer().getControlledCities().contains(city);
        cityNameLabel.setText((owned?"🏰 ":"⚔ ")+city.getName().toUpperCase());
        cityNameLabel.setForeground(owned?new Color(75,195,95):new Color(218,75,55));
        String st=owned?"✅ Your Territory":"🔴 Enemy Territory";
        if(city.isUnderSiege()) st+="   |   ⚙ SIEGE Turn "+city.getTurnsUnderSiege()+"/3";
        cityStatusLabel.setText(st);
        if(owned){
            if(city.getEconomicalBuildings().isEmpty()&&city.getMilitaryBuildings().isEmpty())
                buildingsPanel.add(italicL("No buildings yet. Press 🏗 Build."));
            city.getEconomicalBuildings().forEach(b->buildingsPanel.add(buildingCard(b)));
            city.getMilitaryBuildings().forEach(b->buildingsPanel.add(buildingCard(b)));
        }else buildingsPanel.add(italicL("Enemy city — send an army to conquer it."));
        Army def=city.getDefendingArmy();
        if(def!=null&&!def.getUnits().isEmpty()) def.getUnits().forEach(u->armyPanel.add(unitCard(u)));
        else armyPanel.add(italicL("No defenders."));
        repaintPanels();
    }
    private void repaintPanels(){buildingsPanel.revalidate();buildingsPanel.repaint();armyPanel.revalidate();armyPanel.repaint();}

    // ── CARDS ─────────────────────────────────────────────────────
    private JPanel buildingCard(Building b){
        JPanel c=new JPanel(new BorderLayout(8,0));c.setOpaque(false);
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(75,55,18,110)),BorderFactory.createEmptyBorder(6,8,6,8)));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE,64));
        String em=switch(b.getClass().getSimpleName()){case"Farm"->"🌾";case"Market"->"💰";case"Barracks"->"⚔";case"Stable"->"🐴";case"ArcheryRange"->"🏹";default->"🏛";};
        JLabel nm=new JLabel(em+"  "+b.getClass().getSimpleName());nm.setFont(FontManager.FONT_BODY_BOLD.deriveFont(14f));nm.setForeground(new Color(218,198,138));
        String det="Level "+b.getLevel()+"/3";
        if(b instanceof EconomicBuilding eb) det+="  +"+eb.harvest()+" resources";
        if(b instanceof MilitaryBuilding mb) det+="  Recruit "+mb.getRecruitmentCost()+" 💰";
        if(b.isCoolDown()) det+="  ⏳";
        JLabel dl=new JLabel(det);dl.setFont(FontManager.FONT_SMALL.deriveFont(12f));dl.setForeground(new Color(165,150,95));
        JPanel tx=new JPanel(new GridLayout(2,1));tx.setOpaque(false);tx.add(nm);tx.add(dl);c.add(tx,BorderLayout.CENTER);
        JPanel dots=new JPanel(new FlowLayout(FlowLayout.RIGHT,2,0));dots.setOpaque(false);
        for(int i=1;i<=3;i++){JLabel d=new JLabel("●");d.setFont(new Font("Serif",Font.PLAIN,16));d.setForeground(i<=b.getLevel()?new Color(218,168,28):new Color(55,44,24));dots.add(d);}
        c.add(dots,BorderLayout.EAST);return c;
    }
    private JPanel unitCard(Unit u){
        JPanel c=new JPanel(new BorderLayout(6,0));c.setOpaque(false);
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(55,44,18,95)),BorderFactory.createEmptyBorder(4,8,4,8)));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
        String em=switch(u.getClass().getSimpleName()){case"Archer"->"🏹";case"Infantry"->"⚔";case"Cavalry"->"🐴";default->"👤";};
        JLabel nm=new JLabel(em+" "+u.getClass().getSimpleName()+"  Lv"+u.getLevel());nm.setFont(FontManager.FONT_BODY_BOLD.deriveFont(13f));nm.setForeground(new Color(208,188,128));
        int pct=u.getMaxSoldierCount()>0?(int)(100.0*u.getCurrentSoldierCount()/u.getMaxSoldierCount()):0;
        Color bc=pct>60?new Color(55,175,55):pct>30?new Color(195,158,28):new Color(195,48,48);
        JProgressBar hp=new JProgressBar(0,u.getMaxSoldierCount());hp.setValue(u.getCurrentSoldierCount());
        hp.setString(u.getCurrentSoldierCount()+"/"+u.getMaxSoldierCount());hp.setStringPainted(true);
        hp.setForeground(bc);hp.setBackground(new Color(28,18,7));hp.setFont(FontManager.FONT_SMALL.deriveFont(11f));
        hp.setBorder(BorderFactory.createLineBorder(new Color(75,55,18),1));hp.setPreferredSize(new Dimension(155,16));
        JPanel tx=new JPanel(new GridLayout(2,1,0,2));tx.setOpaque(false);tx.add(nm);tx.add(hp);c.add(tx,BorderLayout.CENTER);return c;
    }

    // ── ACTIONS ───────────────────────────────────────────────────
    private void doBuild(){
        City city=requireOwned();if(city==null)return;
        String[]opts={"Farm","Market","Barracks","Stable","ArcheryRange"};
        String ch=(String)JOptionPane.showInputDialog(this,"Choose building for "+city.getName()+":","Build",JOptionPane.PLAIN_MESSAGE,iconCastle,opts,opts[0]);
        if(ch==null)return;
        try{game.getPlayer().build(ch.toLowerCase(),city.getName());AudioManager.get().playBuild();log("✅ Built "+ch+" in "+city.getName());refreshAll();}
        catch(NotEnoughGoldException ex){sfxErr("Not enough gold to build "+ch+"!");}
    }
    private void doUpgrade(){
        City city=requireOwned();if(city==null)return;
        ArrayList<Building> all=new ArrayList<>();all.addAll(city.getEconomicalBuildings());all.addAll(city.getMilitaryBuildings());
        if(all.isEmpty()){sfxErr("No buildings to upgrade.");return;}
        String[]lbs=all.stream().map(b->b.getClass().getSimpleName()+" (Lv"+b.getLevel()+")  — "+b.getUpgradeCost()+" 💰").toArray(String[]::new);
        String ch=(String)JOptionPane.showInputDialog(this,"Upgrade which building in "+city.getName()+"?","Upgrade",JOptionPane.PLAIN_MESSAGE,iconSwords,lbs,lbs[0]);
        if(ch==null)return;
        int idx=0;for(int i=0;i<lbs.length;i++)if(lbs[i].equals(ch)){idx=i;break;}
        Building b=all.get(idx);
        try{game.getPlayer().upgradeBuilding(b);AudioManager.get().playBuild();log("⬆  "+b.getClass().getSimpleName()+" → Lv"+b.getLevel()+" in "+city.getName());refreshAll();}
        catch(NotEnoughGoldException ex){sfxErr("Not enough gold! Need "+b.getUpgradeCost()+".");}
        catch(BuildingInCoolDownException ex){sfxErr("Building in cool-down — wait one turn.");}
        catch(MaxLevelException ex){sfxErr("Already at maximum level!");}
    }
    private void doRecruit(){
        City city=requireOwned();if(city==null)return;
        if(city.getMilitaryBuildings().isEmpty()){sfxErr("No military buildings in "+city.getName()+".");return;}
        String[]types=city.getMilitaryBuildings().stream().map(b->{
            String t=b instanceof Barracks?"infantry":b instanceof Stable?"cavalry":"archer";
            return t+" ("+b.getClass().getSimpleName()+"  "+b.getRecruitmentCost()+" 💰)";
        }).toArray(String[]::new);
        String ch=(String)JOptionPane.showInputDialog(this,"Recruit unit in "+city.getName()+":","Recruit",JOptionPane.PLAIN_MESSAGE,iconSwords,types,types[0]);
        if(ch==null)return;
        try{game.getPlayer().recruitUnit(ch.split(" ")[0],city.getName());AudioManager.get().playRecruit();log("⚔  Recruited in "+city.getName());refreshAll();}
        catch(BuildingInCoolDownException ex){sfxErr("Building in cool-down — wait one turn.");}
        catch(MaxRecruitedException ex){sfxErr("Max 3 recruits per building per turn.");}
        catch(NotEnoughGoldException ex){sfxErr("Not enough gold to recruit!");}
    }
    private void doInitiateArmy(){
        City city=requireOwned();if(city==null)return;
        Army def=city.getDefendingArmy();
        if(def==null||def.getUnits().isEmpty()){sfxErr("No garrison units.");return;}
        String[]lbs=def.getUnits().stream().map(u->u.getClass().getSimpleName()+" Lv"+u.getLevel()+" ["+u.getCurrentSoldierCount()+"/"+u.getMaxSoldierCount()+"]").toArray(String[]::new);
        String ch=(String)JOptionPane.showInputDialog(this,"Select lead unit from "+city.getName()+"'s garrison:","Form Army",JOptionPane.PLAIN_MESSAGE,iconArmy,lbs,lbs[0]);
        if(ch==null)return;
        int idx=0;for(int i=0;i<lbs.length;i++)if(lbs[i].equals(ch)){idx=i;break;}
        game.getPlayer().initiateArmy(city,def.getUnits().get(idx));
        AudioManager.get().playRecruit();log("⚔  Army formed at "+city.getName());refreshAll();
    }

    /** March — applies to ALL selected armies that are not besieging. */
    private void doMarch(){
        List<Army> selected = armyList.getSelectedValuesList();
        if(selected.isEmpty()){sfxErr("Select at least one army first.");return;}
        List<Army> eligible = selected.stream()
            .filter(a->a.getCurrentStatus()!=Status.BESIEGING).toList();
        if(eligible.isEmpty()){sfxErr("All selected armies are currently besieging.");return;}

        String[]targets=game.getAvailableCities().stream()
            .map(City::getName).toArray(String[]::new);
        String ch=(String)JOptionPane.showInputDialog(this,
            "March "+eligible.size()+" army/armies to:","March",
            JOptionPane.PLAIN_MESSAGE,iconArmy,targets,targets[0]);
        if(ch==null)return;
        String dest=ch;
        int marched=0;
        for(Army a:eligible){
            if(a.getCurrentLocation().equals(dest)) continue;
            game.targetCity(a,dest); marched++;
        }
        AudioManager.get().playClick();
        log("🏃 "+marched+" army/armies marching to "+dest);
        refreshAll();
    }

    /** Lay Siege — applies to ALL selected armies IDLE at an enemy city. */
    private void doLaySiege(){
        List<Army> selected=armyList.getSelectedValuesList();
        if(selected.isEmpty()){sfxErr("Select at least one army first.");return;}
        int count=0;
        for(Army army:selected){
            City target=enemyCityAt(army.getCurrentLocation());
            if(target==null) continue;
            try{game.getPlayer().laySiege(army,target);count++;log("⚙  Siege laid on "+target.getName()+" by army at "+army.getCurrentLocation());}
            catch(TargetNotReachedException ex){log("⚠ Army not yet at "+army.getTarget());}
            catch(FriendlyCityException ex){log("⚠ Cannot siege own city.");}
        }
        if(count>0){AudioManager.get().playSiege();refreshAll();}
        else sfxErr("None of the selected armies are eligible to lay siege.\n(They must be IDLE at an enemy city.)");
    }

    /** Auto Resolve — applies to ALL selected armies at an enemy city. */
    private void doAutoResolve(){
        List<Army> selected=armyList.getSelectedValuesList();
        if(selected.isEmpty()){sfxErr("Select at least one army first.");return;}
        List<Army> eligible=selected.stream()
            .filter(a->enemyCityAt(a.getCurrentLocation())!=null).toList();
        if(eligible.isEmpty()){sfxErr("None of the selected armies are at an enemy city.");return;}

        // Build summary
        StringBuilder sb=new StringBuilder("Resolve "+eligible.size()+" battle(s):\n");
        for(Army a:eligible){
            City t=enemyCityAt(a.getCurrentLocation());
            sb.append("  • "+a.getCurrentLocation()+" — "+a.getUnits().size()+" vs "+
                (t!=null?t.getDefendingArmy().getUnits().size():0)+" defenders\n");
        }
        int ok=JOptionPane.showConfirmDialog(this,sb.toString(),"Confirm Battles",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(ok!=JOptionPane.YES_OPTION)return;

        int wins=0,losses=0;
        for(Army army:new ArrayList<>(eligible)){
            City target=enemyCityAt(army.getCurrentLocation());
            if(target==null) continue;
            try{
                game.autoResolve(army,target.getDefendingArmy());
                if(game.getPlayer().getControlledCities().contains(target)){
                    wins++;AudioManager.get().playVictory();log("🏆 VICTORY! "+target.getName()+" conquered!");
                }else{
                    losses++;AudioManager.get().playDefeat();log("💀 DEFEAT at "+target.getName()+".");
                }
            }catch(FriendlyFireException ex){log("⚠ Friendly fire avoided.");}
        }
        if(wins>0) JOptionPane.showMessageDialog(this,
            "⚔  Battle Results\n\n✅ Victories: "+wins+"\n💀 Defeats: "+losses,
            "Battle Results",JOptionPane.INFORMATION_MESSAGE,iconCastle);
        refreshAll(); checkGameOver();
    }

    private void doEndTurn(){
        game.endTurn();AudioManager.get().playEndTurn();
        log("─────── Turn "+game.getCurrentTurnCount()+" ───────");
        if(game.getPlayer().getFood()<500)    log("⚠  Food critically low!");
        if(game.getPlayer().getTreasury()<1000)log("⚠  Treasury low!");
        refreshAll(); cityMapPanel.repaint(); checkGameOver();
    }

    void checkGameOver(){
        if(!game.isGameOver()) return;
        stopAI();
        boolean won=game.getPlayer().getControlledCities().size()==game.getAvailableCities().size();
        if(won)AudioManager.get().playVictory(); else AudioManager.get().playDefeat();
        JOptionPane.showMessageDialog(this,
            won?"🏆 VICTORY!\n\nAll cities conquered! Your empire rules the ancient world."
               :"💀 DEFEAT\n\nTime has run out. The empire remains divided.",
            won?"VICTORY!":"GAME OVER",
            won?JOptionPane.INFORMATION_MESSAGE:JOptionPane.ERROR_MESSAGE);
        if(JOptionPane.showConfirmDialog(this,"Play again?","New Game",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
            new MainMenuFrame();
        AudioManager.get().shutdown(); dispose();
    }

    private void showIntro(String city){
        JOptionPane.showMessageDialog(this,new JLabel(
            "<html><body style='width:330px;font-family:serif;font-size:13px;'>"
            +"<h2 style='color:#D4AA30;'>The Year is 60 BC…</h2>"
            +"<p>Three great powers struggle for dominance of the known world.</p>"
            +"<p>You, <b style='color:#D4AA30;'>"+playerName+"</b>, rise to lead <b style='color:#80C860;'>"+city+"</b>.</p>"
            +"<p>You have <b>30 turns</b> to conquer Cairo, Rome, and Sparta.</p>"
            +"<p><b>ESC</b> = Pause Menu &nbsp;•&nbsp; <b>🗺 MAP</b> = World Map<br>"
            +"<b>💡 ADVISOR</b> = Hints &nbsp;•&nbsp; <b>🤖 AUTO-WIN</b> = AI plays for you</p>"
            +"</body></html>"),
            "The Story Begins",JOptionPane.INFORMATION_MESSAGE,iconCastle);
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private City requireOwned(){
        City c=cityList.getSelectedValue();
        if(c==null){sfxErr("Select a city first.");return null;}
        if(!game.getPlayer().getControlledCities().contains(c)){sfxErr("You don't control "+c.getName()+".");return null;}
        return c;
    }
    private City enemyCityAt(String loc){
        for(City c:game.getAvailableCities())
            if(c.getName().equals(loc)&&!game.getPlayer().getControlledCities().contains(c))return c;
        return null;
    }
    void log(String s){
        SwingUtilities.invokeLater(()->{
            logArea.append(s+"\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    private void sfxErr(String msg){AudioManager.get().playError();JOptionPane.showMessageDialog(this,msg,"Action Failed",JOptionPane.WARNING_MESSAGE);}

    // ── WIDGET FACTORIES ─────────────────────────────────────────
    private JLabel resLbl(String t){JLabel l=new JLabel(t);l.setFont(FontManager.FONT_BODY_BOLD.deriveFont(15f));l.setForeground(new Color(228,208,128));return l;}
    private JSeparator sep(){JSeparator s=new JSeparator(JSeparator.VERTICAL);s.setForeground(new Color(95,72,22,140));s.setPreferredSize(new Dimension(2,26));return s;}
    private JLabel italicL(String t){JLabel l=new JLabel("<html><i>"+t+"</i></html>");l.setFont(FontManager.FONT_ITALIC.deriveFont(13f));l.setForeground(new Color(135,122,82));l.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    private JLabel panelHead(String t,ImageIcon ic){JLabel l=new JLabel(t,ic,SwingConstants.LEFT);l.setFont(FontManager.FONT_BODY_BOLD.deriveFont(13f));l.setForeground(new Color(208,162,42));l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(155,118,38,115)),BorderFactory.createEmptyBorder(6,8,6,8)));l.setOpaque(false);return l;}
    private JPanel darkPanel(LayoutManager lm){JPanel p=new JPanel(lm){@Override protected void paintComponent(Graphics g){g.setColor(new Color(14,9,3,232));g.fillRect(0,0,getWidth(),getHeight());}};p.setOpaque(false);return p;}
    private JPanel glass(int w,int h){JPanel p=new JPanel(){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g2.setColor(new Color(11,7,2,205));g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);g2.setColor(new Color(135,98,28,95));g2.setStroke(new BasicStroke(1f));g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);}};p.setOpaque(false);if(w>0)p.setPreferredSize(new Dimension(w,h>0?h:p.getPreferredSize().height));return p;}
    private JScrollPane darkScroll(JComponent c){JScrollPane s=new JScrollPane(c);s.setOpaque(false);s.getViewport().setOpaque(false);s.setBorder(BorderFactory.createLineBorder(new Color(75,55,18,75),1));s.getVerticalScrollBar().setUnitIncrement(12);return s;}
    private JButton smlBtn(String t){JButton b=new JButton(t){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Color bg=getModel().isPressed()?new Color(55,38,5):getModel().isRollover()?new Color(95,68,14):new Color(32,22,7);g2.setColor(bg);g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);g2.setColor(new Color(155,115,32,145));g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,6,6);super.paintComponent(g);}};b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);b.setFont(FontManager.FONT_BODY_BOLD.deriveFont(13f));b.setForeground(new Color(218,192,118));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(e->AudioManager.get().playClick());return b;}
    private JButton topBtn(String t){JButton b=smlBtn(t);b.setPreferredSize(new Dimension(152,32));return b;}
    private JButton goldBtn(String t){JButton b=new JButton(t){@Override protected void paintComponent(Graphics g){Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Color top=getModel().isPressed()?new Color(95,65,8):getModel().isRollover()?new Color(192,142,22):new Color(165,118,18);Color bot=getModel().isPressed()?new Color(58,38,4):getModel().isRollover()?new Color(132,98,12):new Color(105,72,10);g2.setPaint(new GradientPaint(0,0,top,0,getHeight(),bot));g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);g2.setColor(new Color(215,168,48,175));g2.setStroke(new BasicStroke(1.5f));g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,8,8);super.paintComponent(g);}};b.setContentAreaFilled(false);b.setBorderPainted(false);b.setFocusPainted(false);b.setFont(FontManager.FONT_BODY_BOLD.deriveFont(15f));b.setForeground(new Color(255,240,178));b.setPreferredSize(new Dimension(158,36));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(e->AudioManager.get().playClick());return b;}

    // ── RENDERERS ─────────────────────────────────────────────────
    private class CityRenderer extends DefaultListCellRenderer{
        @Override public Component getListCellRendererComponent(JList<?>list,Object value,int idx,boolean sel,boolean foc){
            City city=(City)value;boolean owned=game.getPlayer().getControlledCities().contains(city);
            JPanel c=new JPanel(new BorderLayout(6,0));c.setOpaque(true);c.setBackground(sel?new Color(98,68,14,205):new Color(18,12,4,205));
            c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(55,42,14,95)),BorderFactory.createEmptyBorder(4,10,4,8)));
            JLabel nm=new JLabel((owned?"🏰":"⚔")+"  "+city.getName());nm.setFont(FontManager.FONT_BODY_BOLD.deriveFont(15f));nm.setForeground(owned?new Color(75,195,95):city.isUnderSiege()?new Color(252,158,38):new Color(215,75,55));
            JLabel sb=new JLabel(owned?"Your Territory":city.isUnderSiege()?"⚙ Under Siege":"Enemy City");sb.setFont(FontManager.FONT_SMALL.deriveFont(Font.ITALIC,11f));sb.setForeground(new Color(155,138,88));
            JPanel tx=new JPanel(new GridLayout(2,1,0,1));tx.setOpaque(false);tx.add(nm);tx.add(sb);c.add(tx,BorderLayout.CENTER);
            int def=city.getDefendingArmy()!=null?city.getDefendingArmy().getUnits().size():0;
            JLabel dl=new JLabel("🛡 "+def);dl.setFont(FontManager.FONT_BODY_BOLD.deriveFont(12f));dl.setForeground(new Color(175,158,78));c.add(dl,BorderLayout.EAST);return c;}
    }
    private class ArmyRenderer extends DefaultListCellRenderer{
        @Override public Component getListCellRendererComponent(JList<?>list,Object value,int idx,boolean sel,boolean foc){
            Army army=(Army)value;
            JPanel c=new JPanel(new BorderLayout(6,0));c.setOpaque(true);c.setBackground(sel?new Color(98,68,14,205):new Color(18,12,4,205));
            c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(55,42,14,95)),BorderFactory.createEmptyBorder(6,10,6,8)));
            String st=switch(army.getCurrentStatus()){case IDLE->"⚓";case MARCHING->"🏃";case BESIEGING->"⚙";};
            JLabel lc=new JLabel(st+"  "+army.getCurrentLocation());lc.setFont(FontManager.FONT_BODY_BOLD.deriveFont(13f));lc.setForeground(new Color(208,188,128));
            String det=army.getUnits().size()+" units";if(army.getCurrentStatus()==Status.MARCHING)det+=" → "+army.getTarget()+" ("+army.getDistancetoTarget()+" turns)";
            JLabel dl=new JLabel(det);dl.setFont(FontManager.FONT_SMALL.deriveFont(Font.ITALIC,11f));dl.setForeground(new Color(148,132,82));
            JPanel tx=new JPanel(new GridLayout(2,1,0,1));tx.setOpaque(false);tx.add(lc);tx.add(dl);c.add(tx,BorderLayout.CENTER);return c;}
    }
}
