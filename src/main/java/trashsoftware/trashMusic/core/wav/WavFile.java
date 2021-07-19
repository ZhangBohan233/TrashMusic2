package trashsoftware.trashMusic.core.wav;

import trashsoftware.trashMusic.core.eq.Equalizer;
import trashsoftware.trashMusic.core.eq.Overtone;
import trashsoftware.trashMusic.util.Util;

import java.io.*;
import java.util.Collections;
import java.util.Map;

public class WavFile {

    public static final int DEFAULT_SAMPLE_RATE = 22050;

    private final File file;
    private int[][] data;
    private int numFrames;
    private int numChannels;
    private int audioFormat;
    private int bitsPerSample;
    private int blockAlign;
    private long sampleRate;
    private long byteRate;
    private Util.IntList[] writeBuffer;

    private WavFile(File file) {
        this.file = file;
    }

    public static WavFile createNew(File outFile, long sampleRate, int numChannels) {
        WavFile wavFile = new WavFile(outFile);
        wavFile.sampleRate = sampleRate;
        wavFile.bitsPerSample = 16;
        wavFile.numChannels = numChannels;
        wavFile.byteRate = (long) numChannels * sampleRate * wavFile.bitsPerSample / 8;
        wavFile.blockAlign = numChannels * wavFile.bitsPerSample / 8;
        wavFile.audioFormat = 1;
        wavFile.writeBuffer = new Util.IntList[numChannels];
        for (int i = 0; i < numChannels; ++i) {
            wavFile.writeBuffer[i] = new Util.IntList();
        }
        return wavFile;
    }

    public static WavFile fromFile(File file) throws IOException {
        return fromFile(file, true);
    }

    public static WavFile fromFile(File file, boolean readData) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            WavFile wavFile = new WavFile(file);
            String chunkDescriptor = Util.readString(bis, 4);
            if (!chunkDescriptor.endsWith("RIFF")) {
                throw new IllegalArgumentException("Not RIFF");
            }
            long chunkSize = Util.readInt4Little(bis);
            String fmtFlag = Util.readString(bis, 4);
            if (!fmtFlag.endsWith("WAVE")) {
                throw new IllegalArgumentException("Not WAVE");
            }
            String subChunk1Id = Util.readString(bis, 4);
            if (!subChunk1Id.endsWith("fmt ")) {
                throw new IllegalArgumentException("Not fmt");
            }
            long subChunk1Size = Util.readInt4Little(bis);
            wavFile.audioFormat = Util.readInt2Little(bis);
            wavFile.numChannels = Util.readInt2Little(bis);
            wavFile.sampleRate = Util.readInt4Little(bis);
            wavFile.byteRate = Util.readInt4Little(bis);
            wavFile.blockAlign = Util.readInt2Little(bis);
            wavFile.bitsPerSample = Util.readInt2Little(bis);

            String subChunk2Id;
            long subChunk2Size;
            while (!(subChunk2Id = Util.readString(bis, 4)).endsWith("data")) {
                subChunk2Size = Util.readInt4Little(bis);
                System.out.println(subChunk2Id + ": " + subChunk2Size);
                if (bis.skip(subChunk2Size) != subChunk2Size) {
                    throw new IOException();
                }
            }
            subChunk2Size = Util.readInt4Little(bis);  // data chunk size
            if (subChunk2Size == 0) throw new IllegalArgumentException("Empty data chunk");

            wavFile.numFrames = (int) (subChunk2Size / (wavFile.bitsPerSample / 8) / wavFile.numChannels);
//            System.out.println(subChunk2Id + subChunk2Size);

            if (readData) {
                wavFile.data = new int[wavFile.numChannels][wavFile.numFrames];
                for (int f = 0; f < wavFile.numFrames; ++f) {
                    for (int c = 0; c < wavFile.numChannels; ++c) {
                        if (wavFile.bitsPerSample == 8) {
                            wavFile.data[c][f] = bis.read();
                        } else if (wavFile.bitsPerSample == 16) {
                            wavFile.data[c][f] = Util.readInt2Little(bis);
                        }
                    }
                }
            }
            return wavFile;
        }
    }

    public boolean hasData() {
        return data != null;
    }

    public File getFile() {
        return file;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public long getByteRate() {
        return byteRate;
    }

    public double getLengthSeconds() {
        return (double) numFrames / sampleRate;
    }

    /**
     * 如果是新建的wav，必须在{@link WavFile#flushBuffer()}后使用。
     *
     * @param channelIndex 声道号，从0开始
     * @return 数据
     */
    public int[] getChannel(int channelIndex) {
        return data[channelIndex];
    }

    public void setData(int[] data, int channelIndex) {
        if (this.data == null) {
            this.data = new int[numChannels][];
            this.data[channelIndex] = data;
        }
    }

    public double putFlatFreq(double freq, double durationMs, double volumePercentage) {
        return putFlatFreq(freq, durationMs, volumePercentage, 0, Overtone.PLAIN, Equalizer.PLAIN);
    }

    public double putFlatFreq(double freq, double durationMs, double volumePercentage,
                            Overtone overtone, Equalizer equalizer) {
        return putFlatFreq(freq, durationMs, volumePercentage, 0, overtone, equalizer);
    }

    /**
     * Returns the actual number of milliseconds written
     */
    public double putFlatFreq(double freq, double durationMs, double volumePercentage, int channelIndex,
                            Overtone overtone, Equalizer equalizer) {
        int halfTotal = bitsPerSample == 8 ? 128 : 32768;
        int nFrames = (int) Math.round(durationMs * sampleRate / 1000.0);
        if (freq == 0) {
            for (int frame = 0; frame < nFrames; ++frame) writeBuffer[channelIndex].add(0);
            numFrames += nFrames;
            return 1000.0 * nFrames / sampleRate;
        }

        Map<Double, Double> overtoneMap = overtone.getMultiplierVolMap();
        double[][] mulVols = new double[overtoneMap.size()][2];  // waveLength multiplier, volume
        double volDivider = overtone.getTotalVolume();

        int i = 0;
        for (Map.Entry<Double, Double> entry : overtoneMap.entrySet()) {
            double mul = entry.getKey();
            double vol = entry.getValue();
            double thisFreq = freq * mul;
            double realVol = equalizer.volumeMultiplier(thisFreq) *
                    vol * halfTotal * volumePercentage / 100.0 / volDivider;
            double waveLength = sampleRate / thisFreq;
            double multiplier = Math.PI / (waveLength / 2);
            mulVols[i][0] = multiplier;
            mulVols[i++][1] = realVol;
        }

        for (int frame = 0; frame < nFrames; ++frame) {
            double y = 0.0;
            for (double[] mulVol : mulVols) {
                y += Math.sin(frame * mulVol[0]) * mulVol[1];
            }

            writeBuffer[channelIndex].add((int) y);
            numFrames++;
        }
        return 1000.0 * nFrames / sampleRate;
    }

    public void putData(int[] data, int channelIndex) {
        for (int datum : data) {
            writeBuffer[channelIndex].add(datum);
        }
    }

    public static int[] makeSimpleWaveData(double freq, double durationMs, int bitsPerSample, long sampleRate,
                                           double volPercent) {
        int halfTotal = bitsPerSample == 8 ? 128 : 32768;
        int nFrames = (int) Math.round(durationMs * sampleRate / 1000.0);
        double waveLength = sampleRate / freq;
        double multiplier = Math.PI / (waveLength / 2);

        int[] res = new int[nFrames];

        for (int frame = 0; frame < nFrames; ++frame) {
            double y = Math.sin(frame * multiplier) * halfTotal * volPercent / 100.0;
            res[frame] = (int) y;
        }

        return res;
    }

    public void flushBuffer() {
        if (data != null) {
            throw new RuntimeException("Wave file already has data.");
        }
        data = new int[numChannels][numFrames];
        for (int c = 0; c < numChannels; ++c) {
            for (int f = 0; f < numFrames; ++f) {
                data[c][f] = writeBuffer[c].get(f);
            }
        }
        writeBuffer = null;
    }

    public void writeWav() {
        int subChunk2Size = numFrames * numChannels * (bitsPerSample / 8);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            Util.writeString(bos, "RIFF");
            Util.writeInt4Little(bos, subChunk2Size + 36);
            Util.writeString(bos, "WAVE");

            Util.writeString(bos, "fmt ");
            Util.writeInt4Little(bos, 16);
            Util.writeInt2Little(bos, audioFormat);
            Util.writeInt2Little(bos, numChannels);
            Util.writeInt4Little(bos, sampleRate);
            Util.writeInt4Little(bos, byteRate);
            Util.writeInt2Little(bos, blockAlign);
            Util.writeInt2Little(bos, bitsPerSample);

            Util.writeString(bos, "data");
            Util.writeInt4Little(bos, subChunk2Size);

            for (int f = 0; f < numFrames; ++f) {
                for (int c = 0; c < numChannels; ++c) {
                    if (bitsPerSample == 8) {
                        bos.write(data[c][f]);
                    } else if (bitsPerSample == 16) {
                        Util.writeInt2Little(bos, data[c][f]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "WavFile{" +
                "file=" + file +
                ", numFrames=" + numFrames +
                ", numChannels=" + numChannels +
                ", audioFormat=" + audioFormat +
                ", bitsPerSample=" + bitsPerSample +
                ", blockAlign=" + blockAlign +
                ", sampleRate=" + sampleRate +
                ", byteRate=" + byteRate +
//                ",\n data=" + Arrays.toString(data[0]) +
                '}';
    }
}
