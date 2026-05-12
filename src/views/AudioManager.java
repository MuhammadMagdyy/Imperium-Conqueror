package views;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Plays real WAV files from the classpath (audio/ folder inside the JAR).
 * If a file is missing or unreadable, falls back to synthesized PCM / MIDI.
 * Handles mixed formats (24-bit, 24kHz, stereo) by converting to standard PCM.
 */
public class AudioManager {

    private static AudioManager instance;
    public  static AudioManager get() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    private boolean musicEnabled = true;
    private boolean sfxEnabled   = true;

    private Clip           bgClip;
    private Sequencer      sequencer;
    private Synthesizer    synth;
    private SourceDataLine sfxLine;

    // Standard format we convert everything to before playing
    private static final AudioFormat STD =
        new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);

    private AudioManager() {
        initSfxLine();
        startMusic();
    }

    // ── Public SFX ───────────────────────────────────────────────
    public void playClick()   { sfx("audio/sfx_click.wav",   this::genClick);   }
    public void playBuild()   { sfx("audio/sfx_build.wav",   this::genBuild);   }
    public void playRecruit() { sfx("audio/sfx_recruit.wav", this::genRecruit); }
    public void playSiege()   { sfx("audio/sfx_siege.wav",   this::genSiege);   }
    public void playVictory() { sfx("audio/sfx_victory.wav", this::genVictory); }
    public void playDefeat()  { sfx("audio/sfx_defeat.wav",  this::genDefeat);  }
    public void playEndTurn() { sfx("audio/sfx_endturn.wav", this::genEndTurn); }
    public void playError()   { sfx("audio/sfx_error.wav",   this::genError);   }

    public void toggleMusic() {
        musicEnabled = !musicEnabled;
        if (bgClip != null) {
            if (!musicEnabled) bgClip.stop();
            else               { bgClip.setFramePosition(0); bgClip.loop(Clip.LOOP_CONTINUOUSLY); }
        } else if (sequencer != null) {
            if (!musicEnabled) sequencer.stop();
            else               { sequencer.setTickPosition(0); sequencer.start(); }
        }
    }
    public void toggleSfx()         { sfxEnabled = !sfxEnabled; }
    public boolean isMusicEnabled() { return musicEnabled; }
    public boolean isSfxEnabled()   { return sfxEnabled;   }

    public void shutdown() {
        try { if (bgClip    != null) bgClip.close();    } catch (Exception ignored) {}
        try { if (sequencer != null) sequencer.close(); } catch (Exception ignored) {}
        try { if (synth     != null) synth.close();     } catch (Exception ignored) {}
        try { if (sfxLine   != null) sfxLine.close();   } catch (Exception ignored) {}
    }

    // ── BACKGROUND MUSIC ─────────────────────────────────────────
    private void startMusic() {
        if (tryWavMusic("audio/music_background.wav")) return;
        buildMidi();
    }

    private boolean tryWavMusic(String res) {
        try {
            InputStream raw = stream(res);
            if (raw == null) return false;
            AudioInputStream src = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
            AudioInputStream std = toStd(src);
            bgClip = AudioSystem.getClip();
            bgClip.open(std);
            setGain(bgClip, 0.5f);
            bgClip.loop(Clip.LOOP_CONTINUOUSLY);
            return true;
        } catch (Exception e) { return false; }
    }

    private AudioInputStream toStd(AudioInputStream src) {
        AudioFormat f = src.getFormat();
        // Already standard?
        if (f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && f.getSampleRate() == 44100 && f.getSampleSizeInBits() == 16) return src;
        // First decode compressed (MP3 etc.) to PCM if needed
        AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
            f.getSampleRate(), 16, f.getChannels(),
            f.getChannels() * 2, f.getSampleRate(), false);
        AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, src);
        // Then resample / re-channel to STD
        if (!AudioSystem.isConversionSupported(STD, pcm)) return decoded;
        return AudioSystem.getAudioInputStream(STD, decoded);
    }

    private void setGain(Clip c, float linear) {
        try {
            FloatControl fc = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float)(20.0 * Math.log10(Math.max(linear, 0.0001)));
            fc.setValue(Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), db)));
        } catch (Exception ignored) {}
    }

    // ── SFX DISPATCH ─────────────────────────────────────────────
    private interface Gen { byte[] get(); }

    private void sfx(String res, Gen fallback) {
        if (!sfxEnabled) return;
        new Thread(() -> {
            if (!tryWavSfx(res)) playPcm(fallback.get());
        }, "sfx").start();
    }

    private boolean tryWavSfx(String res) {
        try {
            InputStream raw = stream(res);
            if (raw == null) return false;
            AudioInputStream src = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
            AudioInputStream std = toStd(src);
            Clip clip = AudioSystem.getClip();
            clip.open(std);
            setGain(clip, 0.85f);
            clip.start();
            Thread.sleep(clip.getMicrosecondLength() / 1000 + 80);
            clip.close();
            return true;
        } catch (Exception e) { return false; }
    }

    private InputStream stream(String res) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(res);
        if (is != null) return is;
        try { File f = new File(res); if (f.exists()) return new FileInputStream(f); }
        catch (Exception ignored) {}
        return null;
    }

    // ── PCM FALLBACK ─────────────────────────────────────────────
    private void initSfxLine() {
        try {
            AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            sfxLine = (SourceDataLine) AudioSystem.getLine(info);
            sfxLine.open(fmt, 44100 / 10);
            sfxLine.start();
        } catch (Exception ignored) {}
    }

    private void playPcm(byte[] pcm) {
        if (sfxLine == null || pcm == null) return;
        sfxLine.write(pcm, 0, pcm.length);
    }

    private static final int SR = 44100;
    private ByteBuffer buf(int n) { return ByteBuffer.allocate(n*2).order(ByteOrder.LITTLE_ENDIAN); }

    private byte[] genClick() {
        int n=(int)(SR*.07f); ByteBuffer b=buf(n);
        for(int i=0;i<n;i++){double t=i/(double)SR;
            b.putShort((short)(Math.exp(-t*65)*Math.sin(2*Math.PI*900*t)*28000));}
        return b.array();}

    private byte[] genBuild() {
        int n=(int)(SR*.28f); ByteBuffer b=buf(n);
        for(int i=0;i<n;i++){double t=i/(double)SR; double e=Math.exp(-t*16);
            b.putShort((short)(e*(Math.sin(2*Math.PI*110*t)+.5*Math.sin(2*Math.PI*75*t)+.4*(Math.random()*2-1))*21000));}
        return b.array();}

    private byte[] genRecruit() {
        int n=(int)(SR*.20f); ByteBuffer b=buf(n);
        for(int i=0;i<n;i++){double t=i/(double)SR;
            b.putShort((short)(Math.exp(-t*13)*Math.sin(2*Math.PI*(420+280*Math.exp(-t*22))*t)*26000));}
        return b.array();}

    private byte[] genSiege() {
        int n=(int)(SR*.55f); ByteBuffer b=buf(n); double ph=0;
        for(int i=0;i<n;i++){double t=i/(double)SR;
            double e=t<.1?t/.1:Math.exp(-(t-.1)*4.5);
            ph+=2*Math.PI*(58+18*Math.sin(2*Math.PI*2.8*t))/SR;
            b.putShort((short)(e*(Math.sin(ph)+.28*(Math.random()*2-1))*20000));}
        return b.array();}

    private byte[] genVictory() {
        int[]nn={392,440,523,659,784}; int d=(int)(SR*.16f);
        ByteBuffer b=ByteBuffer.allocate(nn.length*d*2).order(ByteOrder.LITTLE_ENDIAN);
        for(int ni=0;ni<nn.length;ni++){double f=nn[ni];
            for(int i=0;i<d;i++){double t=i/(double)SR;
                double e=i<d*.08?i/(d*.08):Math.exp(-(t-.013)*7);
                b.putShort((short)(e*(Math.sin(2*Math.PI*f*t)+.3*Math.sin(2*Math.PI*f*2*t))*24000));}}
        return b.array();}

    private byte[] genDefeat() {
        int[]nn={440,392,349,294}; int d=(int)(SR*.24f);
        ByteBuffer b=ByteBuffer.allocate(nn.length*d*2).order(ByteOrder.LITTLE_ENDIAN);
        for(int ni=0;ni<nn.length;ni++){double f=nn[ni];
            for(int i=0;i<d;i++){double t=i/(double)SR;
                b.putShort((short)(Math.exp(-t*4.5)*Math.sin(2*Math.PI*f*t)*22000));}}
        return b.array();}

    private byte[] genEndTurn() {
        int n=(int)(SR*.40f); ByteBuffer b=buf(n); double f=660;
        for(int i=0;i<n;i++){double t=i/(double)SR; double e=Math.exp(-t*5.5);
            b.putShort((short)(e*(Math.sin(2*Math.PI*f*t)+.2*Math.sin(2*Math.PI*f*2*t)+.1*Math.sin(2*Math.PI*f*3*t))*26000));}
        return b.array();}

    private byte[] genError() {
        int n=(int)(SR*.22f); ByteBuffer b=buf(n);
        for(int i=0;i<n;i++){double t=i/(double)SR;
            b.putShort((short)(Math.exp(-t*9)*Math.sin(2*Math.PI*175*t)*(1+.5*Math.sin(2*Math.PI*22*t))*22000));}
        return b.array();}

    // ── MIDI FALLBACK ─────────────────────────────────────────────
    private void buildMidi() {
        try {
            synth=MidiSystem.getSynthesizer(); synth.open();
            sequencer=MidiSystem.getSequencer(false); sequencer.open();
            sequencer.getTransmitter().setReceiver(synth.getReceiver());
            Sequence seq=new Sequence(Sequence.PPQ,480);
            addTempo(seq,65); bldMel(seq); bldHarm(seq); bldBass(seq); bldPerc(seq);
            sequencer.setSequence(seq);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            mVol(.55f);
            if(musicEnabled) sequencer.start();
        } catch(Exception ignored){}
    }
    private void addTempo(Sequence s,int bpm) throws Exception {
        Track t=s.createTrack(); int us=60_000_000/bpm;
        MetaMessage m=new MetaMessage();
        m.setMessage(0x51,new byte[]{(byte)(us>>16),(byte)(us>>8),(byte)us},3);
        t.add(new MidiEvent(m,0));}
    private void bldMel(Sequence s) throws Exception {
        Track t=s.createTrack(); int ch=0; pg(t,ch,0,48);
        int Q=480,H=960;
        int[][]p1={{62,H},{64,Q},{65,Q},{67,H},{69,Q},{67,Q},{65,H},{64,Q},{62,Q},{60,H*2},
                   {62,Q},{64,Q},{65,H},{67,Q},{69,Q},{67,H},{65,Q},{64,Q},{62,Q},{60,Q},{62,H*2}};
        int[][]p2={{69,H},{67,Q},{65,Q},{64,H},{62,Q},{64,Q},{65,H},{67,Q},{65,Q},{64,H*2},
                   {62,Q},{65,Q},{67,H},{69,H},{67,Q},{65,Q},{64,Q},{62,Q},{60,Q},{62,Q},{62,H*2}};
        long tick=0;
        for(int p=0;p<2;p++) for(int[]n:(p==0?p1:p2)){on(t,ch,n[0],72,tick);of(t,ch,n[0],tick+n[1]-20);tick+=n[1];}
    }
    private void bldHarm(Sequence s) throws Exception {
        Track t=s.createTrack(); int ch=1; pg(t,ch,0,19); int W=1920;
        int[][]c2={{62,65,69},{57,60,64},{53,57,60},{48,52,55},{62,65,69},{57,60,64},{55,58,62},{57,61,64}};
        long tick=0;
        for(int r=0;r<4;r++) for(int[]c:c2){for(int n:c){on(t,ch,n,42,tick);of(t,ch,n,tick+W-20);}tick+=W;}
    }
    private void bldBass(Sequence s) throws Exception {
        Track t=s.createTrack(); int ch=2; pg(t,ch,0,43); int H=960;
        int[]b={38,33,29,36,38,33,31,33}; long tick=0;
        for(int r=0;r<4;r++) for(int n:b){on(t,ch,n,68,tick);of(t,ch,n,tick+H-30);tick+=H;
            on(t,ch,n,58,tick);of(t,ch,n,tick+H-30);tick+=H;}
    }
    private void bldPerc(Sequence s) throws Exception {
        Track t=s.createTrack(); int ch=9,E=240; int[]pat={41,-1,-1,54,41,-1,41,54}; long tick=0;
        for(int bar=0;bar<32;bar++) for(int step=0;step<8;step++){
            if(pat[step]!=-1){on(t,ch,pat[step],52,tick);of(t,ch,pat[step],tick+E-10);}tick+=E;}
    }
    private void pg(Track t,int ch,long tk,int pc) throws Exception {
        ShortMessage m=new ShortMessage(); m.setMessage(ShortMessage.PROGRAM_CHANGE,ch,pc,0); t.add(new MidiEvent(m,tk));}
    private void on(Track t,int ch,int n,int v,long tk) throws Exception {
        ShortMessage m=new ShortMessage(); m.setMessage(ShortMessage.NOTE_ON,ch,n,v); t.add(new MidiEvent(m,tk));}
    private void of(Track t,int ch,int n,long tk) throws Exception {
        ShortMessage m=new ShortMessage(); m.setMessage(ShortMessage.NOTE_OFF,ch,n,0); t.add(new MidiEvent(m,tk));}
    private void mVol(float v) {
        if(synth==null) return; int vol=(int)(v*127);
        for(MidiChannel c:synth.getChannels()) if(c!=null) c.controlChange(7,vol);}
}
