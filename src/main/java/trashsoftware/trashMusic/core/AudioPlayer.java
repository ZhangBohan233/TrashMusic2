package trashsoftware.trashMusic.core;

import trashsoftware.trashMusic.core.wav.WavFile;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayer {

    private static final long CHUNK_MS = 100;

    private final WavFile wavFile;
    private int framePos;
    private boolean playing = false;

    private final byte[] buffer;
    private final int bytesPerFrame;

    private final AudioInputStream audioInputStream;
    private final SourceDataLine dataLine;
    private final Runnable runAfterFinished;

    public AudioPlayer(File audioFile, Runnable runAfterFinished) {
        this.runAfterFinished = runAfterFinished;
        try {
            this.wavFile = WavFile.fromFile(audioFile, false);
            audioInputStream = AudioSystem.getAudioInputStream(wavFile.getFile());
            AudioFormat audioFormat = audioInputStream.getFormat();
            buffer = new byte[(int) (wavFile.getByteRate() / 1000 * CHUNK_MS)];
            bytesPerFrame = wavFile.getBitsPerSample() / 8 * wavFile.getNumChannels();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            dataLine = (SourceDataLine) AudioSystem.getLine(info);
            dataLine.open();
            dataLine.start();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void setStartPos(double posSecond) throws IOException {
        framePos = (int) (posSecond * wavFile.getSampleRate());
        long skipBytes = (long) framePos * bytesPerFrame;
        long skipped = audioInputStream.skip(skipBytes);
        if (skipped != skipBytes) throw new IOException();
    }

    public void play() throws IOException {
        playing = true;
        int read;
        while (playing && (read = audioInputStream.read(buffer)) > 0) {
            dataLine.write(buffer, 0, read);
            framePos += read / bytesPerFrame;
        }
        System.out.println(framePos + " " + wavFile.getNumFrames());
        if (framePos == wavFile.getNumFrames()) {  // 播放正常结束
            terminate();
        }
    }

    public void pause() {
        playing = false;
    }

    public void terminate() throws IOException {
        playing = false;
        close();
        if (runAfterFinished != null)
            runAfterFinished.run();
    }

    public void close() throws IOException {
        audioInputStream.close();
        dataLine.drain();
        dataLine.close();
    }
}
