import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static String propertyfilePath;
    private  static String treatedFile = "treated.txt";
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        try {
            propertyfilePath = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {

        }

        Optional.ofNullable(propertyfilePath)
                .orElseThrow(() -> new Exception("Property file must be set"));

        try (InputStream input = new FileInputStream(propertyfilePath)) {

            Properties prop = new Properties();
            prop.load(input);

            String sourcePaths = getProperties(prop, "sourceFolderPaths");
            String cropOffset = getProperties(prop, "cropOffset");
            String maxSize = getProperties(prop, "maxSize");

            Map<String, String> sourceDestination = getPathsFromString(sourcePaths);

            List<String> stringSet = new ArrayList<>(sourceDestination.keySet());
            Collections.shuffle(stringSet);
            //VideoUtilities.splitAndCombineVideo("D:\\p\\FuckableGirlsSite\\Hidori Rose\\hidorirose-08-07-2020-77702782-Race Queen Atago blowjob and self pleasure.mp4", VideoUtilities.FileFormat.MP4, 50, false);
            for (String sourcePath : stringSet) {

                File sourceFoler = new File(sourcePath);
                String destinationPath = sourceDestination.get(sourcePath);
                try {
                List<String> allTreatedPaths = VideoUtilities.getFileLines(destinationPath+"//"+treatedFile);
                    if (sourceFoler.exists() && sourceFoler.isDirectory()) {

                        for (File file : Arrays.stream(Objects.requireNonNull(sourceFoler.listFiles()))
                                .filter(f -> f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".png")))
                                .filter(f -> !allTreatedPaths.contains(f.getAbsolutePath()))
                                .limit(10)
                                .collect(Collectors.toList())) {

                            LOGGER.info("Start processing file " + file.getAbsolutePath());
                            try {
                                PhotoUtilities.cropImage(60, file, destinationPath);
                            }catch (Exception e) {
                                e.printStackTrace();
                            }

                            VideoUtilities.addLineToFile(new File(destinationPath + "//" + treatedFile), file.getAbsolutePath());
                        }


                        List<File> collect = Arrays.stream(Objects.requireNonNull(sourceFoler.listFiles()))
                                .filter(f -> f.getName().endsWith(".mp4") || f.getName().endsWith(".mov") ||f.getName().endsWith(".ts"))
                                .filter(f -> !allTreatedPaths.contains(f.getAbsolutePath()))
                                .limit(5)
                                .collect(Collectors.toList());
                        Collections.shuffle(collect);
                        for (File file : collect) {

                                LOGGER.info("Start processing file " + file.getAbsolutePath());

                                LOGGER.info("Start cropping file " + file.getAbsolutePath());
                                String croppedDestination = destinationPath + "/cropped";
                                createFolderIfNotExists(croppedDestination);
                                String filePath = VideoUtilities.cropVideo(file.getAbsolutePath(), croppedDestination, Integer.parseInt(cropOffset));
                                file = new File(filePath);

                                LOGGER.info("Finish cropping file " + file.getAbsolutePath());

                                if (VideoUtilities.getFileSize(file) > Integer.parseInt(maxSize)) {
                                    LOGGER.info("Start split and combine file " + file.getAbsolutePath());
                                    try {
                                        VideoUtilities.splitAndCombineVideo(file.getAbsolutePath(), VideoUtilities.FileFormat.MP4, Integer.parseInt(maxSize), destinationPath, true);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    LOGGER.info("Finished split and combine file " + file.getAbsolutePath());
                                } else {
                                    Files.move(Paths.get(file.getAbsolutePath()), Paths.get(destinationPath + "/" + file.getName()), StandardCopyOption.REPLACE_EXISTING);
                                }

                            VideoUtilities.addLineToFile(new File(destinationPath + "//" + treatedFile), file.getAbsolutePath());
                        }

                    }
                } catch (Exception e) {
                    LOGGER.error("An error occur while processing file " + sourcePath);
                    e.printStackTrace();
                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private static String getProperties(Properties prop, String key) throws Exception {
        if(prop!= null && prop.getProperty(key) != null && !prop.getProperty(key).isEmpty()) {
            LOGGER.info("Loading property "+key+" = "+prop.getProperty(key));
            return prop.getProperty(key);
        }

        throw new Exception("You must set the property : "+ key);
    }

    public static void createFolderIfNotExists(String folderPath) {
        if(!new File(folderPath).exists()) {
            new File(folderPath).mkdir();
        }
    }

    static Map<String, String> getPathsFromString(String paths) {
        Map<String, String> result = new HashMap<>();
        String[] split = paths.split("\\|");
        for(String part1: split) {
            String[] sourceDestinationPart = part1.split("=");
            result.put(sourceDestinationPart[0].trim(), sourceDestinationPart[1].trim());
        }

        return result;
    }
}

