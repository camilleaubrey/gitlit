import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

public class Git {

    public static void main(String[] args) {
        // Test with two files: test and hello
        init();
        add("testing/test.txt");
        add("testing/hello.txt");
        add("testing/subfolder/test.txt");
    }

    public static void init() {
        boolean existed = false;
        if (new File("./git").exists() && new File("./git/objects").exists()
                && new File("./git/index").exists() && new File("./git/HEAD").exists()) {
            existed = true;
        }

        File gitDirectory = new File("./git");
        if (!gitDirectory.exists()) {
            gitDirectory.mkdir();
        }

        File objectsDirectory = new File("./git/objects");
        if (!objectsDirectory.exists()) {
            objectsDirectory.mkdir();
        }

        File indexFile = new File("./git/index");
        if (!indexFile.exists()) {
            try {
                indexFile.createNewFile();
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        File headFile = new File("./git/HEAD");
        if (!headFile.exists()) {
            try {
                headFile.createNewFile();
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        if (existed == true) {
            System.out.println("Git Repository Already Exists");
        } else {
            System.out.println("Git Repository Created");
        }
    }


    // Copied hashFile from fileHasher.java
    public static String hashFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(path);
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            throw new RuntimeException("Something's not working in MessageDigest.");
        }

        // digest.update(fileBytes);
        byte[] encodedHash = digest.digest(fileBytes);
        String hashString = HexFormat.of().formatHex(encodedHash);
        System.out.println(hashString);
        return hashString;
    }

    public static void add(String filePath) {
        try {
            String hashString = hashFile(filePath);
            addToObjects(hashString, filePath);
            addToIndex(hashString, filePath);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void addToObjects(String hashString, String filePath) {
        // Make new file in objects folder titled the hashString hash
        File newFile = new File("./git/objects/" + hashString);

        // If the file already exists, meaning same contents already saved, do nothing
        if (newFile.exists()) {
            return;
        }

        try {
            newFile.createNewFile();

            // Copy contents from original file and write them into the object
            String content = Files.readString(Paths.get(filePath));
            FileWriter writer1 = new FileWriter("./git/objects/" + hashString);
            writer1.write(content);
            writer1.close();

            // Use below to test if works:
            // String file1content = Files.readString(Paths.get("./git/objects/" + hashString));
            // System.out.println(file1content);
        } catch (Exception e) {
            System.out.println("Failed to create a new file: " + e);
        }
    }

    public static void addToIndex(String hashString, String filePath) {
        Path indexPath = Paths.get("./git/index");
        try {
            // Reads all contents of index file into an arraylist of arraylists
            boolean exists = false;
            ArrayList<ArrayList<String>> indexContents = new ArrayList<>();
            for (String line : Files.readAllLines(indexPath)) {
                String[] partsOfLine = line.split(" ", 3);
                ArrayList<String> lineInIndex = new ArrayList<>(); // ArrayList of each line in
                                                                   // index
                lineInIndex.add(partsOfLine[0]);
                lineInIndex.add(partsOfLine[1]);
                indexContents.add(lineInIndex);
            }

            // Checks if already exists by going through each arraylist containing each line of
            // index file
            for (ArrayList<String> lineInIndex : indexContents) {
                if (lineInIndex.get(1).equals(filePath)) {
                    lineInIndex.set(0, hashString);
                    exists = true;
                    break;
                }
                // else if (lineInIndex.get(0).equals(hashString)) {
                // lineInIndex.set(1, filePath);
                // exists = true;
                // break;
                // }
            }

            // If the arraylist containing the file name doesn't already exist, add to arraylist of
            // lines called indexContents
            if (exists == false) {
                ArrayList<String> lineInIndex = new ArrayList<>();
                lineInIndex.add(hashString);
                lineInIndex.add(filePath);
                indexContents.add(lineInIndex);
            }

            // Add indexContents back to actual file
            ArrayList<String> fileLines = new ArrayList<>();
            for (ArrayList<String> lineInIndex : indexContents) {
                fileLines.add(lineInIndex.get(0) + " " + lineInIndex.get(1));
            }
            Files.writeString(indexPath, String.join("\n", fileLines));

        } catch (Exception e) {
            System.out.println("This didn't work because " + e);
        }

    }

    public static String createTree(ArrayList<String> workingList, String dirPath) {
        StringBuilder treeContent = new StringBuilder();

        // loop through workingList line by line
        for (String line : workingList) {
            String[] lineParts = line.split(" ", 3);

            // different parts of each line
            String type = lineParts[0]; // either blob or tree
            String hash = lineParts[1];
            String fullPath = lineParts[2];

            int lastSlashIndex = fullPath.lastIndexOf('/');
            String parentName;
            String childName;

            // check if staged files are in the folder we're looking for
            if (lastSlashIndex == -1) {
                parentName = "";
                childName = fullPath;
            } else {
                parentName = fullPath.substring(0, lastSlashIndex);
                childName = fullPath.substring(lastSlashIndex + 1);
            }
            if (parentName.equals(dirPath)) {
                treeContent.append(type + " " + hash + " " + childName + "\n");
            }
        }

        try {
            // hashing the result
            byte[] contentBytes = treeContent.toString().getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String treeHash = HexFormat.of().formatHex(digest.digest(contentBytes));

            Path outputPath = Path.of("./git/objects", treeHash);
            if (!Files.exists(outputPath)) {
                Files.write(outputPath, contentBytes);
            }

            return treeHash;
        } catch (Exception e) {
            throw new RuntimeException("Tree creation error", e);
        }
    }

    public static String createTreeFromIndex() {
        Path indexPath = Paths.get("./git/index");
        ArrayList<String> workingList = new ArrayList<>();

        try {
            // read index and copy over to workingList with the prefix
            for (String indexLine : Files.readAllLines(indexPath)) {
                if (!indexLine.isBlank()) {
                    workingList.add("blob " + indexLine);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("workingList creation error", e);
        }

        // loop until whole tree is collapsed
        while (true) {
            // sort based on path
            workingList.sort(Comparator.comparing(line -> line.split(" ", 3)[2]));

            // find the deepest directory in workingList
            String deepestDirectory = "";
            int deepestLevel = -1;
            for (String line : workingList) {
                String[] lineParts = line.split(" ", 3);
                String fullPath = lineParts[2];

                int lastSlashIndex = fullPath.lastIndexOf('/');

                // assign parent before last slash
                String parentName;
                if (lastSlashIndex == -1) {
                    parentName = "";
                } else {
                    parentName = fullPath.substring(0, lastSlashIndex);
                }

                // assign depth of parent
                int directoryDepth;
                if (parentName.isEmpty()) {
                    directoryDepth = 0;
                } else {
                    directoryDepth = parentName.split("/").length;
                }

                // if we found a deeper level, update integer deepestLevel
                if (directoryDepth > deepestLevel) {
                    deepestLevel = directoryDepth;
                    deepestDirectory = parentName;
                }
            }

            // run createTree on the deepest directory
            String treeHash = createTree(workingList, deepestDirectory);

            // end if deepest directory is root
            if (deepestDirectory.isEmpty()) {
                return treeHash;
            }

            // collapse the deepest directory and replace it as a tree entry
            ArrayList<String> collapsedList = new ArrayList<>();
            for (String line : workingList) {
                String[] lineParts = line.split(" ", 3);
                String fullPath = lineParts[2];
                int lastSlashIndex = fullPath.lastIndexOf('/');

                // if line is not in that deepest directory then add to collapsedList
                String parentName;
                if (lastSlashIndex == -1) {
                    parentName = "";
                } else {
                    parentName = fullPath.substring(0, lastSlashIndex);
                }
                if (!parentName.equals(deepestDirectory)) {
                    collapsedList.add(line);
                }
            }

            // add the tree
            collapsedList.add("tree " + treeHash + " " + deepestDirectory);
            workingList = collapsedList;
        }
    }

    public static String saveCommit(String author, String message) throws IOException {
        String hash = createTreeFromIndex();
        File tempCommit = File.createTempFile("commit", ".tmp");
        PrintWriter pw = new PrintWriter(tempCommit);
        pw.write("tree: " + hash + "\n");
        String head = Files.readString(Path.of("git/HEAD"));
        if (!head.isEmpty()) {
            pw.write("parent: " + head + "\n");
        }
        pw.write("author: " + author + "\n");
        pw.write("date: " + LocalDateTime.now() + "\n");
        pw.write("message: " + message);
        pw.close();

        //copied from hashFile above
        byte[] fileBytes = Files.readAllBytes(tempCommit.toPath());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            throw new RuntimeException("Something's not working in MessageDigest.");
        }
        // digest.update(fileBytes);
        byte[] encodedHash = digest.digest(fileBytes);
        String hashString = HexFormat.of().formatHex(encodedHash);

        File commitFile = new File("git/objects/" + hashString);
        commitFile.createNewFile();
        Files.copy(tempCommit.toPath(), commitFile.toPath());
        Files.writeString(Path.of("git/HEAD"), hashString);
        tempCommit.delete();
        return hashString;
    }

    public static void regenIndex(String treeHash, File tempIndex, String parent) throws IOException {
        List<String> objects = Files.readAllLines(Path.of("git/objects/" + treeHash));
        for (String line : objects) {
            String[] parts = line.split(" ");
            String path = "";
            if (parent == "") {
                path = parts[2];
            } else {
                path = parent + "/" + parts[2];
            }
            if (parts[0].equals("blob")) {
                String newIndex = (parts[1] + " " + path);
                Files.writeString(tempIndex.toPath(), newIndex + "\n", StandardOpenOption.APPEND);
            } else if (parts[0].equals("tree")) {
                regenIndex(parts[1], tempIndex, path);
            }
        }
    }
}
