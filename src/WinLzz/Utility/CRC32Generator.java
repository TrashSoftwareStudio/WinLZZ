package WinLzz.Utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

public class CRC32Generator {

    public static long generateCRC32(String fileName) throws IOException {
        CRC32 crc = new CRC32();
        FileChannel fc = new FileInputStream(fileName).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int read;
        while ((read = fc.read(buffer)) > 0) {
            buffer.flip();
            crc.update(buffer.array(), 0, read);
            buffer.clear();
        }
        fc.close();
        return crc.getValue();
    }
}
