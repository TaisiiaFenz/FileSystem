
import com.fs.filesystem.Directory;
import com.fs.filesystem.FileSystem;
import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import com.fs.utils.FileSystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTest {
    private FileSystem fileSystem;

    @BeforeEach
    void init() {
        fileSystem = new FileSystem(new IOSystem(new LDisk()));
    }

    @Test
    public void create() {
        assertEquals(FileSystemConfig.SUCCESS,fileSystem.create("FILE"));
        assertEquals(9, fileSystem.searchFreeDataBlock(fileSystem.bitmap));
        assertEquals(FileSystemConfig.ERROR, fileSystem.create("FIIIIILE"));
    }

    @Test
    public void destroy() {
        assertEquals(FileSystemConfig.SUCCESS,fileSystem.create("FILE"));
        assertEquals(9, fileSystem.searchFreeDataBlock(fileSystem.bitmap));
        assertEquals(FileSystemConfig.SUCCESS,fileSystem.destroy("FILE"));
        assertEquals(8, fileSystem.searchFreeDataBlock(fileSystem.bitmap));
        assertEquals(FileSystemConfig.ERROR, fileSystem.destroy("f1"));
    }

    @Test
    public void createFileWithExistingFileName() {
        fileSystem.create("f1");
        fileSystem.open("f1");
        int count = 70;
        byte[] memArea = new byte[count];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) 'x';
        }
        fileSystem.write(1, memArea,count);
        ByteBuffer readBuffer = ByteBuffer.allocate(count);
        assertTrue(fileSystem.read(1,readBuffer,count )>50);
        fileSystem.close(1);
        fileSystem.create("f1");
        fileSystem.open("f1");
        readBuffer = ByteBuffer.allocate(count);
        System.out.println(fileSystem.read(1,readBuffer,count ));
        // when fileLength is zero then read method returns -1
        assertTrue(fileSystem.read(1,readBuffer,count )<0);
    }

    @Test
    public void write() {
        fileSystem.create("FILE");
        fileSystem.open("FILE");

        byte[] memArea = new byte[60];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) 'x';
        }
        int bytesWrittenToFile= fileSystem.write(1, memArea, 60);
        assertNotNull(bytesWrittenToFile);
        assertEquals(60, bytesWrittenToFile);
    }

    @Test
    public void readWithoutWritting() {
        fileSystem.create("FILE");
        fileSystem.open("FILE");

        ByteBuffer readBuffer = ByteBuffer.allocate(10);
        int numOfReadBytes = fileSystem.read(1, readBuffer, 10);
        assertEquals(-1, numOfReadBytes);
    }

    @Test
    public void readWithWritting() {
        fileSystem.create("FILE");
        fileSystem.open("FILE");

        byte[] memArea = new byte[30];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) 'x';
        }
        fileSystem.write(1, memArea, 30);

        ByteBuffer readBuffer = ByteBuffer.allocate(10);
        int numOfReadBytes = fileSystem.read(1, readBuffer, 10);
        assertNotNull(numOfReadBytes);
        assertEquals(10, numOfReadBytes);
    }

    @Test
    public void readWithWrittingAtEndOfFile() {
        fileSystem.create("FILE");
        fileSystem.open("FILE");

        byte[] memArea = new byte[60];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) 'x';
        }
        fileSystem.write(1, memArea, 60);

        ByteBuffer readBuffer = ByteBuffer.allocate(10);
        int numOfReadBytes = fileSystem.read(1, readBuffer, 10);
        assertNotNull(numOfReadBytes);
        assertEquals(4, numOfReadBytes);
    }

    @Test
    public void seek() {
        fileSystem.create("FILE");
        fileSystem.open("FILE");

        byte[] memArea = new byte[60];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) 'x';
        }
        fileSystem.write(1, memArea, 60);
        int statusOfSeek = fileSystem.seek(1, 55);
        assertEquals(1, statusOfSeek);
    }

    @Test
    public void open() {
        fileSystem.create("FILE");
        int oftIndex = fileSystem.open("FILE");

        //oftIndex points to first empty OFT entry
        assertEquals(1, oftIndex);
        assertNotNull(fileSystem.openFileTable.entries[oftIndex]);
    }

    @Test
    public void open_wrongFileName() {
        fileSystem.create("FILE");
        int response = fileSystem.open("FILE1");
        assertEquals(-1, response);
    }

    @Test
    public void open_alreadyOpened() {
        fileSystem.create("FILE");
        fileSystem.open("FILE");
        int response = fileSystem.open("FILE");
        assertEquals(-1, response);
    }

    @Test
    public void open_noMoreFreeOftEntries() {
        fileSystem.create("F");
        int response = fileSystem.open("F");

        fileSystem.create("F1");
        int response1 = fileSystem.open("F1");

        fileSystem.create("F3");
        int response2 = fileSystem.open("F3");

        fileSystem.create("F4");
        int response3 = fileSystem.open("F4");

        assertEquals(1, response);
        assertEquals(2, response1);
        assertEquals(3, response2);
        assertEquals(-1, response3);
    }

    @Test
    public void close() {
        fileSystem.create("FILE");
        int oftIndex = fileSystem.open("FILE");

        int response = fileSystem.close(oftIndex);

        assertEquals(1, response);
        //entry is cleaned
        assertNull(fileSystem.openFileTable.entries[oftIndex]);
    }

    @Test
    public void close_wrongIndex() {
        fileSystem.create("FILE");
        int oftIndex = fileSystem.open("FILE");

        int response = fileSystem.close(oftIndex + 1);

        assertEquals(-1, response);
    }

    @Test
    public void saveAndReadDescriptors() {
        fileSystem.create("F1");
        fileSystem.create("F2");
        fileSystem.create("F3");
        fileSystem.create("F4");
        fileSystem.saveDescriptorsToDisk();
        fileSystem.descriptors[1] = null;
        assertNull(fileSystem.descriptors[1]);
        fileSystem.readDescriptorsFromDisk();
        assertNotNull(fileSystem.descriptors[1]);
    }

    @Test
    void saveBitMapToDisk() {
        BitSet bitMapOnDisk = fileSystem.readBitMapFromDisk();

        //previously bits were false
        assertFalse(bitMapOnDisk.get(9));
        assertFalse(bitMapOnDisk.get(10));

        //set them to true
        BitSet bitMap = fileSystem.bitmap;
        bitMap.set(9, true);
        bitMap.set(10, true);

        fileSystem.saveBitMapToDisk(bitMap);

        bitMapOnDisk = fileSystem.readBitMapFromDisk();
        //and now these bits are updated
        assertTrue(bitMapOnDisk.get(9));
        assertTrue(bitMapOnDisk.get(10));
    }

    @Test
    void saveDirectoryToDisk() {
        int maxNumberOfFiles = 23;
        for (int i = 0; i < maxNumberOfFiles; i++) {
            fileSystem.create("F" + i);
        }

        fileSystem.directory = new Directory();
        assertEquals(0, fileSystem.directory.listOfEntries.size());
        fileSystem.readDirectoryFromDisk();
        for (int i = 0; i < maxNumberOfFiles; i++) {
            assertEquals("F"+i, fileSystem.directory.listOfEntries.get(i).fileName);
        }
    }


    @Test
    void saveDiskToFile() {
        fileSystem.create("file");
        fileSystem.ioSystem.saveDiskToFile("disk.txt");
        LDisk disk = fileSystem.ioSystem.readDiskFromFile("disk.txt");
        FileSystem newFileSystem = new FileSystem(disk);
        assertEquals(fileSystem.searchFreeDataBlock(fileSystem.bitmap), newFileSystem.searchFreeDataBlock(newFileSystem.bitmap));
        assertEquals(fileSystem.directory.listOfEntries.get(0).fileName, newFileSystem.directory.listOfEntries.get(0).fileName);
    }

    @Test
    void listDirectory() {
        fileSystem.create("f1");
        fileSystem.create("f2");
        fileSystem.create("f3");
        fileSystem.listDirectory();
    }
}
