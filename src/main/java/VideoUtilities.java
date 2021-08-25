import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VideoUtilities {

    protected static Logger LOGGER = LoggerFactory.getLogger(VideoUtilities.class);
    public enum FileFormat {
        MP4(".mp4"),
        MOV(".mov"),
        TS(".TS"),
        MPEG(".mpeg"),
        MKV(".mkv"),
        M4A(".avi"),
        FLV(".flv");

        FileFormat(String extension) {
            this.extension = extension;
        }
        String extension;

    }

    private static AudioAttributes getAudioAttributes() {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(128000);
        audio.setChannels(2);
        audio.setSamplingRate(44100);
        return audio;
    }

    private static AudioAttributes getAudioAttributesLibfaac() {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("aac");
        audio.setBitRate(128000);
        audio.setSamplingRate(44100);
        audio.setChannels(2);
        return audio;
    }


    private static void cropvideo(VideoAttributes videoAttributes, String path, int offsetCropFromBottom) {
        VideoSize videoSize = getVideoSize(path);
        if(videoSize != null) {
            int videoHeigt = videoSize.getHeight();
            int videoweight = videoSize.getWidth();
            VideoFilter videoFilter = new VideoFilter("crop=" + videoweight + ":" + (videoHeigt - offsetCropFromBottom) + ":" + 0 + ":" + 0);
            videoAttributes.addFilter(videoFilter);
        }
    }

    private static void deleteLogo(VideoAttributes videoAttributes, String path) {
        VideoSize videoSize = getVideoSize(path);
        if(videoSize != null) {
            int videoHeigt = videoSize.getHeight() - 70;
            int videoweight = videoSize.getWidth() - 40;
            VideoFilter videoFilter = new VideoFilter("delogo=x=30:y=" + videoHeigt + ":w=" + videoweight + ":h=60");
            videoAttributes.addFilter(videoFilter);
        }
    }

    private static void blur(VideoAttributes videoAttributes) {
        VideoFilter videoFilter = new VideoFilter("[0:v]boxblur=luma_radius=180:chroma_radius=40:luma_power=2[blurred]");
        videoAttributes.addFilter(videoFilter);
    }

    private static VideoAttributes getVideoAttributes(String website, String path, boolean deleteLogo) {
        VideoAttributes video = new VideoAttributes();
        if(deleteLogo) {
            deleteLogo(video, path);
        }
        return video;
    }

    private static VideoAttributes getVideoAttributes() {
        VideoAttributes video = new VideoAttributes();
        video.setCodec(VideoAttributes.DIRECT_STREAM_COPY);
        return video;
    }

    private static VideoAttributes getVideoAttributes(String encoder) {
        VideoAttributes video = new VideoAttributes();
        video.setCodec(encoder);
        video.setBitRate(160000);
        video.setFrameRate(15);
        return video;
    }


    public static String cutVideo(String sourcePath, FileFormat fileFormat) {
        return  cutVideo( sourcePath, 0f, null, false, fileFormat);
    }

    public static String cutVideo(String sourcePath, Float offset, FileFormat fileFormat) {
        return  cutVideo( sourcePath, offset, null, false, fileFormat);
    }

    public static String cutVideo(String sourcePath, Float offset, String website, boolean deleteLogo, FileFormat fileFormat) {
        File source = new File(sourcePath);

        File target = new File(sourcePath.replace(fileFormat.extension, "_reduced"+ FileFormat.MP4.extension));

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setVideoAttributes(getVideoAttributes(website, sourcePath, deleteLogo));
        attrs.setAudioAttributes(getAudioAttributes());
        attrs.setDuration(90f);
        attrs.setOffset(offset);

        Encoder instance = new Encoder();

        try {

            instance.encode(new MultimediaObject(source), target, attrs, null);
        } catch (EncoderException e) {
            e.printStackTrace();
        }

        return target.getAbsolutePath();
    }

    public static String addWaterMark(String sourcePath, String destination, String website) {
        File source = new File(sourcePath);

        File target = new File(destination+"\\"+sourcePath.substring(sourcePath.lastIndexOf("\\")+1, sourcePath.lastIndexOf("."))+"_watermarked"+sourcePath.substring(sourcePath.lastIndexOf(".")));

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setVideoAttributes(getVideoAttributes(website, sourcePath, false));
        attrs.setAudioAttributes(getAudioAttributes());

        Encoder instance = new Encoder();

        try {

            instance.encode(new MultimediaObject(source), target, attrs, null);
        } catch (EncoderException e) {
            e.printStackTrace();
        }

        return target.getAbsolutePath();
    }



    public static String cropVideo(String sourcePath, String destination, int offsetCropFromBottom) {
        File source = new File(sourcePath);

        File target = new File(destination+"\\"+sourcePath.substring(sourcePath.lastIndexOf("\\")+1, sourcePath.lastIndexOf("."))+System.currentTimeMillis()+sourcePath.substring(sourcePath.lastIndexOf(".")));

        EncodingAttributes attrs = new EncodingAttributes();
        VideoAttributes videoAttributes = new VideoAttributes();
        cropvideo(videoAttributes, sourcePath, offsetCropFromBottom);
        attrs.setVideoAttributes(videoAttributes);
        attrs.setAudioAttributes(getAudioAttributes());

        Encoder instance = new Encoder();

        try {

            instance.encode(new MultimediaObject(source), target, attrs, null);
        } catch (EncoderException e) {
            e.printStackTrace();
        }

        return target.getAbsolutePath();
    }

    public static String blurVideo(String sourcePath, String destination) {
        File source = new File(sourcePath);

        File target = new File(destination+"\\"+sourcePath.substring(sourcePath.lastIndexOf("\\")+1, sourcePath.lastIndexOf("."))+"_blur"+sourcePath.substring(sourcePath.lastIndexOf(".")));



        EncodingAttributes attrs = new EncodingAttributes();
        VideoAttributes videoAttributes = new VideoAttributes();
        blur(videoAttributes);
        attrs.setAudioAttributes(getAudioAttributes());
        attrs.setVideoAttributes(videoAttributes);



        Encoder instance = new Encoder();

        try {

            instance.encode(new MultimediaObject(source), target, attrs, null);
        } catch (EncoderException e) {
            e.printStackTrace();
        }

        return target.getAbsolutePath();
    }

    public static String cutVideo(List<String> sourcePaths, File target) {

        List<MultimediaObject> multimediaObjects = new ArrayList<>();

        for(String paths : sourcePaths) {
            multimediaObjects.add(new MultimediaObject(new File(paths)));
        }

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setVideoAttributes(getVideoAttributes());
        attrs.setAudioAttributes(getAudioAttributesLibfaac());
        attrs.setFormat("mp4");


        Encoder instance = new Encoder();

        try {

            instance.encode(multimediaObjects, target, attrs);
        } catch (EncoderException e) {
            e.printStackTrace();
        }

        return target.getAbsolutePath();
    }

    public static String splitAndCombineVideo(String sourcePath, FileFormat fileFormat, double maxSize, boolean deleteAfter) {

        if(new File(sourcePath).length() == 0) {
            return  null;
        }
        List<String> allChunkPaths = getAllGeneratedChunksPaths( sourcePath, 10, fileFormat);
        if(allChunkPaths.isEmpty()) {
            return  null;
        }
        List<String> toDelete = new ArrayList<>(allChunkPaths);
        allChunkPaths.remove(0);

        Collections.shuffle(allChunkPaths);
        int numberOfChunks = 5;

        String targetPath = cutVideo(allChunkPaths.stream().limit(numberOfChunks).sorted().collect(Collectors.toList()), new File(sourcePath.replace(fileFormat.extension, "_reduced"+ FileFormat.MP4.extension)));

        while(getFileSize(new File(targetPath)) >= maxSize) {
            numberOfChunks--;
            targetPath = cutVideo(allChunkPaths.stream().limit(numberOfChunks).sorted().collect(Collectors.toList()), new File(sourcePath.replace(fileFormat.extension, "_reduced"+ FileFormat.MP4.extension)));
        }
        toDelete.forEach(path -> new File(path).delete());

        if(!sourcePath.contains("_reduced") && deleteAfter) {
            new File(sourcePath).delete();
        }
        return targetPath;
    }

    public static String splitAndCombineVideo(String sourcePath, FileFormat fileFormat, boolean deleteAfter) {
        return splitAndCombineVideo(sourcePath, fileFormat, 50.0,  deleteAfter);
    }

    public static String splitAndCombineVideo(String sourcePath, FileFormat fileFormat, double maxSize, String targetFolder, boolean deleteAfter) {

        List<String> allChunkPaths = getAllGeneratedChunksPaths( sourcePath, 30, fileFormat);
        List<String> toDelete = new ArrayList<>(allChunkPaths);
        if(allChunkPaths.size() > 1) {
            allChunkPaths.remove(0);
        }

        Collections.shuffle(allChunkPaths);
        int numberOfChunks = 3;

        String targetPath = cutVideo(allChunkPaths.stream().limit(numberOfChunks).sorted().collect(Collectors.toList()), new File(targetFolder +"/"+ new File(sourcePath).getName().replace(fileFormat.extension, "_"+System.currentTimeMillis()+ FileFormat.MP4.extension)));

        while(getFileSize(new File(targetPath)) >= maxSize && numberOfChunks > 1) {
            numberOfChunks--;
            toDelete.add(targetPath);
            targetPath = cutVideo(allChunkPaths.stream().limit(numberOfChunks).sorted().collect(Collectors.toList()), new File(targetFolder +"/"+ new File(sourcePath).getName().replace(fileFormat.extension, "_"+System.currentTimeMillis()+ FileFormat.MP4.extension)));

        }
        String finalTargetPath = targetPath;
        toDelete
                .stream().filter(f -> !f.equals(finalTargetPath))
                .forEach(path -> new File(path).delete());
        if(!sourcePath.contains("_reduced") && deleteAfter) {
            if(new File(sourcePath).delete()) {
                System.out.println("deleted");
            } else {
                System.out.println("not deleted");
            }
        }
        return targetPath;
    }

    public static List<String> getAllGeneratedChunksPaths(String sourcePath, float duration, FileFormat fileFormat) {

        List<Long> allChunksOffsets = getAllVideoOffsets(getVideoDurationInSeconds(sourcePath), (long) duration);
        List<String> allChunksPaths = new ArrayList<>();
        if (allChunksOffsets.size() > 2) {
            for (int i = 0; i < allChunksOffsets.size(); i++) {
                File target = new File(sourcePath.replace(fileFormat.extension, "_reduced" + i + FileFormat.TS.extension));
                if (target.exists()) {
                    allChunksPaths.add(target.getAbsolutePath());
                } else {
                    allChunksPaths.add(getVideoChunk(sourcePath, duration, (float) allChunksOffsets.get(i), target));
                }
            }
            return allChunksPaths;
        }

        return Collections.singletonList(cutVideo(sourcePath, fileFormat));
    }

    public static String getVideoChunk(String sourcePath, float duration, float offset, File target) {
        File source = new File(sourcePath);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setVideoAttributes(getVideoAttributes());

        AudioAttributes audioAttributes = getAudioAttributes();

        attrs.setAudioAttributes(audioAttributes);

        attrs.setDuration(duration);
        attrs.setOffset(offset);

        Encoder instance = new Encoder();

        try {

            instance.encode(new MultimediaObject(source), target, attrs, null);
        } catch (EncoderException e) {
            e.printStackTrace();
        }

        return target.getAbsolutePath();
    }

    public static Long getVideoDurationInSeconds(String sourcePath) {
        File source = new File(sourcePath);
        try {
            MultimediaObject mmObject= new MultimediaObject(source);
            MultimediaInfo infos= mmObject.getInfo();
            return TimeUnit.MILLISECONDS.toSeconds(infos.getDuration());
        } catch (Exception e) {
            LOGGER.error("Invalid format file "+ sourcePath);
        }

        return null;
    }

    public static List<Long> getAllVideoOffsets(Long duration, Long chunk) {

        List<Long> result = new ArrayList();

        for (long i = 0; i < duration; i = i + chunk) {
            result.add(i);
        }

        return result;
    }


    public static VideoSize getVideoSize(String sourcePath) {
        File source = new File(sourcePath);

        try {
            MultimediaObject mmObject= new MultimediaObject(source);
            MultimediaInfo infos= mmObject.getInfo();

            return infos.getVideo().getSize();

        } catch (EncoderException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static double getFileSize(File file) {
        double bytes = file.length();
        double kilobytes = (bytes / 1024);
        return (kilobytes / 1024);
    }


    public static String getFileSizeMo(File file) {
        double bytes = file.length();
        double kilobytes = (bytes / 1024);
        return Double.valueOf((kilobytes / 1024)).intValue() +"";
    }

    public static List<String> getFileLines(String filePath) throws IOException {
        File f = new File(filePath);
        if(!f.exists()){
            f.createNewFile();
        }else{
            System.out.println("File already exists");
        }
        BufferedReader bufReader;
        ArrayList<String> listOfLines = new ArrayList<>();
        try {
            bufReader = new BufferedReader(new FileReader(filePath));
            String line = bufReader.readLine();
            while (line != null) {
                listOfLines.add(line);
                line = bufReader.readLine();
            }
            bufReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return listOfLines;
    }

    public static void addLineToFile(File file, String line) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            bw.append(line).append("\n");
            bw.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
