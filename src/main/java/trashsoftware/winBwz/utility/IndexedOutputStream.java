package trashsoftware.winBwz.utility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class IndexedOutputStream {

    private OutputStream outputStream;
    private int bufferSize;

    /**
     * The bitwise operand that simulate the operation {@code index % bufferSize}.
     */
    private long andEr;

    private byte[] buffer1;
    private byte[] buffer2;
    private boolean headInBuffer1 = true;

    /**
     * The currently writing index.
     */
    private long head = 0;

    public IndexedOutputStream(OutputStream outputStream, int bufferSize) {
        this.outputStream = outputStream;
        if (!Bytes.is2Power(bufferSize)) throw new ArithmeticException("Buffer size must be power of 2");
        this.bufferSize = bufferSize;
        buffer1 = new byte[bufferSize];
        buffer2 = new byte[bufferSize];
        andEr = bufferSize - 1;
    }

    /**
     * Writes one byte to the stream.
     *
     * @param b the byte to be written
     * @throws IOException if the {@code outputStream} throws error
     */
    public void writeOne(byte b) throws IOException {
        if (headInBuffer1) {
            buffer1[(int) (head++ & andEr)] = b;
        } else {
            buffer2[(int) (head++ & andEr)] = b;
        }
        if ((head & andEr) == 0) {
            flushBuffer(head);
            headInBuffer1 = !headInBuffer1;
        }
    }

    /**
     * Copies a byte sequence from the previous position in this stream and append the sequence to this stream.
     *
     * @param from    the index in {@code outputStream} of copy begin, inclusive
     * @param copyLen the length of copying
     */
    public void copyRepeat(long from, int copyLen) throws IOException {
        if (from < head - bufferSize || copyLen > bufferSize || copyLen == 0)
            throw new IndexOutOfBoundsException("Index out of buffer or, length too long, or empty copy.");

        long headAfter = head + copyLen;
        long bufferBoarder = head - (head & andEr);
        int fromInBuf = (int) (from & andEr);
        int headInBuf = (int) (head & andEr);

        if ((headAfter & andEr) > (head & andEr)) {  // the new head is still in the current buffer
            if (from < bufferBoarder) {  // from is in another buffer
                // [........][........]
                //         >   < | ^     from = >, to = <, new head = ^
                int lenInPrevBuf = Math.min(bufferSize - fromInBuf, copyLen);
                if (headInBuffer1) {
                    System.arraycopy(buffer2, fromInBuf, buffer1, headInBuf, lenInPrevBuf);
                    System.arraycopy(buffer1,
                            0,
                            buffer1,
                            headInBuf + lenInPrevBuf,
                            copyLen - lenInPrevBuf);
                } else {
                    System.arraycopy(buffer1, fromInBuf, buffer2, headInBuf, lenInPrevBuf);
                    System.arraycopy(buffer2,
                            0,
                            buffer2,
                            headInBuf + lenInPrevBuf,
                            copyLen - lenInPrevBuf);
                }
            } else {  // same buffer copy
                // [........][........]
                //            > < | ^
                if (headInBuffer1) {
                    System.arraycopy(buffer1, fromInBuf, buffer1, headInBuf, copyLen);
                } else {
                    System.arraycopy(buffer2, fromInBuf, buffer2, headInBuf, copyLen);
                }
            }
        } else {  // one buffer would be flushed
            flushBuffer(headAfter);
            int pasteLenInFirstBuf = bufferSize - headInBuf;

            // not possible since if this is true, then the 'from' would smaller than head - bufferSize
            if (from < bufferBoarder) {  // from previous buffer
                // [........][........][........]
                //         >    <   |    ^
                int copyLenInPrevBuf = bufferSize - fromInBuf;
                int secondPasteLen = pasteLenInFirstBuf - copyLenInPrevBuf;
                if (secondPasteLen < 0) throw new ArithmeticException("Not possible");
                int thirdPasteLen = copyLen - copyLenInPrevBuf - secondPasteLen;

                if (headInBuffer1) {
                    System.arraycopy(buffer2, fromInBuf, buffer1, headInBuf, copyLenInPrevBuf);
                    System.arraycopy(buffer1,
                            0,
                            buffer1,
                            headInBuf + copyLenInPrevBuf,
                            secondPasteLen);
                    System.arraycopy(buffer1, secondPasteLen, buffer2, 0, thirdPasteLen);
                } else {
                    System.arraycopy(buffer1, fromInBuf, buffer2, headInBuf, copyLenInPrevBuf);
                    System.arraycopy(buffer2,
                            0,
                            buffer2,
                            headInBuf + copyLenInPrevBuf,
                            secondPasteLen);
                    System.arraycopy(buffer2, secondPasteLen, buffer1, 0, thirdPasteLen);
                }
            } else {  // from is in current operating buffer
                // [........][........][........]
                //             >  < |    ^
                if (headInBuffer1) {
                    System.arraycopy(buffer1, fromInBuf, buffer1, headInBuf, pasteLenInFirstBuf);
                    System.arraycopy(buffer1,
                            fromInBuf + pasteLenInFirstBuf,
                            buffer2,
                            0,
                            copyLen - pasteLenInFirstBuf);
                } else {
                    System.arraycopy(buffer2, fromInBuf, buffer2, headInBuf, pasteLenInFirstBuf);
                    System.arraycopy(buffer2,
                            fromInBuf + pasteLenInFirstBuf,
                            buffer1,
                            0,
                            copyLen - pasteLenInFirstBuf);
                }
            }
            headInBuffer1 = !headInBuffer1;
        }
        head = headAfter;
    }

    /**
     * Writes all remaining data in the buffer into the out file.
     *
     * @throws IOException if the {@code outputStream} is not writable or cannot be flushed
     */
    public void flush() throws IOException {
        if (headInBuffer1) {
            if (head > bufferSize) {
                outputStream.write(buffer2);
            }
            outputStream.write(buffer1, 0, (int) (head & andEr));
        } else {
            if (head > bufferSize) {
                outputStream.write(buffer1);
            }
            outputStream.write(buffer2, 0, (int) (head & andEr));
        }
        outputStream.flush();
    }

    /**
     * Closes this {@code FileOutputBufferArray} and all of its inner streams.
     *
     * @throws IOException if the {@code outputStream} cannot be closed
     */
    public void close() throws IOException {
        outputStream.close();
    }

    /**
     * Returns the current writing position.
     *
     * @return the current writing position
     */
    public long getIndex() {
        return head;
    }

    public void printBuffers() {
        System.out.println(headInBuffer1);
        if (headInBuffer1) {
            System.out.print("b2");
            System.out.print(Arrays.toString(buffer2));
            System.out.print("b1");
            System.out.println(Arrays.toString(buffer1));
        } else {
            System.out.print("b1");
            System.out.print(Arrays.toString(buffer1));
            System.out.print("b2");
            System.out.println(Arrays.toString(buffer2));
        }
    }

    private void flushBuffer(long newHead) throws IOException {
        if (newHead >= bufferSize * 2) {
            if (headInBuffer1) {
                outputStream.write(buffer2);
            } else {
                outputStream.write(buffer1);
            }
        }
    }
}
