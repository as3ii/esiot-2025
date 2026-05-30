package org.drone_remote_unit.view;

import java.util.Collection;
import java.util.Comparator;
import javax.swing.DefaultComboBoxModel;

final class SortedComboBoxModel<T> extends DefaultComboBoxModel<T> {
    private static final long serialVersionUID = 849712390324L;
    private final Comparator<T> comparator; // Can be null

    SortedComboBoxModel(final T[] array) {
        this(array, null);
    }

    SortedComboBoxModel(final T[] array, final Comparator<T> comp) {
        comparator = comp;
        for (final T item : array) {
            this.addElement(item);
        }
    }

    SortedComboBoxModel(final Collection<T> collection) {
        this(collection, null);
    }

    SortedComboBoxModel(final Collection<T> collection, final Comparator<T> comp) {
        comparator = comp;
        for (final T item : collection) {
            this.addElement(item);
        }
    }

    @Override
    public void addElement(final T element) {
        int index;
        for (index = 0; index < getSize(); index++) {
            if (comparator != null) {
                if (comparator.compare(element, getElementAt(index)) > 0) {
                    break;
                }
            } else {
                final Comparable<T> c = (Comparable<T>) getElementAt(index);
                if (c.compareTo(element) > 0) {
                    break;
                }
            }
        }
        super.insertElementAt(element, index);
    }
}
