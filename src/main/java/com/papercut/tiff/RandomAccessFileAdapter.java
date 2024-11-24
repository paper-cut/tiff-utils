package com.papercut.tiff;


import com.papercut.tiff.expection.TiffException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RandomAccessFileAdapter {

    private RandomAccessFile randomAccessFile;

    protected long length;

    protected long pointer=0;

    protected ByteOrder byteOrder=null;


    public RandomAccessFileAdapter(String filePath, String mode) throws IOException {
        File file = new File(filePath);
        if (!file.exists()){
            throw new IOException("文件不存在: "+filePath);
        }
        this.randomAccessFile= new RandomAccessFile(file, mode);
        this.length=randomAccessFile.length();
    }


    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public long getPointer() {
        return pointer;
    }

    public void setPointer(long pointer) {
        this.pointer = pointer;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    public String readString(int num) throws IOException {
        String value = readString(pointer, num);
        pointer += num;
        return value;
    }


    public String readString(long offset, int num)
            throws IOException {
        verifyRemainingBytes(offset, num);
        String value = null;
        byte[] bytes = new byte[num];
        randomAccessFile.seek(offset);
        randomAccessFile.read(bytes);
        if (num != 1 || bytes[0] != 0) {
            value = new String(bytes, 0, num, StandardCharsets.US_ASCII);
        }
        return value;
    }

    /**
     * Read a short
     *
     * @return short
     */
    public short readShort() throws IOException {
        short value = readShort(pointer);
        pointer += 2;
        return value;
    }

    /**
     * Read a short
     *
     * @param offset
     *            byte offset
     * @return short
     */
    public short readShort(long offset) throws IOException {
        verifyRemainingBytes(offset, 2);
        byte[] bytes = new byte[2];
        randomAccessFile.seek(offset);
        randomAccessFile.read(bytes);
        short value = ByteBuffer.wrap(bytes, 0, 2).order(byteOrder)
                .getShort();
        return value;
    }

    /**
     * Read an unsigned short
     *
     * @return unsigned short as int
     */
    public int readUnsignedShort() throws IOException {
        int value = readUnsignedShort(pointer);
        pointer += 2;
        return value;
    }

    /**
     * Read an unsigned short
     *
     * @param offset
     *            byte offset
     * @return unsigned short as int
     */
    public int readUnsignedShort(long offset) throws IOException {
        return (readShort(offset) & 0xffff);
    }

    /**
     * 读取一个Integer
     *
     * @return integer
     */

    public int readInt() throws IOException {
        int value = readInt(pointer);
        pointer += 4;
        return value;
    }

    /**
     * 读取一个Integer
     *
     * @param offset
     *            byte 偏移
     * @return integer
     */
    public int readInt(long offset) throws IOException {
        verifyRemainingBytes(offset, 4);
        byte[] bytes = new byte[4];
        randomAccessFile.seek(offset);
        randomAccessFile.read(bytes);
        int value = ByteBuffer.wrap(bytes, 0, 4).order(byteOrder).getInt();
        return value;
    }

    /**
     * Read an unsigned int
     *
     * @return unsigned int as long
     */
    public long readUnsignedInt() throws IOException {
        long value = readUnsignedInt(pointer);
        pointer += 4;
        return value;
    }

    /**
     * Read an unsigned int
     *
     * @param offset
     *            byte offset
     * @return unsigned int as long
     */
    public long readUnsignedInt(long offset) throws IOException {
        return ((long) this.readInt(offset) & 0xffffffffL);
    }

    /**
     * Read a byte
     *
     * @return byte
     */
    public byte readByte() throws IOException {
        byte value = readByte(pointer);
        pointer++;
        return value;
    }

    /**
     * Read a byte
     *
     * @param offset
     *            byte offset
     * @return byte
     */
    public byte readByte(long offset) throws IOException {
        verifyRemainingBytes(offset, 1);
        randomAccessFile.seek(offset);
        byte value = randomAccessFile.readByte();
        return value;
    }

    /**
     * Read an unsigned byte
     *
     * @return unsigned byte as short
     */
    public short readUnsignedByte() throws IOException {
        short value = readUnsignedByte(pointer);
        pointer++;
        return value;
    }

    /**
     * Read an unsigned byte
     *
     * @param offset
     *            byte offset
     * @return unsigned byte as short
     */
    public short readUnsignedByte(long offset) throws IOException {
        return ((short) (readByte(offset) & 0xff));
    }

    /**
     * Read a number of bytes
     *
     * @param num
     *            number of bytes
     * @return bytes
     */
    public byte[] readBytes(int num) throws IOException {
        byte[] readBytes = readBytes(pointer, num);
        pointer += num;
        return readBytes;
    }

    /**
     * Read a number of bytes
     *
     * @param offset
     *            byte offset
     * @param num
     *            number of bytes
     * @return bytes
     */
    public byte[] readBytes(long offset, int num) throws IOException {
        verifyRemainingBytes(offset, num);
        byte[] bytes = new byte[num];
        randomAccessFile.seek(offset);
        randomAccessFile.read(bytes);
        byte[] readBytes = Arrays.copyOfRange(bytes, 0, num);
        return readBytes;
    }

    /**
     * Read a float
     *
     * @return float
     */
    public float readFloat() throws IOException {
        float value = readFloat(pointer);
        pointer += 4;
        return value;
    }

    /**
     * Read a float
     *
     * @param offset
     *            byte offset
     * @return float
     */
    public float readFloat(long offset) throws IOException {
        verifyRemainingBytes(offset, 4);
        byte[] bytes = new byte[4];
        randomAccessFile.seek(offset);
        randomAccessFile.read(bytes);
        float value = ByteBuffer.wrap(bytes, 0, 4).order(byteOrder)
                .getFloat();
        return value;
    }

    /**
     * Read a double
     *
     * @return double
     */
    public double readDouble() throws IOException {
        double value = readDouble(pointer);
        pointer += 8;
        return value;
    }

    /**
     * Read a double
     *
     * @param offset
     *            byte offset
     * @return double
     */
    public double readDouble(long offset) throws IOException {
        verifyRemainingBytes(offset, 8);
        byte[] bytes = new byte[8];
        randomAccessFile.seek(offset);
        randomAccessFile.read(bytes);
        double value = ByteBuffer.wrap(bytes, 0, 8).order(byteOrder)
                .getDouble();
        return value;
    }




    private void verifyRemainingBytes(long offset, int bytesToRead) {
        if (offset + bytesToRead > this.length) {
            throw new TiffException(
                    "No more remaining bytes to read. Total Bytes: "
                            + length + ", Byte offset: " + offset
                            + ", Attempted to read: " + bytesToRead);
        }
    }


}
