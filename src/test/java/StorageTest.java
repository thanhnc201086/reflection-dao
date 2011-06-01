import org.junit.Test;
import org.pleasantnightmare.dbase.DefaultIdentified;
import org.pleasantnightmare.dbase.reflection.ReflectionDAO;
import org.pleasantnightmare.dbase.reflection.Table;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @since 6/1/11 11:53 AM
 */
public class StorageTest {
    @Test
    public void storageTest() {
        Table<StorageTestData> view = ReflectionDAO.instantiateViewOn(StorageTestData.class);

        StorageTestData storageTestData = new StorageTestData(1, "le name", true);
        assertFalse(storageTestData.isPersisted());

        assertEquals(0, view.all().size());
        view.insert(storageTestData);
        assertTrue(storageTestData.isPersisted());
        System.err.println("Data inserted and marked as persisted.");

        assertEquals(1, view.all().size());

        StorageTestData selectedData = view.selectById(storageTestData.getId());
        assertFalse(selectedData == storageTestData);
        assertTrue(selectedData.equals(storageTestData));
        System.err.println("Selected data is different object than inserted data, but they're equal");

        List<StorageTestData> all = view.all();
        for (StorageTestData testData : all)
            view.delete(testData);

        assertEquals(0, view.all().size());
        System.err.println("All data deleted.");
    }

    private static class StorageTestData extends DefaultIdentified {
        private int number;
        private String name;
        private boolean state;

        private StorageTestData() {
        }

        private StorageTestData(int number, String name, boolean state) {
            this.number = number;
            this.name = name;
            this.state = state;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isState() {
            return state;
        }

        public void setState(boolean state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StorageTestData that = (StorageTestData) o;

            if (number != that.number) return false;
            if (state != that.state) return false;
            if (!name.equals(that.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = number;
            result = 31 * result + name.hashCode();
            result = 31 * result + (state ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("StorageTestData");
            sb.append("{number=").append(number);
            sb.append(", name='").append(name).append('\'');
            sb.append(", state=").append(state);
            sb.append('}');
            return sb.toString();
        }
    }
}
