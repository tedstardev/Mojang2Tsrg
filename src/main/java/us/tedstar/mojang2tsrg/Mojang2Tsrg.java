package us.tedstar.mojang2tsrg;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Mojang2Tsrg {
    public Map<String, String> classMap;

    public Mojang2Tsrg() {
        classMap = new HashMap<>();
    }

    public String typeToDescriptor(String type) {
        if(type.endsWith("[]"))
            return "[" + typeToDescriptor(type.substring(0, type.length() - 2));
        if(type.contains("."))
            return "L" + classMap.getOrDefault(type.replaceAll("\\.", "/"), type.replaceAll("\\.", "/")) + ";";

        switch(type) {
            case "void": return "V";
            case "int": return "I";
            case "float": return "F";
            case "char": return "C";
            case "byte": return "B";
            case "boolean": return "Z";
            case "double": return "D";
            case "long": return "J";
            case "short": return "S";
            default: return "";
        }
    }

    public void loadClasses(File map) throws IOException {
        FileReader reader = new FileReader(map);
        BufferedReader buf = new BufferedReader(reader);

        boolean loop = true;
        while(loop) {
            String s = buf.readLine();

            if (s != null && !s.isEmpty()) {
                if (s.startsWith("#")) continue;

                if (s.startsWith(" ")) // We only care about lines mapping classes.
                    continue;
                else { // Read the class name into the map.
                    String parts[] = s.split(" ");
                    assert parts.length == 3;

                    String className = parts[0].replaceAll("\\.", "/");
                    String obfName = parts[2].substring(0, parts[2].length() - 1);
                    if(obfName.contains("."))
                        obfName = obfName.replaceAll("\\.", "/");

                    classMap.put(className, obfName);
                }
            } else loop = false;
        }

        buf.close();
        reader.close();
    }

    public void writeTsrg(File map, File out) throws IOException {
        FileReader reader = new FileReader(map);
        FileWriter writer = new FileWriter(out);
        BufferedReader txt = new BufferedReader(reader);
        BufferedWriter buf = new BufferedWriter(writer);

        boolean loop = true;
        while(loop) {
            String s = txt.readLine();
            if (s != null && !s.isEmpty()) {
                if (s.startsWith("#")) continue;

                if (s.startsWith(" ")) { // This is a field or a method.
                    s = s.substring(4);
                    String parts[] = s.split(" ");
                    assert parts.length == 4;

                    if(parts[1].endsWith(")")) { // This is a method.
                        String returnType = parts[0].contains(":") ? parts[0].split(":")[2] : parts[0]; // Split line numbers.
                        String obfName = parts[3];
                        String methodName = parts[1].split("\\(")[0]; // Separate params from name.
                        String params = parts[1].split("\\(")[1];
                        params = params.substring(0, params.length() - 1);

                        returnType = typeToDescriptor(returnType);
                        params = Arrays.stream(params.split(",")).map(this::typeToDescriptor).collect(Collectors.joining());

                        if(!methodName.equals("<init>") && !methodName.equals("<clinit>"))
                            buf.write("\t" + obfName + " (" + params + ")" + returnType + " " + methodName + "\n");
                    } else { // This is a field.
                        String fieldName = parts[1];
                        String obfName = parts[3];

                        buf.write("\t" + obfName + " " + fieldName + "\n");
                    }
                } else { // Classes have no dependencies.
                    String parts[] = s.split(" ");
                    assert parts.length == 3;

                    String className = parts[0].replaceAll("\\.", "/");
                    String obfName = parts[2].substring(0, parts[2].length() - 1);
                    if(obfName.contains("."))
                        obfName = obfName.replaceAll("\\.", "/");

                    buf.write(obfName + " " + className + "\n"); // Write class entry.
                }
            } else loop = false;
        }

        buf.close();
        txt.close();
        writer.close();
        reader.close();
    }

    public static void main(String[] args) throws IOException {
        File map = new File(args[0]);
        File out = new File(args[1]);

        Mojang2Tsrg m2t = new Mojang2Tsrg();
        m2t.loadClasses(map);
        m2t.writeTsrg(map, out);
    }
}
