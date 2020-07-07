package decrypt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static String repeat(int count, String with) {
        if(count <= 0) return "";
        return new String(new char[count]).replace("\0", with);
    }


    public static String readFile(File filePath, String fileName) {
        return readFile(new File(filePath + "/" + fileName));
    }

    public static String readFile(File filename) {
        try {
            if(!filename.exists()) return "";

            return new String(Files.readAllBytes(Paths.get(filename.toString())));
        } catch (Exception e) {
            System.err.println("Unable to read file: " + filename);
            e.printStackTrace();
            System.exit(1);
        }
        return "";
    }

    public static void writeFile(String filename, String content) {
        try {
            Files.write( Paths.get(filename), content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getFileWithEnding(File dir, String ending) {
        File[] filesList = dir.listFiles();
        if (filesList != null) {
            for(File f : filesList){
                if(f.isFile() && f.getName().endsWith(ending)){
                    return f;
                }
            }
        }

        return null;
    }

    private static ArrayList<SyncWord> subList(List<SyncWord> list, int start, int end) {
        ArrayList<SyncWord> newList = new ArrayList<>();

        for(int i=start; i<=end; i++) {
            newList.add(list.get(i).clone());
        }

        return newList;
    }
}
