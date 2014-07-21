package org.jenkins.plugins.leroy.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by Dzmitry Bahdanovich on 04.07.14.
 */
public class JaxbUtils {

    /**
     * Unmarshal XML to Wrapper and return List value.
     */
    public static <T> List<T> unmarshal(Unmarshaller unmarshaller,
                                         Class<T> clazz, String xmlLocation) throws JAXBException {
        StreamSource xml = new StreamSource(xmlLocation);
        ListWrapper<T> wrapper = (ListWrapper<T>) unmarshaller.unmarshal(xml,
                ListWrapper.class).getValue();
        return wrapper.getItems();
    }

    /**
     * Wrap List in Wrapper, then leverage JAXBElement to supply root element
     * information.
     */
    public static void marshal(Marshaller marshaller, List<?> list, String name, String xmlFile)
            throws JAXBException {
        QName qName = new QName(name);
        ListWrapper wrapper = new ListWrapper(list);
        JAXBElement<ListWrapper> jaxbElement = new JAXBElement<ListWrapper>(qName,
                ListWrapper.class, wrapper);
        marshaller.marshal(jaxbElement, new File(xmlFile));
    }

    public static void replaceEmptyStringFieldsToNull(Object obj) throws IllegalAccessException {
        Field[] fields = obj.getClass().getDeclaredFields();
        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                if (String.class.equals(f.getType())) {
                    boolean accessible = f.isAccessible();
                    f.setAccessible(true);
                    Object value = f.get(obj);
                    if ("".equals(value)) {
                        f.set(obj, null);
                    }
                    f.setAccessible(accessible);
                }
            }
        }
    }


}
