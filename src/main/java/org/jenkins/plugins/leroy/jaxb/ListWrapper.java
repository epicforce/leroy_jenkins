package org.jenkins.plugins.leroy.jaxb;

import javax.xml.bind.annotation.XmlAnyElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dzmitry Bahdanovich on 04.07.14.
 */
public class ListWrapper<T> {

    private List<T> items;

    public ListWrapper() {
        items = new ArrayList<T>();
    }

    public ListWrapper(List<T> items) {
        this.items = items;
    }

    @XmlAnyElement(lax = true)
    public List<T> getItems() {
        return items;
    }

}
