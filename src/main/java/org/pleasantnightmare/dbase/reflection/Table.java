package org.pleasantnightmare.dbase.reflection;

import org.pleasantnightmare.dbase.Identified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.LinkedList;
import java.util.List;

/**
 * An illusion of dbase table, that works with filesystem.
 * Terminology similarity with rmdbs is intentional, as it
 * might ease the understanding of reflection DAO.
 *
 * @author ivicaz
 */
public class Table<T extends Identified> {
    private static final Logger LOG = LoggerFactory.getLogger(Table.class);
    private File relativeTableDirectory;
    private File root;
    private Class<T> dataClass;

    public Table(File root, Class<T> dataClass) {
        this.root = root;
        this.dataClass = dataClass;
        updateTableDir();
    }

    public List<T> all() {
        List<T> data = new LinkedList<T>();
        File[] files = relativeTableDirectory.listFiles();
        for (File file : files) {
            if (file.getName().startsWith("."))
                continue;
            T object = deserializeObject(file);
            data.add(object);
        }

        return data;
    }

    private void updateTableDir() {
        relativeTableDirectory = new File(root, dataClass.getSimpleName().toLowerCase());
        File maxNumberFile = new File(relativeTableDirectory, ".maxNumber");
        try {
            if (!relativeTableDirectory.exists()) {
                relativeTableDirectory.mkdirs();
                maxNumberFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * This stuff simulates an auto-increment number from a proper DB.
     * Yes, I am aware of the sanity point cost to reading this code.
     *
     * @return new unused number
     */
    private int getNextIndex() throws IOException {
        File maxNumberFile = new File(relativeTableDirectory, ".maxNumber");
        RandomAccessFile maxNumberRAFile;
        maxNumberRAFile = new RandomAccessFile(maxNumberFile, "rw");
        FileChannel maxNumberChannel = maxNumberRAFile.getChannel();
        FileLock lock = maxNumberChannel.lock();
        ByteBuffer numberBuffer = ByteBuffer.allocate(4);
        maxNumberChannel.read(numberBuffer);
        int maxNumber = numberBuffer.getInt(0);
        int nextIndex = maxNumber + 1;
        numberBuffer.putInt(0, nextIndex);
        numberBuffer.position(0);
        int bytesWritten = maxNumberChannel.write(numberBuffer, 0);
        lock.release();
        maxNumberChannel.close();
        return nextIndex;
    }

    public T selectById(int id) {
        File file = getRow(id);
        return deserializeObject(file);
    }

    public boolean contains(int id) {
        File file = getRow(id);
        return file != null;
    }

    public void update(int id, T data) {
        if (!contains(id))
            throw new IllegalStateException("No row with ID: " + id);

        File file = getRow(id);
        if (!file.delete())
            throw new IllegalStateException("Failed to delete row: " + id);
        saveFileAs(id, data);
    }

    public void insert(T data) {
        // Bear in mind that this thing behaves similar to normal dbase
        // autoincrements: it doesn't care if there are any 'free'
        // ID's in the middle, it just returns one ID higher than
        // highest ID stored.
        int nextId = 0;
        try {
            nextId = getNextIndex();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        data.setId(nextId);
        saveFileAs(nextId, data);
    }

    public void delete(T data) {
        if (data.isAlreadyIdentified()) {
            deleteFileAs(data.getId());
        }
    }

    private void deleteFileAs(int id) {
        File row = getRow(id);
        if (row != null) {
            row.delete();
        }
    }

    private void saveFileAs(int id, T data) {
        if (contains(id))
            throw new IllegalStateException("Row already exists, ID: " + id);

        File row = new File(relativeTableDirectory, String.valueOf(id));
        serializeObject(data, row);
    }

    private File getRow(final int id) {
        File potentialRowFile = new File(relativeTableDirectory, Integer.toString(id));

        if (potentialRowFile.exists()) {
            return potentialRowFile;
        }

        return null;
    }

    private void serializeObject(T data, File row) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(row);
            if (!data.isAlreadyIdentified())
                data.setId(Integer.parseInt(row.getName()));
            ReflectionDAO.getInstance().serialize(data, fos);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                LOG.warn("Failed to close serialization stream!", e);
            }
        }
    }

    private T deserializeObject(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return ReflectionDAO.getInstance().deserialize(dataClass, fis);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                LOG.warn("Failed to close deserialization stream!", e);
            }
        }
    }
}
