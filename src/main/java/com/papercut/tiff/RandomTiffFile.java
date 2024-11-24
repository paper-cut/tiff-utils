package com.papercut.tiff;


import com.papercut.tiff.constant.FieldTagType;
import com.papercut.tiff.constant.FieldType;
import com.papercut.tiff.constant.TiffConstants;
import com.papercut.tiff.expection.TiffException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 利用RandomAccessFile，将其中的read、readByte、write等操作方法做封装，将对偏离位置的操作转换为对特定行列号的操作
 */
public class RandomTiffFile {

    /**
     * 本RandomTiffFile文件对应的RandomAccessFile
     * 后续的各种操作都是基于此完成的
     */
    private RandomAccessFileAdapter file;

    private Set<FileDirectory> fileDirectories=new HashSet<>();




    /**
     * 构造方法，通过文件地址和启动模式构造RandomTiffFile类
     * mode包括如下：
     * r: 只读
     * rw: 读写
     * @param filePath
     * @param mode
     */
    public RandomTiffFile(String filePath,String mode) throws IOException {
        this.file=new RandomAccessFileAdapter(filePath,mode);
        readTiff();
    }


    /**
     * 主函数
     * @param args
     */
    public static void main(String[] args) {
        try {
            RandomTiffFile file1 = new RandomTiffFile("D:\\software\\arcgis\\workspace\\LoadTifftest.tif", "r");
            System.out.printf(file1.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 读取TIFF信息
     *
     */
    public void readTiff() throws IOException {

        // Read the 2 bytes of byte order
        String byteOrderString = null;
        try {
            byteOrderString = file.readString(2);
        } catch (UnsupportedEncodingException e) {
            throw new TiffException("Failed to read byte order", e);
        }

        // Determine the byte order
//        ByteOrder byteOrder = null;
        switch (byteOrderString) {
            case TiffConstants.BYTE_ORDER_LITTLE_ENDIAN:
                file.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                break;
            case TiffConstants.BYTE_ORDER_BIG_ENDIAN:
                file.setByteOrder(ByteOrder.BIG_ENDIAN);
                break;
            default:
                throw new TiffException("Invalid byte order: " + byteOrderString);
        }

        long tiffIdentifier = file.readUnsignedShort();

        if (tiffIdentifier != TiffConstants.FILE_IDENTIFIER) {
            throw new TiffException("Invalid file identifier, not a TIFF");
        }

        // Get the offset in bytes of the first image file directory (IFD)
        long byteOffset = file.readUnsignedInt();

        // Get the TIFF Image
        parseTIFFImage(byteOffset);

//        return tiffImage;
    }


    /**
     * 解析IFD
     * @param byteOffset
     * @throws IOException
     */
    private void parseTIFFImage(long byteOffset) throws IOException {


        // Continue until the byte offset no longer points to another file
        // directory
        while (byteOffset != 0) {

            // Set the next byte to read from
            file.setPointer(byteOffset);

            // Create the new directory
            SortedSet<FileDirectoryEntry> entries = new TreeSet<>();

            // Read the number of directory entries

            int numDirectoryEntries = file.readUnsignedShort();

            // Read each entry and the values
            for (short entryCount = 0; entryCount < numDirectoryEntries; entryCount++) {

                // Read the field tag, field type, and type count
                int fieldTagValue = file.readUnsignedShort();
                FieldTagType fieldTag = FieldTagType.getById(fieldTagValue);

                int fieldTypeValue = file.readUnsignedShort();
                FieldType fieldType = FieldType.getFieldType(fieldTypeValue);
                if (fieldType == null) {
                    throw new TiffException(
                            "Unknown field type value " + fieldTypeValue);
                }

                long typeCount = file.readUnsignedInt();

                // Save off the next byte to read location
                long nextByte = file.getPointer();

                // Read the field values
                Object values = readFieldValues(fieldTag, fieldType,
                        typeCount);

                // Create and add a file directory if the tag is recognized.
                if (fieldTag != null) {
                    FileDirectoryEntry entry = new FileDirectoryEntry(fieldTag,
                            fieldType, typeCount, values);
                    entries.add(entry);
                }

                // Restore the next byte to read location
                file.setPointer(nextByte + 4);
            }

            // Add the file directory
            FileDirectory fileDirectory = new FileDirectory(entries);
            fileDirectories.add(fileDirectory);
            // Read the next byte offset location
            byteOffset = file.readUnsignedInt();
        }
    }

    /**
     * 读取字段属性值
     *
     * @param fieldTag
     *            field tag type
     * @param fieldType
     *            field type
     * @param typeCount
     *            type count
     * @return values
     */
    private Object readFieldValues(FieldTagType fieldTag, FieldType fieldType, long typeCount) throws IOException {

        // If the value is larger and not stored inline, determine the offset
        if (fieldType.getBytes() * typeCount > 4) {
            long valueOffset = file.readUnsignedInt();
            file.setPointer(valueOffset);
        }

        // Read the directory entry values
        List<Object> valuesList = getValues( fieldType, typeCount);

        // Get the single or array values
        Object values = null;
        if (typeCount == 1 && fieldTag != null && !fieldTag.isArray()
                && !(fieldType == FieldType.RATIONAL
                || fieldType == FieldType.SRATIONAL)) {
            values = valuesList.get(0);
        } else {
            values = valuesList;
        }

        return values;
    }

    /**
     * 获取TIFF目录值
     *
     * @param fieldType
     *            field type
     * @param typeCount
     *            type count
     * @return values
     */
    private List<Object> getValues(FieldType fieldType, long typeCount) throws IOException {

        List<Object> values = new ArrayList<Object>();

        for (int i = 0; i < typeCount; i++) {

            switch (fieldType) {
                case ASCII:
                    try {
                        values.add(file.readString(1));
                    } catch (UnsupportedEncodingException e) {
                        throw new TiffException("Failed to read ASCII character",
                                e);
                    }
                    break;
                case BYTE:
                case UNDEFINED:
                    values.add(file.readUnsignedByte());
                    break;
                case SBYTE:
                    values.add(file.readByte());
                    break;
                case SHORT:
                    values.add(file.readUnsignedShort());
                    break;
                case SSHORT:
                    values.add(file.readShort());
                    break;
                case LONG:
                    values.add(file.readUnsignedInt());
                    break;
                case SLONG:
                    values.add(file.readInt());
                    break;
                case RATIONAL:
                    values.add(file.readUnsignedInt());
                    values.add(file.readUnsignedInt());
                    break;
                case SRATIONAL:
                    values.add(file.readInt());
                    values.add(file.readInt());
                    break;
                case FLOAT:
                    values.add(file.readFloat());
                    break;
                case DOUBLE:
                    values.add(file.readDouble());
                    break;
                default:
                    throw new TiffException("Invalid field type: " + fieldType);
            }

        }

        // If ASCII characters, combine the strings
        if (fieldType == FieldType.ASCII) {
            List<Object> stringValues = new ArrayList<Object>();
            StringBuilder stringValue = new StringBuilder();
            for (Object value : values) {
                if (value == null) {
                    if (stringValue.length() > 0) {
                        stringValues.add(stringValue.toString());
                        stringValue = new StringBuilder();
                    }
                } else {
                    stringValue.append(value.toString());
                }
            }
            values = stringValues;
        }

        return values;
    }


    public RandomAccessFileAdapter getFile() {
        return file;
    }

    public Set<FileDirectory> getFileDirectories() {
        return fileDirectories;
    }
}
