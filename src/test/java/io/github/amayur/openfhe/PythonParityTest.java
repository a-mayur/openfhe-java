package io.github.amayur.openfhe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// everything in python-api.txt (tools/extract_python_api.py) must exist in the Java API
public class PythonParityTest {
    private static final Map<String, String> DUNDER = Map.of(
            "__str__", "toString", "__repr__", "toString", "__add__", "add", "__eq__", "equals", "__ne__", "equals");
    private static final Map<String, String> CLASSES = Map.of("SERJSON", "SerType", "SERBINARY", "SerType");

    public static List<String> findMissing() throws Exception {
        List<String> missing = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                PythonParityTest.class.getResourceAsStream("/python-api.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] f = line.split("\\|", -1);
                String kind = f[0], pyClass = f[1], name = f[2];
                Integer lo = f[3].isEmpty() ? null : Integer.valueOf(f[3]);
                Integer hi = f[4].isEmpty() ? null : Integer.valueOf(f[4]);
                List<String> types = f[5].isEmpty() ? null : Arrays.asList(f[5].split(","));
                Class<?> cls = javaClass(pyClass);
                if (cls == null) {
                    missing.add("class " + pyClass + "  (needed by " + line + ")");
                    continue;
                }
                switch (kind) {
                    case "class":
                        break;
                    case "constant":
                        if (!hasConstant(cls, name)) missing.add("constant " + name);
                        break;
                    case "field":
                        if (find(cls, name, false, 0, null) == null) missing.add("field accessor " + pyClass + "." + name + "()");
                        break;
                    case "init":
                        if (!hasConstructor(cls, lo)) missing.add("constructor " + pyClass + "(" + lo + " args)");
                        break;
                    default:
                        boolean isStatic = !kind.equals("method");
                        String javaName = DUNDER.getOrDefault(name, name);
                        if (lo == null) {
                            if (find(cls, javaName, isStatic, null, null) == null)
                                missing.add(kind + " " + pyClass + "." + name);
                        } else {
                            for (int n = lo; n <= hi; n++) {
                                List<String> t = types == null ? null : types.subList(0, n);
                                if (find(cls, javaName, isStatic, n, t) == null)
                                    missing.add(kind + " " + pyClass + "." + name + "(" + (t == null ? n + " args" : String.join(", ", t)) + ")");
                            }
                        }
                }
            }
        }
        return missing;
    }

    private static Class<?> javaClass(String pyClass) {
        try {
            return Class.forName("io.github.amayur.openfhe." + CLASSES.getOrDefault(pyClass, pyClass), false,
                    PythonParityTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean hasConstant(Class<?> cls, String name) {
        try {
            Field fld = cls.getField(name);
            return Modifier.isStatic(fld.getModifiers());
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static boolean hasConstructor(Class<?> cls, int n) {
        for (Constructor<?> c : cls.getConstructors())
            if (c.getParameterCount() == n) return true;
        return false;
    }

    private static Method find(Class<?> cls, String name, boolean isStatic, Integer arity, List<String> types) {
        for (Method m : cls.getMethods()) {
            if (!m.getName().equals(name) || Modifier.isStatic(m.getModifiers()) != isStatic) continue;
            if (m.getDeclaringClass() == Object.class && !name.equals("toString") && !name.equals("equals")) continue;
            if (arity != null && m.getParameterCount() != arity) continue;
            if (types != null) {
                Class<?>[] p = m.getParameterTypes();
                boolean ok = true;
                for (int i = 0; i < p.length && ok; i++) ok = p[i].getSimpleName().equals(types.get(i));
                if (!ok) continue;
            }
            return m;
        }
        return null;
    }

    @Test
    public void javaApiCoversPythonApi() throws Exception {
        List<String> missing = findMissing();
        assertTrue(missing.isEmpty(), missing.size() + " Python API entries missing in Java:\n  " + String.join("\n  ", missing));
    }

    public static void main(String[] args) throws Exception {
        List<String> missing = findMissing();
        missing.forEach(System.out::println);
        System.out.println(missing.size() + " missing");
    }
}
