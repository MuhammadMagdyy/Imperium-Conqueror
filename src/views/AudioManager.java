package views;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Self-contained audio engine — no external audio files required.
 *
 *  • Background music: a procedurally composed medieval MIDI piece in D-Dorian.
 *    Layered strings, organ, bass, and light percussion. Loops forever.
 *  • Sound effects: synthesized pure-PCM tones shaped into realistic sounds.
 */
public class AudioManager {

    private boolean isHighTension = false;

    // ── Singleton ────────────────────────────────────────────────────
    private static AudioManager instance;
    public  static AudioManager get() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    // ── MIDI ─────────────────────────────────────────────────────────
    private Sequencer  sequencer;
    private Synthesizer synth;
    private float musicVolume = 0.55f;
    private boolean musicEnabled = true;
    private boolean sfxEnabled   = true;

    // ── SFX channel ──────────────────────────────────────────────────
    private SourceDataLine sfxLine;

    private AudioManager() {
        initSfxLine();
        buildAndPlayMusic();
    }

    // ─────────────────────────────────────────────────────────────────
    //  PUBLIC INTERFACE
    // ─────────────────────────────────────────────────────────────────

    public void playClick()   { playSfxAsync(genClick()); }
    public void playBuild()   { playSfxAsync(genBuild()); }
    public void playRecruit() { playSfxAsync(genRecruit()); }
    public void playSiege()   { playSfxAsync(genSiege()); }
    public void playVictory() { playSfxAsync(genVictory()); }
    public void playDefeat()  { playSfxAsync(genDefeat()); }
    public void playEndTurn() { playSfxAsync(genEndTurn()); }
    public void playError()   { playSfxAsync(genError()); }

    public void setMusicVolume(float v) {
        musicVolume = Math.max(0, Math.min(1, v));
        applyMidiVolume();
    }

    public void toggleMusic() {
        musicEnabled = !musicEnabled;
        if (sequencer == null) return;
        if (!musicEnabled) sequencer.stop();
        else { sequencer.setTickPosition(0); sequencer.start(); }
    }

    public void toggleSfx()  { sfxEnabled = !sfxEnabled; }
    public boolean isMusicEnabled() { return musicEnabled; }
    public boolean isSfxEnabled()   { return sfxEnabled;   }

    public void shutdown() {
        if (sequencer  != null && sequencer.isOpen())  sequencer.close();
        if (synth      != null && synth.isOpen())      synth.close();
        if (sfxLine    != null && sfxLine.isOpen())    sfxLine.close();
    }

    // ─────────────────────────────────────────────────────────────────
    //  MIDI MUSIC — D-Dorian medieval composition
    // ─────────────────────────────────────────────────────────────────

    private void buildAndPlayMusic() {
        try {
            synth     = MidiSystem.getSynthesizer();
            synth.open();
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.getTransmitter().setReceiver(synth.getReceiver());

            Sequence seq = new Sequence(Sequence.PPQ, 480);
            int tempo = 65; // BPM — slow and stately

            addTempoEvent(seq, tempo);
            buildMelodyTrack(seq);
            buildHarmonyTrack(seq);
            buildBassTrack(seq);
            buildPercTrack(seq);

            sequencer.setSequence(seq);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            applyMidiVolume();
            if (musicEnabled) sequencer.start();

        } catch (Exception e) {
            // MIDI unavailable in headless/server environments — silent fail
        }
    }



    public void setTensionMode(boolean high) {
        if (this.isHighTension == high) return; // No change
        this.isHighTension = high;

        if (sequencer != null && sequencer.isRunning()) {
            // 1. Change Tempo (85 BPM for War, 65 BPM for Peace)
            float factor = high ? 1.3f : 1.0f;
            sequencer.setTempoFactor(factor);

            // 2. Swap Instruments on the fly
            try {
                Track[] tracks = sequencer.getSequence().getTracks();
                // Channel 0 was Strings (48). Let's make it Brass (61) for war.
                int patch = high ? 61 : 48;

                // Send an immediate Program Change to the synth
                synth.getChannels()[0].programChange(patch);
            } catch (Exception e) { /* Silent fail */ }
        }
    }
    private void addTempoEvent(Sequence seq, int bpm) throws Exception {
        Track t = seq.createTrack();
        int us = 60_000_000 / bpm;
        MetaMessage mm = new MetaMessage();
        byte[] data = { (byte)(us >> 16), (byte)(us >> 8), (byte) us };
        mm.setMessage(0x51, data, 3);
        t.add(new MidiEvent(mm, 0));
    }

    // ----- MELODY: String Ensemble (patch 48), channel 0 -----
    private void buildMelodyTrack(Sequence seq) throws Exception {
        Track t = seq.createTrack();
        int ch = 0;
        programChange(t, ch, 0, 48); // String Ensemble 1

        int PPQ = 480;
        int H = PPQ * 2; // half note
        int Q = PPQ;     // quarter
        int E = PPQ / 2; // eighth

        // D-Dorian melody — 16 bars, repeats within the 32-bar loop
        int[][] phrase1 = {
            {62,H},{64,Q},{65,Q},  // D4 E4 F4 G4
            {67,H},{69,Q},{67,Q},  // G4 A4 G4
            {65,H},{64,Q},{62,Q},  // F4 E4 D4
            {60,H*2},              // C4 (long)
            {62,Q},{64,Q},{65,H},  // D4 E4 F4
            {67,Q},{69,Q},{67,H},  // G4 A4 G4
            {65,Q},{64,Q},{62,Q},{60,Q}, // F4 E4 D4 C4
            {62,H*2},              // D4 (long)
        };
        int[][] phrase2 = {
            {69,H},{67,Q},{65,Q},  // A4 G4 F4
            {64,H},{62,Q},{64,Q},  // E4 D4 E4
            {65,H},{67,Q},{65,Q},  // F4 G4 F4
            {64,H*2},              // E4 (long)
            {62,Q},{65,Q},{67,H},  // D4 F4 G4
            {69,H},{67,Q},{65,Q},  // A4 G4 F4
            {64,Q},{62,Q},{60,Q},{62,Q},
            {62,H*2},
        };

        long tick = 0;
        for (int pass = 0; pass < 2; pass++) {
            int[][] phrase = pass == 0 ? phrase1 : phrase2;
            for (int[] note : phrase) {
                noteOn (t, ch, note[0], 75, tick);
                noteOff(t, ch, note[0],     tick + note[1] - 20);
                tick += note[1];
            }
        }
    }

    // ----- HARMONY: Church Organ (patch 19), channel 1 -----
    private void buildHarmonyTrack(Sequence seq) throws Exception {
        Track t = seq.createTrack();
        int ch = 1;
        programChange(t, ch, 0, 19); // Church Organ

        int PPQ = 480;
        int W = PPQ * 4; // whole note

        // Slow chord changes — Dm Am F C Dm Am Gm A
        int[][] chords = {
            {62,65,69}, // Dm
            {57,60,64}, // Am
            {53,57,60}, // F
            {48,52,55}, // C
            {62,65,69}, // Dm
            {57,60,64}, // Am
            {55,58,62}, // Gm
            {57,61,64}, // A
        };

        long tick = 0;
        for (int rep = 0; rep < 4; rep++) {
            for (int[] chord : chords) {
                for (int note : chord) {
                    noteOn (t, ch, note, 45, tick);
                    noteOff(t, ch, note,     tick + W - 20);
                }
                tick += W;
            }
        }
    }

    // ----- BASS: Contrabass (patch 43), channel 2 -----
    private void buildBassTrack(Sequence seq) throws Exception {
        Track t = seq.createTrack();
        int ch = 2;
        programChange(t, ch, 0, 43); // Contrabass

        int PPQ = 480;
        int H = PPQ * 2;

        int[] bassNotes = {38, 33, 29, 36,  38, 33, 31, 33}; // D A F C (octave lower)

        long tick = 0;
        for (int rep = 0; rep < 4; rep++) {
            for (int note : bassNotes) {
                noteOn (t, ch, note, 70, tick);
                noteOff(t, ch, note,     tick + H - 30);
                tick += H;
                noteOn (t, ch, note, 60, tick);
                noteOff(t, ch, note,     tick + H - 30);
                tick += H;
            }
        }
    }

    // ----- PERCUSSION: channel 9, frame drum + tambourine -----
    private void buildPercTrack(Sequence seq) throws Exception {
        Track t = seq.createTrack();
        int ch = 9;
        int PPQ = 480;
        int Q = PPQ;
        int E = PPQ / 2;

        // Frame drum (41 = low floor tom), tambourine (54)
        // Pattern: |kick . . tamb | kick . kick tamb|
        int[] pattern = {41, -1, -1, 54,  41, -1, 41, 54};
        int totalBars = 32;

        long tick = 0;
        for (int bar = 0; bar < totalBars; bar++) {
            for (int step = 0; step < 8; step++) {
                if (pattern[step] != -1) {
                    noteOn (t, ch, pattern[step], 55, tick);
                    noteOff(t, ch, pattern[step],     tick + E - 10);
                }
                tick += E;
            }
        }
    }

    // ----- MIDI helpers -----
    private void programChange(Track t, int ch, long tick, int patch) throws Exception {
        ShortMessage sm = new ShortMessage();
        sm.setMessage(ShortMessage.PROGRAM_CHANGE, ch, patch, 0);
        t.add(new MidiEvent(sm, tick));
    }

    private void noteOn(Track t, int ch, int note, int vel, long tick) throws Exception {
        ShortMessage sm = new ShortMessage();
        sm.setMessage(ShortMessage.NOTE_ON, ch, note, vel);
        t.add(new MidiEvent(sm, tick));
    }

    private void noteOff(Track t, int ch, int note, long tick) throws Exception {
        ShortMessage sm = new ShortMessage();
        sm.setMessage(ShortMessage.NOTE_OFF, ch, note, 0);
        t.add(new MidiEvent(sm, tick));
    }

    private void applyMidiVolume() {
        if (synth == null) return;
        int vol = (int)(musicVolume * 127);
        for (MidiChannel ch : synth.getChannels()) {
            if (ch != null) ch.controlChange(7, vol);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  PCM SOUND EFFECTS (synthesized — no files needed)
    // ─────────────────────────────────────────────────────────────────

    private static final int SR   = 44100; // sample rate
    private static final int BITS = 16;
    private static final int CH   = 1;

    private void initSfxLine() {
        try {
            AudioFormat fmt = new AudioFormat(SR, BITS, CH, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            sfxLine = (SourceDataLine) AudioSystem.getLine(info);
            sfxLine.open(fmt, SR / 10);
            sfxLine.start();
        } catch (Exception ignored) {}
    }

    private void playSfxAsync(byte[] pcm) {
        if (!sfxEnabled || sfxLine == null || pcm == null) return;
        new Thread(() -> sfxLine.write(pcm, 0, pcm.length), "sfx").start();
    }

    /** Short woody click */
    private byte[] genClick() {
        return addEnvelope(genTone(800, 0.06f, "click"), 0.001f, 0.06f);
    }

    /** Stone-on-stone thud — building placed */
    private byte[] genBuild() {
        int n = (int)(SR * 0.25f);
        byte[] buf = new byte[n * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            double t  = i / (double) SR;
            double env = Math.exp(-t * 18);
            double v = env * (Math.sin(2 * Math.PI * 120 * t)
                             + 0.5 * Math.sin(2 * Math.PI * 80  * t)
                             + 0.4 * (Math.random() * 2 - 1));
            bb.putShort((short)(v * 22000));
        }
        return buf;
    }

    /** Metal scrape — unit recruited */
    private byte[] genRecruit() {
        int n = (int)(SR * 0.18f);
        byte[] buf = new byte[n * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            double t   = i / (double) SR;
            double env = Math.exp(-t * 14);
            double freq = 400 + 300 * Math.exp(-t * 20);
            double v = env * Math.sin(2 * Math.PI * freq * t);
            bb.putShort((short)(v * 26000));
        }
        return buf;
    }

    /** Low ominous rumble — siege */
    private byte[] genSiege() {
        int n = (int)(SR * 0.5f);
        byte[] buf = new byte[n * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t   = i / (double) SR;
            double env = t < 0.1 ? t / 0.1 : Math.exp(-(t - 0.1) * 5);
            double freq = 55 + 20 * Math.sin(2 * Math.PI * 3 * t);
            phase += 2 * Math.PI * freq / SR;
            double v = env * (Math.sin(phase) + 0.3 * (Math.random() * 2 - 1));
            bb.putShort((short)(v * 20000));
        }
        return buf;
    }

    /** Triumphant fanfare — victory */
    private byte[] genVictory() {
        int[] notes = {392, 440, 523, 659, 784}; // G A C E G
        int dur = (int)(SR * 0.15f);
        byte[] buf = new byte[notes.length * dur * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        for (int ni = 0; ni < notes.length; ni++) {
            double f = notes[ni];
            for (int i = 0; i < dur; i++) {
                double t   = i / (double) SR;
                double env = i < dur * 0.1 ? i / (dur * 0.1)
                           : Math.exp(-(t - 0.015) * 8);
                double v = env * (Math.sin(2 * Math.PI * f * t)
                                + 0.3 * Math.sin(2 * Math.PI * f * 2 * t));
                bb.putShort((short)(v * 24000));
            }
        }
        return buf;
    }

    /** Descending minor — defeat */
    private byte[] genDefeat() {
        int[] notes = {440, 392, 349, 294}; // A G F D
        int dur = (int)(SR * 0.22f);
        byte[] buf = new byte[notes.length * dur * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        for (int ni = 0; ni < notes.length; ni++) {
            double f = notes[ni];
            for (int i = 0; i < dur; i++) {
                double t   = i / (double) SR;
                double env = Math.exp(-t * 5);
                double v = env * Math.sin(2 * Math.PI * f * t);
                bb.putShort((short)(v * 22000));
            }
        }
        return buf;
    }

    /** Bell chime — end turn */
    private byte[] genEndTurn() {
        return genChime(660, 0.4f);
    }

    /** Buzzy low tone — error */
    private byte[] genError() {
        int n = (int)(SR * 0.2f);
        byte[] buf = new byte[n * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            double t   = i / (double) SR;
            double env = Math.exp(-t * 10);
            double v = env * Math.sin(2 * Math.PI * 180 * t)
                     * (1 + 0.5 * Math.sin(2 * Math.PI * 20 * t));
            bb.putShort((short)(v * 22000));
        }
        return buf;
    }

    // ── Low-level helpers ────────────────────────────────────────────

    private byte[] genTone(double freq, float seconds, String shape) {
        int n = (int)(SR * seconds);
        byte[] buf = new byte[n * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            double t   = i / (double) SR;
            double env = shape.equals("click") ? Math.exp(-t * 60) : Math.exp(-t * 8);
            bb.putShort((short)(env * Math.sin(2 * Math.PI * freq * t) * 28000));
        }
        return buf;
    }

    private byte[] genChime(double freq, float seconds) {
        int n = (int)(SR * seconds);
        byte[] buf = new byte[n * 2];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n; i++) {
            double t   = i / (double) SR;
            double env = Math.exp(-t * 6);
            double v = env * (Math.sin(2 * Math.PI * freq * t)
                            + 0.2 * Math.sin(2 * Math.PI * freq * 2 * t)
                            + 0.1 * Math.sin(2 * Math.PI * freq * 3 * t));
            bb.putShort((short)(v * 26000));
        }
        return buf;
    }

    private byte[] addEnvelope(byte[] raw, float attack, float total) {
        ByteBuffer bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer out = ByteBuffer.allocate(raw.length).order(ByteOrder.LITTLE_ENDIAN);
        int n = raw.length / 2;
        int attackSamples = (int)(SR * attack);
        for (int i = 0; i < n; i++) {
            short s = bb.getShort();
            double gain = i < attackSamples ? (double) i / attackSamples : 1.0;
            out.putShort((short)(s * gain));
        }
        return out.array();
    }
}
