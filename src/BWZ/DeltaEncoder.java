package BWZ;

import Utility.Bytes;
import Utility.Util;

public class DeltaEncoder {

    private byte[] text;

    DeltaEncoder(byte[] text) {
        this.text = text;
    }

    byte[] Encode() {
        int i = 1;
        StringBuilder builder = new StringBuilder();
        while (i < text.length) {
            if (text[i] == text[i - 1]) {
                builder.append('0');
            } else {
                builder.append('1');
                int diff = text[i] - text[i - 1];
                char isPositive = diff > 0 ? '1' : '0';
                builder.append(isPositive);
                for (int j = 0; j < Math.abs(diff); j++) builder.append('1');
            }
            i += 1;
        }
        return Bytes.stringBuilderToBytesFull(builder);
    }
}
