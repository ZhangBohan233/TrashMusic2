package trashsoftware.trashMusic.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Util {

    @SafeVarargs
    public static <K, V> Map<K, V> mergeMaps(Map<K, V>... maps) {
        Map<K, V> resultMap = new HashMap<>();
        for (Map<K, V> map : maps) resultMap.putAll(map);
        return resultMap;
    }

    public static int count(String string, char target) {
        int res = 0;
        for (char c : string.toCharArray()) {
            if (c == target) res++;
        }
        return res;
    }

    public static long readInt4Little(InputStream stream) throws IOException {
        byte[] buf = stream.readNBytes(4);
        return (buf[0] & 0xffL) | ((buf[1] & 0xffL) << 8) | ((buf[2] & 0xffL) << 16) | ((buf[3] & 0xffL) << 24);
    }

    public static void writeInt4Little(OutputStream stream, long value) throws IOException {
        stream.write(new byte[]{(byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24)});
    }

    public static int readInt2Little(InputStream stream) throws IOException {
        byte[] buf = stream.readNBytes(2);
        return (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);
    }

    public static void writeInt2Little(OutputStream stream, int value) throws IOException {
        stream.write(new byte[]{(byte) value, (byte) (value >> 8)});
    }

    public static String readString(InputStream stream, int len) throws IOException {
        return new String(stream.readNBytes(len));
    }

    public static void writeString(OutputStream stream, String s) throws IOException {
        stream.write(s.getBytes(StandardCharsets.UTF_8));
    }

    public static class IntList extends ArrayList<Integer> {

    }
}
