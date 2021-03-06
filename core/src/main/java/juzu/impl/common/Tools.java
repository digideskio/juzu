/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.common;

import juzu.io.UndeclaredIOException;
import juzu.request.ResponseParameter;
import juzu.impl.bridge.Parameters;

import javax.annotation.processing.Completion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class Tools {

  /** . */
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  /** . */
  public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

  /** . */
  public static final Charset ISO_8859_2 = Charset.forName("ISO-8859-2");

  /** . */
  public static final Charset UTF_8 = Charset.forName("UTF-8");

  /** . */
  private static final Iterator EMPTY_ITERATOR = new Iterator() {
    public boolean hasNext() {
      return false;
    }

    public Object next() {
      throw new NoSuchElementException();
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  };

  /** . */
  public static final Comparator<Completion> COMPLETION_COMPARATOR = new Comparator<Completion>() {
    public int compare(Completion o1, Completion o2) {
      return o1.getValue().compareTo(o2.getValue());
    }
  };

  /** . */
  private static final Iterable EMPTY_ITERABLE = new Iterable() {
    public Iterator iterator() {
      return EMPTY_ITERATOR;
    }
  };

  /** . */
  public static Pattern EMPTY_NO_RECURSE = Pattern.compile("");

  /** . */
  public static Pattern EMPTY_RECURSE = Pattern.compile(".*");

  public static Pattern getPackageMatcher(String packageName, boolean recurse) {

    // PackageName       -> Identifier
    // PackageName       -> PackageName . Identifier
    // Identifier        -> IdentifierChars but not a Keyword or BooleanLiteral or NullLiteral
    // IdentifierChars   -> JavaLetter
    // IdentifierChars   -> IdentifierChars JavaLetterOrDigit
    // JavaLetter        -> any Unicode character that is a Java letter
    // JavaLetterOrDigit -> any Unicode character that is a Java letter-or-digit

    if (packageName.length() == 0) {
      return recurse ? EMPTY_RECURSE : EMPTY_NO_RECURSE;
    }
    else {
      String regex;
      if (recurse) {
        regex = Pattern.quote(packageName) + "(\\..*)?";
      }
      else {
        regex = Pattern.quote(packageName);
      }
      return Pattern.compile(regex);
    }
  }

  /**
   * Returns the parent package of the provided package. Null is returned if the provided package
   * was a top level package.
   *
   * @param pkgName the package name
   * @return the parent package
   */
  public static String parentPackageOf(String pkgName) {
    int index = pkgName.lastIndexOf('.');
    if (index == -1) {
      return null;
    } else {
      return pkgName.substring(0, index);
    }
  }

  public static void escape(CharSequence s, StringBuilder appendable) {
    for (int i = 0;i < s.length();i++) {
      char c = s.charAt(i);
      if (c == '\n') {
        appendable.append("\\n");
      }
      else if (c == '\'') {
        appendable.append("\\\'");
      }
      else if (c == '\r') {
        // Skip carriage return
      } else {
        appendable.append(c);
      }
    }
  }

  public static boolean safeEquals(Object o1, Object o2) {
    return o1 == null ? o2 == null : (o2 != null && o1.equals(o2));
  }

  public static void safeClose(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      }
      catch (IOException ignore) {
      }
    }
  }

  public static Method safeGetMethod(Class<?> type, String name, Class<?>... parameterTypes) {
    try {
      return type.getDeclaredMethod(name, parameterTypes);
    }
    catch (NoSuchMethodException e) {
      return null;
    }
  }

  public static <E> void addAll(Collection<? super E> collection, Iterable<E> elements) {
    for (E element : elements) {
      collection.add(element);
    }
  }

  public static <T> List<T> safeUnmodifiableList(T... list) {
    return safeUnmodifiableList(Arrays.asList(list));
  }

  public static <T> List<T> safeUnmodifiableList(List<T> list) {
    if (list == null || list.isEmpty()) {
      return Collections.emptyList();
    }
    else {
      return Collections.unmodifiableList(new ArrayList<T>(list));
    }
  }

  public static byte[] bytes(InputStream in) throws IOException {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(in.available());
      copy(in, baos);
      return baos.toByteArray();
    }
    finally {
      safeClose(in);
    }
  }

  public static void write(String content, File f) throws IOException {
    write(content.getBytes(), f);
  }

  public static void write(byte[] content, File f) throws IOException {
    FileOutputStream out = new FileOutputStream(f);
    try {
      copy(new ByteArrayInputStream(content), out);
    }
    finally {
      safeClose(out);
    }
  }

  public static Map<String, String> responseHeaders(HttpURLConnection conn) {
    Map<String, String> headers = Collections.emptyMap();
    for (int i=0; ; i++) {
      String name = conn.getHeaderFieldKey(i);
      String value = conn.getHeaderField(i);
      if (name == null && value == null) {
        break;
      }
      if (name != null) {
        if (headers.isEmpty()) {
          headers = new HashMap<String, String>();
        }
        headers.put(name, value);
      }
    }
    return headers;
  }

  public static String read(URL url) throws IOException {
    return read(url.openStream());
  }

  public static String read(File f) throws IOException {
    return read(new FileInputStream(f));
  }

  public static String read(InputStream in) throws IOException {
    return read(in, Tools.UTF_8);
  }

  public static String read(InputStream in, Charset charset) throws IOException {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      copy(in, baos);
      return new String(baos.toByteArray(), charset);
    }
    finally {
      safeClose(in);
    }
  }

  public static <O extends OutputStream> O copy(InputStream in, O out) throws IOException {
    byte[] buffer = new byte[256];
    for (int l;(l = in.read(buffer)) != -1;) {
      out.write(buffer, 0, l);
    }
    return out;
  }

  public static String unquote(String s) throws NullPointerException {
    if (s == null) {
      throw new NullPointerException("Can't unquote null string");
    }
    if (s.length() > 1) {
      char c1 = s.charAt(0);
      char c2 = s.charAt(s.length() - 1);
      if ((c1 == '\'' || c1 == '"') && c1 == c2) {
        return s.substring(1, s.length() - 1);
      }
    }
    return s;
  }

  public static String join(char separator, String... names) {
    return join(separator, names, 0, names.length);
  }

  public static String join(char separator, String[] names, int from, int end) {
    int length = 0;
    for (int i = from;i < end;i++) {
      if (i > from) {
        length++;
      }
      length += names[i].length();
    }
    return join(new StringBuilder(length), separator, names, from, end).toString();
  }

  public static String join(char separator, Iterator<String> names) {
    return join(new StringBuilder(), separator, names).toString();
  }

  public static String join(char separator, Iterable<String> names) {
    return join(separator, names.iterator());
  }

  public static StringBuilder join(StringBuilder sb, char separator, String... names) {
    return join(sb, separator, names, 0, names.length);
  }

  public static StringBuilder join(StringBuilder sb, char separator, String[] names, int from, int end) {
    try {
      join((Appendable)sb, separator, names, from, end);
      return sb;
    }
    catch (IOException e) {
      throw new UndeclaredIOException(e);
    }
  }

  public static StringBuilder join(StringBuilder sb, char separator, Iterator<String> names) {
    try {
      join((Appendable)sb, separator, names);
      return sb;
    }
    catch (IOException e) {
      throw new UndeclaredIOException(e);
    }
  }

  public static StringBuilder join(StringBuilder sb, char separator, Iterable<String> names) {
    try {
      join((Appendable)sb, separator, names);
      return sb;
    }
    catch (IOException e) {
      throw new UndeclaredIOException(e);
    }
  }

  public static <A extends Appendable> Appendable join(A appendable, char separator, String... names) throws IOException {
    return join(appendable, separator, names, 0, names.length);
  }

  public static <A extends Appendable> Appendable join(A appendable, char separator, String[] names, int from, int end) throws IOException {
    int length = end - from;
    switch (length) {
      case 0:
        break;
      case 1:
        appendable.append(names[from]);
        break;
      default:
        for (int i = from;i < end;i++) {
          if (i > from) {
            appendable.append(separator);
          }
          appendable.append(names[i]);
        }
        break;
    }
    return appendable;
  }

  public static <A extends Appendable> Appendable join(A appendable, char separator, Iterable<String> names) throws IOException {
    return join(appendable, separator, names.iterator());
  }

  public static <A extends Appendable> Appendable join(A appendable, char separator, Iterator<String> names) throws IOException {
    if (names.hasNext()) {
      appendable.append(names.next());
      while (names.hasNext()) {
        appendable.append(separator);
        appendable.append(names.next());
      }
    }
    return appendable;
  }

  public static <A extends Appendable> A nameOf(Class<?> clazz, A appendable) throws IOException {
    if (clazz.isMemberClass()) {
      nameOf(clazz.getEnclosingClass(), appendable).append('.').append(clazz.getSimpleName());
    } else {
      appendable.append(clazz.getSimpleName());
    }
    return appendable;
  }

  public static String getName(Class<?> clazz) {
    if (clazz.isLocalClass() || clazz.isAnonymousClass()) {
      throw new IllegalArgumentException("Cannot use local or anonymous class");
    }
    else {
      try {
        return nameOf(clazz, new StringBuilder()).toString();
      }
      catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }

  public static String getImport(Class<?> clazz) {
    if (clazz.isLocalClass() || clazz.isAnonymousClass()) {
      throw new IllegalArgumentException("Cannot use local or anonymous class");
    }
    else if (clazz.isMemberClass()) {
      StringBuilder sb = new StringBuilder();
      while (clazz.isMemberClass()) {
        sb.insert(0, clazz.getSimpleName());
        sb.insert(0, '.');
        clazz = clazz.getEnclosingClass();
      }
      sb.insert(0, clazz.getSimpleName());
      String pkg = clazz.getPackage().getName();
      if (pkg.length() > 0) {
        sb.insert(0, '.');
        sb.insert(0, pkg);
      }
      return sb.toString();
    }
    else {
      return clazz.getName();
    }
  }

  /**
   * <p>Add the specified to the specified set and returns the result. When the <code>set</code> argument is an
   * instance of {@link HashSet} the element is directly added, otherwise a new <code>HashSet</code> is created by
   * cloning the <code>set</code> argument and the <code>e</code> argument is added.</p>
   * <p/>
   * <p>Usage pattern : adding a set to a non modifiable set</p>
   * <pre><code>
   *    Set&lt;String&gt; set = Collections.emptySet();
   *    set = addToHashSet(set, "foo");
   * </code></pre>
   *
   * @param set the set
   * @param e   the element
   * @param <E> the set generic type
   * @return an <code>HashSet</code> containing the element
   */
  public static <E> HashSet<E> addToHashSet(Set<E> set, E e) {
    HashSet<E> hashSet;
    if (set instanceof HashSet) {
      hashSet = (HashSet<E>)set;
    }
    else {
      hashSet = new HashSet<E>(set);
    }
    hashSet.add(e);
    return hashSet;
  }

  /**
   * <p>Add the specified to the specified list and returns the result. When the <code>list</code> argument is an
   * instance of {@link ArrayList} the element is directly added, otherwise a new <code>ArrayList</code> is created by
   * cloning the <code>list</code> argument and the <code>e</code> argument is added.</p>
   * <p/>
   * <p>Usage pattern : adding a list to a non modifiable list</p>
   * <pre><code>
   *    List&lt;String&gt; list = Collections.emptyList();
   *    list = addToArrayList(list, "foo");
   * </code></pre>
   *
   * @param list the list
   * @param e    the element
   * @param <E>  the set generic type
   * @return an <code>ArrayList</code> containing the element
   */
  public static <E> ArrayList<E> addToArrayList(List<E> list, E e) {
    ArrayList<E> arrayList;
    if (list instanceof ArrayList) {
      arrayList = (ArrayList<E>)list;
    }
    else {
      arrayList = new ArrayList<E>(list);
    }
    arrayList.add(e);
    return arrayList;
  }

  public static <E> HashSet<E> set() {
    return new HashSet<E>();
  }

  public static <E> HashSet<E> set(E element) {
    HashSet<E> set = new HashSet<E>();
    set.add(element);
    return set;
  }

  public static <E> HashSet<E> set(E... elements) {
    HashSet<E> set = new HashSet<E>(elements.length);
    Collections.addAll(set, elements);
    return set;
  }

  public static <E> HashSet<E> set(Iterable<E> elements) {
    return set(elements.iterator());
  }

  public static <E> HashSet<E> set(Iterator<E> elements) {
    HashSet<E> list = new HashSet<E>();
    while (elements.hasNext()) {
      list.add(elements.next());
    }
    return list;
  }

  public static <E> HashSet<E> set(Enumeration<E> elements) {
    HashSet<E> list = new HashSet<E>();
    while (elements.hasMoreElements()) {
      list.add(elements.nextElement());
    }
    return list;
  }

  public static <E> ArrayList<E> list(Iterable<E> elements) {
    return list(elements.iterator());
  }

  public static <E> ArrayList<E> list(Iterator<E> elements) {
    ArrayList<E> list = new ArrayList<E>();
    while (elements.hasNext()) {
      list.add(elements.next());
    }
    return list;
  }

  public static <E> ArrayList<E> list(Enumeration<E> elements) {
    ArrayList<E> list = new ArrayList<E>();
    while (elements.hasMoreElements()) {
      list.add(elements.nextElement());
    }
    return list;
  }

  public static <E> ArrayList<E> list(E... elements) {
    ArrayList<E> set = new ArrayList<E>(elements.length);
    Collections.addAll(set, elements);
    return set;
  }


  public static <E> Iterable<E> iterable(final Enumeration<E> elements) throws NullPointerException {
    return new Iterable<E>() {
      public Iterator<E> iterator() {
        return Tools.iterator(elements);
      }
    };
  }

  public static <E> Iterator<E> iterator(final Enumeration<E> elements) throws NullPointerException {
    return new Iterator<E>() {
      public boolean hasNext() {
        return elements.hasMoreElements();
      }
      public E next() {
        return elements.nextElement();
      }
      public void remove() {
        throw new UnsupportedOperationException("Read only");
      }
    };
  }

  public static <E> Iterable<E> iterable(final E element) throws NullPointerException {
    return new Iterable<E>() {
      public Iterator<E> iterator() {
        return Tools.iterator(element);
      }
    };
  }

  public static <E> Iterator<E> iterator(final E element) throws NullPointerException {
    return new Iterator<E>() {
      boolean hasNext = true;
      public boolean hasNext() {
        return hasNext;
      }
      public E next() {
        if (hasNext) {
          hasNext = false;
          return element;
        } else {
          throw new NoSuchElementException();
        }
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public static class ArrayIterator<E> implements Iterator<E> {

    /** . */
    private final E[] elements;

    /** . */
    private final int to;

    /** . */
    private int current;

    ArrayIterator(E[] elements, int to, int current) throws NullPointerException, IndexOutOfBoundsException, IllegalArgumentException {
      if (elements == null) {
        throw new NullPointerException("No null elements accepted");
      }
      if (current < 0) {
        throw new IndexOutOfBoundsException("From index cannot be negative");
      }
      if (to > elements.length + 1) {
        throw new IndexOutOfBoundsException("To index cannot be greater than the array size + 1");
      }
      if (current > to) {
        throw new IllegalArgumentException("From index cannot be greater than the to index");
      }

      //
      this.elements = elements;
      this.current = current;
      this.to = to;
    }

    public boolean hasNext() {
      return current < to;
    }

    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return elements[current++];
    }

    public void remove() {
      throw new NoSuchElementException();
    }
  }

  public static class IterableArray<E> implements Iterable<E> {

    /** . */
    private final E[] elements;

    /** . */
    private final int from;

    /** . */
    private final int to;

    IterableArray(E[] elements, int from, int to) throws NullPointerException, IndexOutOfBoundsException, IllegalArgumentException {
      if (elements == null) {
        throw new NullPointerException("No null elements accepted");
      }
      if (from < 0) {
        throw new IndexOutOfBoundsException("From index cannot be negative");
      }
      if (to > elements.length + 1) {
        throw new IndexOutOfBoundsException("To index cannot be greater than the array size + 1");
      }
      if (from > to) {
        throw new IllegalArgumentException("From index cannot be greater than the to index");
      }

      //
      this.elements = elements;
      this.from = from;
      this.to = to;
    }

    public Iterator<E> iterator() {
      return new ArrayIterator<E>(elements, to, from);
    }
  }

  public static <E> Iterable<E> iterable(final E... elements) throws NullPointerException {
    return new IterableArray<E>(elements, 0, elements.length);
  }

  public static <E> Iterator<E> iterator(E... elements) throws NullPointerException {
    return new ArrayIterator<E>(elements, elements.length, 0);
  }

  /**
   * Create an iterable from the array and bounds
   *
   * @param elements the elements to wrap
   * @param from the from bound
   * @param to the to bound
   * @param <E> the element generic type
   * @return the iterable
   * @throws NullPointerException if the array is null
   * @throws IndexOutOfBoundsException when the bounds are outside of the array
   * @throws IllegalArgumentException if the from argument is greater than the to index
   */
  public static <E> IterableArray<E> iterable(E[] elements, int from, int to) throws NullPointerException, IndexOutOfBoundsException, IllegalArgumentException {
    return new IterableArray<E>(elements, from, to);
  }

  public static <E> Iterator<E> iterator(int from, final E... elements) throws NullPointerException, IndexOutOfBoundsException {
    return new ArrayIterator<E>(elements, elements.length, from);
  }

  public static <E> Iterable<E> iterable(final int from, final int to, final E... elements) throws NullPointerException, IndexOutOfBoundsException {
    return new IterableArray<E>(elements, from, to);
  }

  public static <E> Iterator<E> iterator(final int from, final int to, final E... elements) throws NullPointerException, IndexOutOfBoundsException {
    return new ArrayIterator<E>(elements, to, from);
  }

  public static <E> Iterator<E> emptyIterator() {
    @SuppressWarnings("unchecked")
    Iterator<E> iterator = EMPTY_ITERATOR;
    return iterator;
  }

  public static <E> Iterable<E> emptyIterable() {
    @SuppressWarnings("unchecked")
    Iterable<E> iterable = EMPTY_ITERABLE;
    return iterable;
  }

  public static <E> Iterator<E> append(final Iterator<E> iterator, final E... elements) {
    return new Iterator<E>() {

      /** -1 means the iterator should be used, otherwise it's the index. */
      int index = -1;

      public boolean hasNext() {
        if (index == -1) {
          if (iterator.hasNext()) {
            return true;
          }
          else {
            index = 0;
          }
        }
        return index < elements.length;
      }

      public E next() {
        if (index == -1) {
          if (iterator.hasNext()) {
            return iterator.next();
          }
          else {
            index = 0;
          }
        }
        if (index < elements.length) {
          return elements[index++];
        }
        else {
          throw new NoSuchElementException();
        }
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Append an object to an array of objects. The original array is not modified. The returned array will be of the
   * same component type of the provided array and its first n elements where n is the size of the provided array will
   * be the elements of the provided array. The last element of the array will be the provided object to append.
   *
   * @param array the array to augment
   * @param o     the object to append
   * @return a new array
   * @throws IllegalArgumentException if the array is null
   * @throws ClassCastException       if the appended object class prevents it from being added to the array
   */
  public static <E> E[] appendTo(E[] array, E o) throws IllegalArgumentException, ClassCastException
  {
    if (array == null)
    {
      throw new IllegalArgumentException();
    }

    //
    Class componentType = array.getClass().getComponentType();
    if (o != null && !componentType.isAssignableFrom(o.getClass()))
    {
      throw new ClassCastException("Object with class " + o.getClass().getName() + " cannot be casted to class " + componentType.getName());
    }

    //
    E[] copy = (E[])Array.newInstance(componentType, array.length + 1);
    System.arraycopy(array, 0, copy, 0, array.length);
    copy[array.length] = o;

    //
    return copy;
  }

  public static <S extends Serializable> S unserialize(Class<S> expectedType, File f) throws IOException, ClassNotFoundException {
    return unserialize(expectedType, new FileInputStream(f));
  }

  public static <S> S unserialize(Class<S> expectedType, InputStream in) throws IOException, ClassNotFoundException {
    try {
      ObjectInputStream ois = new ObjectInputStream(in);
      Object o = ois.readObject();
      return expectedType.cast(o);
    }
    finally {
      safeClose(in);
    }
  }

  public static <S extends Serializable> void serialize(S value, File f) throws IOException {
    serialize(value, new FileOutputStream(f));
  }


  public static <T extends Serializable> void serialize(T value, OutputStream out) throws IOException {
    ObjectOutputStream ois = new ObjectOutputStream(out);
    try {
      ois.writeObject(value);
    }
    finally {
      safeClose(out);
    }
  }

  public static <T extends Serializable> T clone(T value) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Tools.serialize(value, baos);
    byte[] bytes = baos.toByteArray();
    return (T)unserialize(Object.class, new ByteArrayInputStream(bytes));
  }

  public static int unsignedByteToInt(byte b) {
    return (int)b & 0xFF;
  }

  /**
   * Parses a date formatted as ISO 8601.
   *
   * @param date the date
   * @return the time in millis corresponding to the date
   */
  public static long parseISO8601(String date) {
    return DatatypeConverter.parseDateTime(date).getTimeInMillis();
  }

  /**
   * Format the time millis as an ISO 8601 date.
   *
   * @param timeMillis the time to format
   * @return the ISO 8601 corresponding dat
   */
  public static String formatISO8601(long timeMillis) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(timeMillis);
    return DatatypeConverter.printDateTime(c);
  }

  public static long handle(Element te) {
    long hash = 0;
    for (Element enclosed : te.getEnclosedElements()) {
      hash = 31 * hash + handle(enclosed);
    }
    hash = 31 * hash + te.getSimpleName().toString().hashCode();
    return hash;
  }

  public static int indexOf(CharSequence s, char c, int from) {
    return indexOf(s, c, from, s.length());
  }

  public static int indexOf(CharSequence s, char c, int from, int end) {
    if (from < end) {
      if (from < 0) {
        from = 0;
      }
      while (from < end) {
        if (s.charAt(from) == c) {
          return from;
        }
        from++;
      }
    }
    return -1;
  }

  public static int lastIndexOf(CharSequence s, char c) {
    return lastIndexOf(s, c, s.length() - 1);
  }

  public static int lastIndexOf(CharSequence s, char c, int from) {
    from = Math.min(from, s.length() - 1);
    while (from >= 0) {
      if (s.charAt(from) == c) {
        return from;
      } else {
        from--;
      }
    }
    return -1;
  }

  /**
   * Count the occurence of the separator string in the specified string with no overlapping.
   *
   * @param s the string to count in
   * @param separator the separator
   * @return the number of occurence
   */
  public static int count(String s, String separator) {
    if (separator.length() == 0) {
      return s.length() + 1;
    } else {
      int count = 0;
      int prev = 0;
      while (true) {
        int pos = s.indexOf(separator, prev);
        if (pos == -1) {
          break;
        } else {
          count++;
          prev = pos + separator.length();
        }
      }
      return count;
    }
  }

  public static String[] split(CharSequence s, char separator) {
    return foo(s, separator, 0, 0, 0);
  }

  public static String[] split(CharSequence s, char separator, int rightPadding) {
    if (rightPadding < 0) {
      throw new IllegalArgumentException("Right padding cannot be negative");
    }
    return foo(s, separator, 0, 0, rightPadding);
  }

  private static String[] foo(CharSequence s, char separator, int count, int from, int rightPadding) {
    int len = s.length();
    if (from < len) {
      int to = from;
      while (to < len && s.charAt(to) != separator) {
        to++;
      }
      String[] ret;
      if (to == len - 1) {
        ret = new String[count + 2 + rightPadding];
        ret[count + 1] = "";
      }
      else {
        ret = to == len ? new String[count + 1 + rightPadding] : foo(s, separator, count + 1, to + 1, rightPadding);
      }
      ret[count] = from == to ? "" : s.subSequence(from, to).toString();
      return ret;
    }
    else if (from == len) {
      return new String[count + rightPadding];
    }
    else {
      throw new AssertionError();
    }
  }

  public static AnnotationMirror getAnnotation(Element element, String annotationFQN) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(annotationFQN)) {
        return mirror;
      }
    }
    return null;
  }

  public static StringBuilder toString(Iterable<Map.Entry<String, String[]>> entries, StringBuilder sb) {
    sb.append('{');
    for (Iterator<Map.Entry<String, String[]>> i = entries.iterator();i.hasNext();) {
      Map.Entry<String, String[]> entry = i.next();
      sb.append(entry.getKey()).append("=[");
      String[] value = entry.getValue();
      for (int j = 0;j < value.length;j++) {
        if (j > 0) {
          sb.append(',');
        }
        sb.append(value[j]);
      }
      sb.append(']');
      if (i.hasNext()) {
        sb.append(',');
      }
    }
    sb.append('}');
    return sb;
  }

  public static String nextUUID() {
    return UUID.randomUUID().toString();
  }

  public static HashMap<String, String[]> toHashMap(Parameters parameters) {
    HashMap<String, String[]> map = new HashMap<String, String[]>();
    for (ResponseParameter parameter : parameters.values()) {
      map.put(parameter.getName(), parameter.toArray());
    }
    return map;
  }

  public static BigInteger bitSet(CharSequence s) {
    BigInteger current = BigInteger.ZERO;
    for (int i = s.length() - 1;i >= 0;i--) {
      char c = s.charAt(i);
      current = current.setBit(c);

    }
    return current;
  }

  public static BigInteger bitSet(char... chars) {
    BigInteger current = BigInteger.ZERO;
    for (char c : chars) {
      current = current.setBit(c);
    }
    return current;
  }

  public static String[] safeConcat(String[] first, String[] second) {
    if (first != null) {
      if (second != null) {
        String[] concat = new String[first.length + second.length];
        System.arraycopy(first, 0, concat, 0, first.length);
        System.arraycopy(second, 0, concat, first.length, second.length);
        return concat;
      } else {
        return first;
      }
    } else {
      if (second != null) {
        return second;
      } else {
        return EMPTY_STRING_ARRAY;
      }
    }
  }
}
