init(): This method checks to see if a directory ./git, directory objects, file HEAD, and file index, exists. If not, it creates them. In this order:
    ./git
    ./git/objects
    ./git/index
    ./git/HEAD


hashFile(String filePath): This method takes in a filePath as a paramater and reads its content as bytes. It then uses MessageDigest to determine the file's unique SHA-1 hash. The result is converted into a hexadecimal String.

add(String filePath): this method creates hash, then runs "addToObjects()" then "addToIndex" in one package to complete staging.

addToObjects(Strign hashString, String filePath): This method creates a file in ./git/objects named the file's SHA-1 hexidecimal hash and then puts the file's original contents into it. This is called a blob because the file has no specific format. If a blob with that hash already exists, it returns the method early since it means the same content hash already been saved. 

addToIndex(String hashString, String filePath): This method reads every line of ./git/index into an ArrayList of ArrayLists, where each ArrayList holds [hash, path]. If the file's path is already in the index, it updates the hash with the new content. If the path isn't there, it adds a new entry into the indexContents ArrayList. It then reads through the indexContents and joins everything together as a string to be added to the index file. 

createTree(ArrayList<String> workingList, String dirPath): This method creates one tree object for the directory specified by dirPath. It loops through the working list and selects entries whose immediate parent directory exactly matches dirPath. Each selected entry is written to the tree using its type, hash, and final file or subdirectory name. The method hashes the completed tree content with SHA-1, writes the content to ./git/objects using the hash as the filename, and returns the tree hash. It uses only the working list and does not scan files from the working directory.

createTreeFromIndex(): This method creates the complete tree structure represented by the staged files in ./git/index. It copies each index entry into a working list with the prefix "blob" and sorts the entries by path. It repeatedly finds the deepest unfinished directory, calls createTree() to write that directory's tree object, and replaces the directory's direct children with one "tree" entry containing the returned hash. The process moves upward one directory at a time until it creates the root tree. An empty directory path represents the root, and the method returns the root tree's SHA-1 hash for use by a future commit method.
